package com.telegramapp.service;

import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.model.Message;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Small helper used to create notification messages for the realtime system.
 * Adjusted to use existing Message constructors.
 */
public class RealtimeService {
    private final MessageDAOImpl messageDAO;

    public RealtimeService() {
        this.messageDAO = new MessageDAOImpl();
    }

    public void publishSystemMessage(UUID receiverId, String receiverType, String text) {
        try {
            String receiverIdStr = receiverId == null ? null : receiverId.toString();
            // use a special sender id like "system" or null depending on your model
            String systemSender = "system";
            Message m = new Message(systemSender, receiverIdStr, receiverType, text);
            messageDAO.save(m);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
