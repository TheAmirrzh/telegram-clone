package com.telegramapp.dao.impl;

import com.telegramapp.dao.GroupDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.Group;
import com.telegramapp.model.GroupMemberInfo;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAOImpl implements GroupDAO {
    private final DataSource ds;

    public GroupDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    public List<Group> searchAllGroups(String query) throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT * FROM groups WHERE LOWER(name) LIKE LOWER(?) ORDER BY name LIMIT 50";
        String searchPattern = "%" + query + "%";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id")));
                }
            }
        }
        return groups;
    }
    @Override
    public List<Group> searchGroups(String query) throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT * FROM groups WHERE LOWER(name) LIKE LOWER(?) LIMIT 50";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id")));
                }
            }
        }
        return groups;
    }

    @Override
    public List<GroupMemberInfo> findMembersWithInfo(String groupId) throws SQLException {
        List<GroupMemberInfo> members = new ArrayList<>();
        String sql = "SELECT user_id, role FROM group_members WHERE group_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(new GroupMemberInfo(rs.getString("user_id"), rs.getString("role")));
                }
            }
        }
        return members;
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
        String sql = "SELECT * FROM groups WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id"));
                }
                return null;
            }
        }
    }

    @Override
    public List<Group> findByUser(String userId) throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.* FROM groups g JOIN group_members gm ON g.id = gm.group_id WHERE gm.user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(new Group(rs.getString("id"), rs.getString("name"), rs.getString("creator_id")));
                }
            }
        }
        return groups;
    }

    @Override
    public void addMember(String groupId, String userId, String role) throws SQLException {
        String sql = "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            ps.setString(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
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
    public void updateMemberRole(String groupId, String userId, String role) throws SQLException {
        String sql = "UPDATE group_members SET role = ? WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, groupId);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }
}
