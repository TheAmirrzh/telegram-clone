package com.telegramapp.dao;

import com.telegramapp.model.User;
import com.telegramapp.util.DB;

import java.sql.*;
import java.util.*;

public class UserDAO {
    public Optional<User> findByUsername(String username){
        String sql = "SELECT id, username, password_hash, profile_name, profile_pic, bio, status FROM users WHERE username = ?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e){ e.printStackTrace(); return Optional.empty(); }
    }

    public Optional<User> findById(UUID id){
        String sql = "SELECT id, username, password_hash, profile_name, profile_pic, bio, status FROM users WHERE id = ?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e){ e.printStackTrace(); return Optional.empty(); }
    }

    public boolean create(User u){
        String sql = "INSERT INTO users (id, username, password_hash, profile_name, profile_pic, bio, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, u.getId());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getProfileName());
            ps.setString(5, u.getProfilePic());
            ps.setString(6, u.getBio());
            ps.setString(7, u.getStatus());
            return ps.executeUpdate()==1;
        } catch (SQLException ex){
            if (ex.getSQLState()!=null && ex.getSQLState().startsWith("23")) {
                System.err.println("Duplicate user: "+u.getUsername());
            } else ex.printStackTrace();
            return false;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getObject("id", UUID.class));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setProfileName(rs.getString("profile_name"));
        u.setProfilePic(rs.getString("profile_pic"));
        u.setBio(rs.getString("bio"));
        u.setStatus(rs.getString("status"));
        return u;
    }
}
