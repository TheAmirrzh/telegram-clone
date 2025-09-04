package com.telegramapp.dao.impl;

import com.telegramapp.dao.GroupDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.Group;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAOImpl implements GroupDAO {
    private final DataSource ds;

    public GroupDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    // for tests
    public GroupDAOImpl(DataSource dataSource) {
        this.ds = dataSource;
    }

    @Override
    public void save(Group group) throws SQLException {
        String sql = "INSERT INTO groups (id, name, creator_id) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, group.getId());
            ps.setString(2, group.getName());
            ps.setString(3, group.getCreatorId());
            ps.executeUpdate();
        }
    }

    @Override
    public Group findById(String id) throws SQLException {
        String sql = "SELECT id, name, creator_id FROM groups WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id"));
                return null;
            }
        }
    }

    @Override
    public List<Group> findAll() throws SQLException {
        String sql = "SELECT id, name, creator_id FROM groups ORDER BY name";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Group> out = new ArrayList<>();
            while (rs.next()) out.add(new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id")));
            return out;
        }
    }

    @Override
    public List<Group> findByUser(String userId) throws SQLException {
        String sql = "SELECT g.id, g.name, g.creator_id FROM groups g JOIN group_members gm ON g.id = gm.group_id WHERE gm.user_id = ? ORDER BY g.name";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Group> out = new ArrayList<>();
                while (rs.next()) out.add(new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id")));
                return out;
            }
        }
    }

    @Override
    public void addMember(String groupId, String userId) throws SQLException {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @Override
    public void removeMember(String groupId, String userId) throws SQLException {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<String> findMembers(String groupId) throws SQLException {
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString("user_id"));
                return out;
            }
        }
    }
}
