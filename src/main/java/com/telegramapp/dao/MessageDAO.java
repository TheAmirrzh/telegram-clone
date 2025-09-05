package com.telegramapp.dao;

import com.telegramapp.model.Message;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageDAO {
    void save(Message message) throws SQLException;
    void update(Message message) throws SQLException;
    void delete(String messageId, String senderId) throws SQLException;
    Optional<Message> findById(String messageId) throws SQLException; // For replies
    List<Message> findConversation(String receiverType, String receiverId, String currentUserId) throws SQLException;
    List<Message> findNewMessagesAfter(String receiverType, String receiverId, String currentUserId, LocalDateTime after) throws SQLException;
    Optional<Message> findLastMessageForChat(String receiverType, String receiverId, String currentUserId) throws SQLException;
    int getUnreadMessageCount(String receiverType, String receiverId, String currentUserId) throws SQLException;
    void markMessagesAsRead(String receiverType, String receiverId, String currentUserId) throws SQLException;
}

