package com.telegramapp.service;

import com.telegramapp.dao.TypingDAO;
import com.telegramapp.dao.impl.TypingDAOImpl;

import java.sql.SQLException;
import java.util.List;

public class TypingService {
    private final TypingDAO typingDAO;

    public TypingService() throws SQLException {
        this.typingDAO = new TypingDAOImpl();
    }

    public void updateTypingStatus(String chatId, String userId) {
        try {
            typingDAO.setTyping(chatId, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTypingUsers(String chatId, String currentUserId, int secondsWindow) throws SQLException {
        return typingDAO.getTypingUsers(chatId, currentUserId, secondsWindow);
    }
}

