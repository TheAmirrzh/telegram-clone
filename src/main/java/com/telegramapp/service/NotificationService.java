package com.telegramapp.service;

import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.model.Message;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Notification service that can insert notification messages into the DB.
 */
public class NotificationService {
    private final MessageDAOImpl messageDAO;

    public NotificationService() {
        this.messageDAO = new MessageDAOImpl();
    }

    public void sendNotification(UUID recipient, String text) {
        try {
            String recipientStr = recipient == null ? null : recipient.toString();
            String systemSender = "system";
            Message m = new Message(systemSender, recipientStr, "USER", text);
            messageDAO.save(m);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
