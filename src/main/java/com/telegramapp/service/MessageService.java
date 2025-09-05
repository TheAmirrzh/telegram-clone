package com.telegramapp.service;

import com.telegramapp.dao.MessageDAO;
import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.model.Message;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MessageService {
    private final MessageDAO dao;

    public MessageService() throws SQLException {
        this.dao = new MessageDAOImpl();
    }

    public void sendMessage(Message m) throws SQLException {
        m.setReadStatus("UNREAD");
        dao.save(m);
    }

    public void editMessage(Message message) throws SQLException {
        dao.update(message);
    }

    public void deleteMessage(String messageId, String senderId) throws SQLException {
        dao.delete(messageId, senderId);
    }

    public List<Message> loadConversation(String receiverType, String receiverId, String currentUserId) throws SQLException {
        return dao.findConversation(receiverType, receiverId, currentUserId);
    }

    public List<Message> loadNewSince(String receiverType, String receiverId, String currentUserId, LocalDateTime since) throws SQLException {
        return dao.findNewMessagesAfter(receiverType, receiverId, currentUserId, since);
    }

    public void markMessagesAsRead(String receiverType, String receiverId, String userId) throws SQLException {
        dao.markMessagesAsRead(receiverType, receiverId, userId);
    }
}
