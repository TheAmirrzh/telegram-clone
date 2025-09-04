package com.telegramapp.dao.impl;

import com.telegramapp.dao.ChannelDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.Channel;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChannelDAOImpl implements ChannelDAO {
    private final DataSource ds;

    public ChannelDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    // For tests
    public ChannelDAOImpl(DataSource dataSource) {
        this.ds = dataSource;
    }

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
