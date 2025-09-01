package com.telegramapp.dao;

import com.telegramapp.util.DB;

import java.sql.*;
import java.util.UUID;

public class ChatDAO {
    public UUID findOrCreatePrivateChat(UUID a, UUID b){
        UUID u1 = a.compareTo(b) < 0 ? a : b;
        UUID u2 = a.compareTo(b) < 0 ? b : a;
        String select = "SELECT id FROM private_chats WHERE user1=? AND user2=?";
        String insert = "INSERT INTO private_chats (user1, user2) VALUES (?, ?) ON CONFLICT (user1, user2) DO NOTHING RETURNING id";
        try (Connection c = DB.getConnection()){
            try (PreparedStatement ps = c.prepareStatement(select)){
                ps.setObject(1, u1); ps.setObject(2, u2);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) return rs.getObject(1, UUID.class);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(insert)){
                ps.setObject(1, u1); ps.setObject(2, u2);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) return rs.getObject(1, UUID.class);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(select)){
                ps.setObject(1, u1); ps.setObject(2, u2);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) return rs.getObject(1, UUID.class);
                }
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return null;
    }
}
