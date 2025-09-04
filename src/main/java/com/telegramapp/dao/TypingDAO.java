package com.telegramapp.dao;

import com.telegramapp.db.DBConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TypingDAO {

    public boolean setTyping(UUID chatId, UUID userId){
        String sql = "INSERT INTO typing_status (chat_id, user_id, last_ts) VALUES (?, ?, now()) ON CONFLICT (chat_id, user_id) DO UPDATE SET last_ts = now()";
        try (Connection c = DBConnection.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, chatId); ps.setObject(2, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public List<UUID> getTypingUsers(UUID chatId, int secondsWindow){
        String sql = "SELECT user_id FROM typing_status WHERE chat_id = ? AND last_ts >= now() - (? || ' seconds')::interval";
        List<UUID> out = new ArrayList<>();
        try (Connection c = DBConnection.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, chatId); ps.setInt(2, secondsWindow);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()) out.add(rs.getObject(1, UUID.class));
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return out;
    }
}
