package com.telegramapp.dao;

import com.telegramapp.model.Message;
import com.telegramapp.util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MessageDAO {
    public boolean insert(Message m){
        if (m.getTimestamp()==null) m.setTimestamp(LocalDateTime.now());
        String sql = "INSERT INTO messages (id, sender_id, receiver_private_chat, receiver_group, receiver_channel, content, image_path, timestamp, read_status) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, m.getId());
            ps.setObject(2, m.getSenderId());
            ps.setObject(3, m.getReceiverPrivateChat());
            ps.setObject(4, m.getReceiverGroup());
            ps.setObject(5, m.getReceiverChannel());
            ps.setString(6, m.getContent());
            ps.setString(7, m.getImagePath());
            ps.setTimestamp(8, Timestamp.valueOf(m.getTimestamp()));
            ps.setString(9, m.getReadStatus());
            return ps.executeUpdate()==1;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public boolean editMessage(UUID messageId, String newContent){
        String sql = "UPDATE messages SET content = ?, read_status = 'EDITED' WHERE id = ?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setString(1, newContent);
            ps.setObject(2, messageId);
            return ps.executeUpdate()==1;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public boolean deleteMessage(UUID messageId){
        String sql = "UPDATE messages SET content = '[deleted]', image_path = NULL, read_status = 'DELETED' WHERE id = ?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, messageId);
            return ps.executeUpdate()==1;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public List<Message> loadPrivateChatHistory(UUID privateChatId, int limit){
        String sql = "SELECT * FROM messages WHERE receiver_private_chat=? ORDER BY timestamp ASC LIMIT ?";
        return load(sql, ps -> { ps.setObject(1, privateChatId); ps.setInt(2, limit); });
    }

    public List<Message> loadGroupHistory(UUID groupId, int limit){
        String sql = "SELECT * FROM messages WHERE receiver_group=? ORDER BY timestamp ASC LIMIT ?";
        return load(sql, ps -> { ps.setObject(1, groupId); ps.setInt(2, limit); });
    }

    public List<Message> loadChannelHistory(UUID channelId, int limit){
        String sql = "SELECT * FROM messages WHERE receiver_channel=? ORDER BY timestamp ASC LIMIT ?";
        return load(sql, ps -> { ps.setObject(1, channelId); ps.setInt(2, limit); });
    }

    public List<Message> searchMessages(String query, UUID userId, int limit){
        String sql = "SELECT m.* FROM messages m WHERE m.content ILIKE ? AND (\n" +
                "  m.receiver_private_chat IN (SELECT id FROM private_chats WHERE user1=? OR user2=?)\n" +
                "  OR m.receiver_group IN (SELECT group_id FROM group_members WHERE user_id=?)\n" +
                "  OR m.receiver_channel IN (SELECT channel_id FROM channel_subscribers WHERE user_id=?)\n" +
                "  OR m.sender_id = ?\n" +
                ") ORDER BY m.timestamp DESC LIMIT ?";
        return load(sql, ps -> {
            ps.setString(1, "%" + query + "%");
            ps.setObject(2, userId);
            ps.setObject(3, userId);
            ps.setObject(4, userId);
            ps.setObject(5, userId);
            ps.setObject(6, userId);
            ps.setInt(7, limit);
        });
    }

    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Message> load(String sql, Binder binder){
        List<Message> list = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()) list.add(map(rs));
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return list;
    }

    private Message map(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getObject("id", UUID.class));
        m.setSenderId(rs.getObject("sender_id", UUID.class));
        m.setReceiverPrivateChat(rs.getObject("receiver_private_chat", UUID.class));
        m.setReceiverGroup(rs.getObject("receiver_group", UUID.class));
        m.setReceiverChannel(rs.getObject("receiver_channel", UUID.class));
        m.setContent(rs.getString("content"));
        m.setImagePath(rs.getString("image_path"));
        Timestamp ts = rs.getTimestamp("timestamp");
        m.setTimestamp(ts!=null? ts.toLocalDateTime() : null);
        m.setReadStatus(rs.getString("read_status"));
        return m;
    }
}
