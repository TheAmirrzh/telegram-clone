package com.telegramapp.dao.impl;

import com.telegramapp.dao.ChannelDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.Channel;
import com.telegramapp.model.ChannelSubscriberInfo;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChannelDAOImpl implements ChannelDAO {
    private final DataSource ds;

    public ChannelDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    @Override
    public List<ChannelSubscriberInfo> findSubscribersWithInfo(String channelId) throws SQLException {
        List<ChannelSubscriberInfo> subscribers = new ArrayList<>();
        String sql = "SELECT user_id, role FROM channel_subscribers WHERE channel_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subscribers.add(new ChannelSubscriberInfo(rs.getString("user_id"), rs.getString("role")));
                }
            }
        }
        return subscribers;
    }

    @Override
    public void removeSubscriber(String channelId, String userId) throws SQLException {
        String sql = "DELETE FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channelId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateSubscriberRole(String channelId, String userId, String newRole) throws SQLException {
        String sql = "UPDATE channel_subscribers SET role = ? WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setString(2, channelId);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }


    // --- New Method Implementation ---
    @Override
    public List<String> findSubscribers(String channelId) throws SQLException {
        List<String> subscriberIds = new ArrayList<>();
        String sql = "SELECT user_id FROM channel_subscribers WHERE channel_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subscriberIds.add(rs.getString("user_id"));
                }
            }
        }
        return subscriberIds;
    }

    @Override
    public void addSubscriber(String channelId, String userId, String role) throws SQLException {
        String sql = "INSERT INTO channel_subscribers (channel_id, user_id, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channelId);
            ps.setString(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean isSubscriber(String channelId, String userId) throws SQLException {
        String sql = "SELECT 1 FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channelId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // --- Paste existing methods from your file below this line ---
    @Override
    public void save(Channel channel) throws SQLException {
        String sql = "INSERT INTO channels (id, name, owner_id) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel.getId());
            ps.setString(2, channel.getName());
            ps.setString(3, channel.getOwnerId());
            ps.executeUpdate();
        }
    }

    @Override
    public Channel findById(String id) throws SQLException {
        String sql = "SELECT id, name, owner_id FROM channels WHERE id = ?";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Channel(rs.getString("id"), rs.getString("name"), rs.getString("owner_id"));
                return null;
            }
        }
    }

    @Override
    public List<Channel> findAll() throws SQLException {
        String sql = "SELECT id, name, owner_id FROM channels ORDER BY name";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Channel> out = new ArrayList<>();
            while (rs.next()) out.add(new Channel(rs.getString("id"), rs.getString("name"), rs.getString("owner_id")));
            return out;
        }
    }

    @Override
    public List<Channel> findByOwner(String ownerId) throws SQLException {
        String sql = "SELECT id, name, owner_id FROM channels WHERE owner_id = ? ORDER BY name";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Channel> out = new ArrayList<>();
                while (rs.next()) out.add(new Channel(rs.getString("id"), rs.getString("name"), rs.getString("owner_id")));
                return out;
            }
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM channels WHERE id = ?";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}
