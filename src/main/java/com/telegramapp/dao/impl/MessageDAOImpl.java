package com.telegramapp.dao.impl;

import com.telegramapp.dao.MessageDAO;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.Message;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageDAOImpl implements MessageDAO {
    private final DataSource ds;

    public MessageDAOImpl() {
        this.ds = DBConnection.getInstance().getDataSource();
    }

    // --- New Method Implementations ---

    @Override
    public Optional<Message> findLastMessageForChat(String receiverType, String receiverId, String currentUserId) throws SQLException {
        String sql;
        if ("USER".equalsIgnoreCase(receiverType)) {
            sql = "SELECT * FROM messages WHERE receiver_type = 'USER' AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) ORDER BY timestamp DESC LIMIT 1";
        } else {
            sql = "SELECT * FROM messages WHERE receiver_type = ? AND receiver_id = ? ORDER BY timestamp DESC LIMIT 1";
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("USER".equalsIgnoreCase(receiverType)) {
                ps.setString(1, currentUserId);
                ps.setString(2, receiverId);
                ps.setString(3, receiverId);
                ps.setString(4, currentUserId);
            } else {
                ps.setString(1, receiverType);
                ps.setString(2, receiverId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readMessageFromResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public int getUnreadMessageCount(String receiverType, String receiverId, String currentUserId) throws SQLException {
        String sql;
        if ("USER".equalsIgnoreCase(receiverType)) {
            // For a user, count messages sent by THEM to ME that are UNREAD
            sql = "SELECT COUNT(*) FROM messages WHERE receiver_type = 'USER' AND sender_id = ? AND receiver_id = ? AND read_status = 'UNREAD'";
        } else {
            // For groups/channels, count all UNREAD messages not sent by ME
            sql = "SELECT COUNT(*) FROM messages WHERE receiver_type = ? AND receiver_id = ? AND sender_id <> ? AND read_status = 'UNREAD'";
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("USER".equalsIgnoreCase(receiverType)) {
                ps.setString(1, receiverId); // from the other user
                ps.setString(2, currentUserId); // to me
            } else {
                ps.setString(1, receiverType);
                ps.setString(2, receiverId);
                ps.setString(3, currentUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public void markMessagesAsRead(String receiverType, String receiverId, String currentUserId) throws SQLException {
        String sql;
        if ("USER".equalsIgnoreCase(receiverType)) {
            // Mark messages sent by THEM to ME as READ
            sql = "UPDATE messages SET read_status = 'READ' WHERE receiver_type = 'USER' AND sender_id = ? AND receiver_id = ? AND read_status = 'UNREAD'";
        } else {
            // For groups/channels, mark all messages not sent by ME as READ
            sql = "UPDATE messages SET read_status = 'READ' WHERE receiver_type = ? AND receiver_id = ? AND sender_id <> ? AND read_status = 'UNREAD'";
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("USER".equalsIgnoreCase(receiverType)) {
                ps.setString(1, receiverId);
                ps.setString(2, currentUserId);
            } else {
                ps.setString(1, receiverType);
                ps.setString(2, receiverId);
                ps.setString(3, currentUserId);
            }
            ps.executeUpdate();
        }
    }


    // --- Existing Methods ---

    @Override
    public void save(Message m) throws SQLException {
        String sql = "INSERT INTO messages (id, sender_id, receiver_id, receiver_type, content, media_type, media_path, timestamp, read_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getSenderId());
            ps.setString(3, m.getReceiverId());
            ps.setString(4, m.getReceiverType());
            ps.setString(5, m.getContent());
            ps.setString(6, m.getMediaType());
            ps.setString(7, m.getMediaPath());
            LocalDateTime ts = m.getTimestamp() == null ? LocalDateTime.now() : m.getTimestamp();
            ps.setTimestamp(8, Timestamp.valueOf(ts));
            ps.setString(9, m.getReadStatus());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Message> findConversation(String receiverType, String receiverId, String currentUserId) throws SQLException {
        String sql;
        if ("USER".equalsIgnoreCase(receiverType)) {
            sql = "SELECT * FROM messages WHERE receiver_type = 'USER' AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) ORDER BY timestamp ASC, id ASC";
        } else {
            sql = "SELECT * FROM messages WHERE receiver_type = ? AND receiver_id = ? ORDER BY timestamp ASC, id ASC";
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("USER".equalsIgnoreCase(receiverType)) {
                ps.setString(1, currentUserId);
                ps.setString(2, receiverId);
                ps.setString(3, receiverId);
                ps.setString(4, currentUserId);
            } else {
                ps.setString(1, receiverType);
                ps.setString(2, receiverId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return readMessagesFromResultSet(rs);
            }
        }
    }

    @Override
    public List<Message> findNewMessagesAfter(String receiverType, String receiverId, String currentUserId, LocalDateTime after) throws SQLException {
        String sql;
        if ("USER".equalsIgnoreCase(receiverType)) {
            sql = "SELECT * FROM messages WHERE receiver_type = 'USER' AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) AND timestamp > ? ORDER BY timestamp ASC, id ASC";
        } else {
            sql = "SELECT * FROM messages WHERE receiver_type = ? AND receiver_id = ? AND timestamp > ? ORDER BY timestamp ASC, id ASC";
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if ("USER".equalsIgnoreCase(receiverType)) {
                ps.setString(paramIndex++, currentUserId);
                ps.setString(paramIndex++, receiverId);
                ps.setString(paramIndex++, receiverId);
                ps.setString(paramIndex++, currentUserId);
            } else {
                ps.setString(paramIndex++, receiverType);
                ps.setString(paramIndex++, receiverId);
            }
            ps.setTimestamp(paramIndex, Timestamp.valueOf(after));
            try (ResultSet rs = ps.executeQuery()) {
                return readMessagesFromResultSet(rs);
            }
        }
    }

    private List<Message> readMessagesFromResultSet(ResultSet rs) throws SQLException {
        List<Message> list = new ArrayList<>();
        while (rs.next()) {
            list.add(readMessageFromResultSet(rs));
        }
        return list;
    }

    private Message readMessageFromResultSet(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("timestamp");
        LocalDateTime dt = ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
        return new Message(
                rs.getString("id"),
                rs.getString("sender_id"),
                rs.getString("receiver_id"),
                rs.getString("receiver_type"),
                rs.getString("content"),
                rs.getString("media_type"),
                rs.getString("media_path"),
                dt,
                rs.getString("read_status")
        );
    }

    @Override
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}

