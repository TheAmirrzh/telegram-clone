package com.telegramapp.dao;

import com.telegramapp.model.Message;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageDAO {
    void save(Message message) throws SQLException;
    List<Message> findConversation(String receiverType, String receiverId, String currentUserId) throws SQLException;
    List<Message> findNewMessagesAfter(String receiverType, String receiverId, String currentUserId, LocalDateTime after) throws SQLException;
}
