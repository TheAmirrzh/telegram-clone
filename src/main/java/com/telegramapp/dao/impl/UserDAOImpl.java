package com.telegramapp.dao.impl;

import com.telegramapp.dao.UserDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.User;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {
    private final DataSource ds;

    public UserDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    // For tests: allow injecting a DataSource (H2)
    public UserDAOImpl(DataSource dataSource) {
        this.ds = dataSource;
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, display_name, bio, profile_pic_path, status FROM users WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User(rs.getString("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("display_name"));
                    u.setBio(rs.getString("bio"));
                    u.setProfilePicPath(rs.getString("profile_pic_path"));
                    u.setStatus(rs.getString("status"));
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT id, username, password_hash, display_name, bio, profile_pic_path, status FROM users WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User(rs.getString("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("display_name"));
                    u.setBio(rs.getString("bio"));
                    u.setProfilePicPath(rs.getString("profile_pic_path"));
                    u.setStatus(rs.getString("status"));
                    return Optional.of(u);
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
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, display_name, bio, profile_pic_path, status FROM users ORDER BY username";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> out = new ArrayList<>();
            while (rs.next()) {
                User u = new User(rs.getString("id"), rs.getString("username"), "", rs.getString("display_name"));
                u.setBio(rs.getString("bio"));
                u.setProfilePicPath(rs.getString("profile_pic_path"));
                u.setStatus(rs.getString("status"));
                out.add(u);
            }
            return out;
        }
    }

    @Override
    public List<User> findAllExcept(String id) throws SQLException {
        String sql = "SELECT id, username, display_name, bio, profile_pic_path, status FROM users WHERE id <> ? ORDER BY username";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> out = new ArrayList<>();
                while (rs.next()) {
                    User u = new User(rs.getString("id"), rs.getString("username"), "", rs.getString("display_name"));
                    u.setBio(rs.getString("bio"));
                    u.setProfilePicPath(rs.getString("profile_pic_path"));
                    u.setStatus(rs.getString("status"));
                    out.add(u);
                }
                return out;
            }
        }
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
}
