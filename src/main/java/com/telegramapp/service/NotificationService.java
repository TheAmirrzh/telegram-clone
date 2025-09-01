package com.telegramapp.service;

import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.dao.MessageDAO;
import com.telegramapp.util.DB;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class NotificationService {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "notif"); t.setDaemon(true); return t; });
    private volatile LocalDateTime lastChecked = LocalDateTime.now().minusSeconds(5);
    private final User user;
    private final MessageDAO messageDAO = new MessageDAO();
    private ScheduledFuture<?> task;

    public NotificationService(User user){ this.user = user; }

    public void start(){
        task = exec.scheduleWithFixedDelay(this::poll, 5, 5, TimeUnit.SECONDS);
    }

    public void stop(){
        if (task!=null) task.cancel(true);
        exec.shutdown();
    }

    private void poll(){
        try {
            // Simple approach: find messages where user is recipient (private chats, groups member, channel subscriber) and timestamp > lastChecked
            String sql = "SELECT m.id, m.sender_id, m.content, m.timestamp, m.receiver_private_chat, m.receiver_group, m.receiver_channel FROM messages m WHERE m.timestamp > ? AND m.sender_id <> ? AND (\n" +
                    "  m.receiver_private_chat IN (SELECT id FROM private_chats WHERE user1=? OR user2=?)\n" +
                    "  OR m.receiver_group IN (SELECT group_id FROM group_members WHERE user_id=?)\n" +
                    "  OR m.receiver_channel IN (SELECT channel_id FROM channel_subscribers WHERE user_id=?)\n" +
                    ") ORDER BY m.timestamp ASC";
            try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
                ps.setTimestamp(1, Timestamp.valueOf(lastChecked));
                ps.setObject(2, user.getId());
                ps.setObject(3, user.getId()); ps.setObject(4, user.getId());
                ps.setObject(5, user.getId()); ps.setObject(6, user.getId());
                try (ResultSet rs = ps.executeQuery()){
                    List<String> messages = new ArrayList<>();
                    LocalDateTime newest = lastChecked;
                    while (rs.next()){
                        Timestamp ts = rs.getTimestamp("timestamp");
                        if (ts!=null && ts.toLocalDateTime().isAfter(newest)) newest = ts.toLocalDateTime();
                        String content = rs.getString("content");
                        messages.add(content == null ? "[image]" : content);
                    }
                    if (!messages.isEmpty()) showPopup(messages.size(), messages.get(messages.size()-1));
                    lastChecked = newest.plusNanos(1);
                }
            }
        } catch (Throwable t){ t.printStackTrace(); }
    }

    private void showPopup(int count, String lastMsg){
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("New messages");
            a.setHeaderText(count + " new message(s)");
            a.setContentText(lastMsg);
            a.show();
        });
    }
}
