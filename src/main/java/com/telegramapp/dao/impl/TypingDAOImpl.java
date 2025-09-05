package com.telegramapp.dao.impl;

import com.telegramapp.dao.TypingDAO;
import com.telegramapp.db.DBConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TypingDAOImpl implements TypingDAO {
    private final DataSource ds;

    public TypingDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    @Override
    public void setTyping(String chatId, String userId) throws SQLException {
        String sql = "INSERT INTO typing_status (chat_id, user_id, last_ts) VALUES (?, ?, NOW()) " +
                "ON CONFLICT (chat_id, user_id) DO UPDATE SET last_ts = NOW()";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chatId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<String> getTypingUsers(String chatId, String currentUserId, int secondsWindow) throws SQLException {
        List<String> typingUsers = new ArrayList<>();
        // Exclude the current user from the list of typing users
        String sql = "SELECT user_id FROM typing_status WHERE chat_id = ? AND user_id <> ? AND last_ts >= NOW() - INTERVAL '? seconds'";

        // A simple way to inject the interval value safely. Prepared statements don't work well with INTERVAL.
        sql = sql.replace("?", String.valueOf(secondsWindow));

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chatId);
            ps.setString(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    typingUsers.add(rs.getString("user_id"));
                }
            }
        }
        return typingUsers;
    }
}

