package com.telegramapp.dao;

import com.telegramapp.model.Message;
import com.telegramapp.util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MessageDAO - basic CRUD for messages.
 * Note: this implementation attempts to emit a NOTIFY payload after insert as a fallback
 * if DB triggers are not installed.
 */
public class MessageDAO {

    public boolean insert(Message m){
        if (m.getTimestamp()==null) m.setTimestamp(LocalDateTime.now());
        String sql = "INSERT INTO messages (id, sender_id, receiver_private_chat, receiver_group, receiver_channel, content, image_path, timestamp, read_status) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, m.getId());
            ps.setObject(2, m.getSenderId());
            ps.setObject(3, m.getReceiverPrivateChat());
            ps.setObject(4, m.getReceiverGroup());
            ps.setObject(5, m.getReceiverChannel());
            ps.setString(6, m.getContent());
            ps.setString(7, m.getImagePath());
            ps.setTimestamp(8, Timestamp.valueOf(m.getTimestamp()));
            ps.setString(9, m.getReadStatus());
            boolean ok = ps.executeUpdate()==1;

            // Fallback realtime notify if DB trigger is not installed
            if (ok) {
                String channel = null;
                String chatType = null;
                String rid = null;
                if (m.getReceiverPrivateChat()!=null) {
                    channel = "private_" + m.getReceiverPrivateChat().toString().replace("-", "");
                    chatType = "private";
                    rid = m.getReceiverPrivateChat().toString();
                } else if (m.getReceiverGroup()!=null) {
                    channel = "group_" + m.getReceiverGroup().toString().replace("-", "");
                    chatType = "group";
                    rid = m.getReceiverGroup().toString();
                } else if (m.getReceiverChannel()!=null) {
                    channel = "channel_" + m.getReceiverChannel().toString().replace("-", "");
                    chatType = "channel";
                    rid = m.getReceiverChannel().toString();
                }

                if (channel != null) {
                    // Build a JSON-ish payload and call pg_notify
                    String payload = String.format("{\"chatType\":\"%s\",\"id\":\"%s\",\"messageId\":\"%s\"}",
                            chatType, rid, m.getId().toString());
                    try (Statement st = c.createStatement()) {
                        // Use single-quoted string; escape single quotes in payload
                        String safe = payload.replace("'", "''");
                        st.execute("SELECT pg_notify('" + channel + "', '" + safe + "')");
                    } catch (SQLException ignore) {
                        // best effort: ignore if notify fails
                    }
                }
            }

            return ok;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public boolean deleteMessage(UUID messageId){
        String sql = "UPDATE messages SET content = '[deleted]', image_path = NULL, read_status = 'DELETED' WHERE id = ?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, messageId);
            return ps.executeUpdate()==1;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    private List<Message> load(String sql, SqlSetter setter){
        List<Message> out = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            if (setter != null) setter.set(ps);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    Message m = new Message();
                    m.setId(rs.getObject("id", UUID.class));
                    m.setSenderId(rs.getObject("sender_id", UUID.class));
                    m.setReceiverPrivateChat(rs.getObject("receiver_private_chat", UUID.class));
                    m.setReceiverGroup(rs.getObject("receiver_group", UUID.class));
                    m.setReceiverChannel(rs.getObject("receiver_channel", UUID.class));
                    m.setContent(rs.getString("content"));
                    m.setImagePath(rs.getString("image_path"));
                    m.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                    m.setReadStatus(rs.getString("read_status"));
                    out.add(m);
                }
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return out;
    }

    public List<Message> loadPrivateChatHistory(UUID privateChatId, int limit){
        String sql = "SELECT * FROM messages WHERE receiver_private_chat=? ORDER BY timestamp ASC LIMIT ?";
        return load(sql, ps -> { ps.setObject(1, privateChatId); ps.setInt(2, limit); });
    }

    public List<Message> loadGroupHistory(UUID groupId, int limit){
        String sql = "SELECT * FROM messages WHERE receiver_group=? ORDER BY timestamp ASC LIMIT ?";
        return load(sql, ps -> { ps.setObject(1, groupId); ps.setInt(2, limit); });
    }

    public List<Message> loadChannelHistory(UUID channelId, int limit){
        String sql = "SELECT * FROM messages WHERE receiver_channel=? ORDER BY timestamp ASC LIMIT ?";
        return load(sql, ps -> { ps.setObject(1, channelId); ps.setInt(2, limit); });
    }

    // Functional interface for parameter setting
    private interface SqlSetter {
        void set(PreparedStatement ps) throws SQLException;
    }
}
