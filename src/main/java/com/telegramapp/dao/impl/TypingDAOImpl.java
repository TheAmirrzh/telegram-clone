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
        String sql = "INSERT INTO typing_status (chat_id, user_id, last_typed) VALUES (?, ?, NOW()) " +
                "ON CONFLICT (chat_id, user_id) DO UPDATE SET last_typed = NOW()";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chatId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<String> getTypingUsers(String chatId, int secondsWindow) throws SQLException {
        List<String> typingUserIds = new ArrayList<>();
        // Correct, safe SQL query using two proper placeholders.
        String sql = "SELECT user_id FROM typing_status WHERE chat_id = ? AND last_typed > NOW() - CAST(? || ' seconds' AS INTERVAL)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Set the parameters for the placeholders correctly.
            ps.setString(1, chatId);
            ps.setInt(2, secondsWindow);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    typingUserIds.add(rs.getString("user_id"));
                }
            }
        }
        return typingUserIds;
    }
}

