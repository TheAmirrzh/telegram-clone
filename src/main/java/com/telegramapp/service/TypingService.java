package com.telegramapp.service;

import com.telegramapp.dao.TypingDAO;
import com.telegramapp.dao.impl.TypingDAOImpl;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<String> getTypingUsers(String chatId, String currentUserId) throws SQLException {
        // Exclude the current user from the list of people typing
        return typingDAO.getTypingUsers(chatId, 5).stream()
                .filter(userId -> !userId.equals(currentUserId))
                .collect(Collectors.toList());
    }
}

