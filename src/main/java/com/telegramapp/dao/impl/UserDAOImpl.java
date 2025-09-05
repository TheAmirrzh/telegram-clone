package com.telegramapp.dao.impl;

import com.telegramapp.dao.UserDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.User;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDAOImpl implements UserDAO {
    private final DataSource ds;

    public UserDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    // New Method for fetching multiple users by their IDs
    @Override
    public List<User> findByIds(List<String> userIds) throws SQLException {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE id IN (" +
                userIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < userIds.size(); i++) {
                ps.setString(i + 1, userIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }


    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (id, username, password_hash, display_name, bio, profile_pic_path, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getDisplayName());
            ps.setString(5, user.getBio());
            ps.setString(6, user.getProfilePicPath());
            ps.setString(7, user.getStatus());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET display_name = ?, bio = ?, profile_pic_path = ? WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getDisplayName());
            ps.setString(2, user.getBio());
            ps.setString(3, user.getProfilePicPath());
            ps.setString(4, user.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    @Override
    public List<User> findAllExcept(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id <> ? ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    @Override
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User(rs.getString("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("display_name"));
        u.setBio(rs.getString("bio"));
        u.setProfilePicPath(rs.getString("profile_pic_path"));
        u.setStatus(rs.getString("status"));
        return u;
    }
}

