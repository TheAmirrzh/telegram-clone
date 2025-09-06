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
                rs.getString("read_status"),
                rs.getString("reply_to_message_id")
        );
    }

    private List<Message> readMessagesFromResultSet(ResultSet rs) throws SQLException {
        List<Message> list = new ArrayList<>();
        while (rs.next()) {
            list.add(readMessageFromResultSet(rs));
        }
        return list;
    }

    @Override
    public Optional<Message> findById(String messageId) throws SQLException {
        String sql = "SELECT * FROM messages WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readMessageFromResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

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
            sql = "SELECT COUNT(*) FROM messages WHERE receiver_type = 'USER' AND sender_id = ? AND receiver_id = ? AND read_status = 'UNREAD'";
        } else {
            sql = "SELECT COUNT(*) FROM messages WHERE receiver_type = ? AND receiver_id = ? AND sender_id <> ? AND read_status = 'UNREAD'";
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
            sql = "UPDATE messages SET read_status = 'READ' WHERE receiver_type = 'USER' AND sender_id = ? AND receiver_id = ? AND read_status = 'UNREAD'";
        } else {
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

    @Override
    public void save(Message m) throws SQLException {
        String sql = "INSERT INTO messages (id, sender_id, receiver_id, receiver_type, content, media_type, media_path, timestamp, read_status, reply_to_message_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getSenderId());
            ps.setString(3, m.getReceiverId());
            ps.setObject(4, m.getReceiverType(), java.sql.Types.OTHER);
            ps.setString(5, m.getContent());
            ps.setString(6, m.getMediaType());
            ps.setString(7, m.getMediaPath());
            ps.setTimestamp(8, Timestamp.valueOf(m.getTimestamp() == null ? LocalDateTime.now() : m.getTimestamp()));
            ps.setObject(9, m.getReadStatus(), java.sql.Types.OTHER);
            ps.setString(10, m.getReplyToMessageId());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Message message) throws SQLException {
        String sql = "UPDATE messages SET content = ?, read_status = 'EDITED', timestamp = ? WHERE id = ? AND sender_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message.getContent());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, message.getId());
            ps.setString(4, message.getSenderId()); // Ensure users can only edit their own messages
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

    @Override
    public void delete(String messageId, String senderId) throws SQLException {
        String sql = "UPDATE messages SET content = '[This message was deleted]', media_path = NULL, media_type = NULL, read_status = 'DELETED' WHERE id = ? AND sender_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, messageId);
            ps.setString(2, senderId);
            ps.executeUpdate();
        }
    }
}

