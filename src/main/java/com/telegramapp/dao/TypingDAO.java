package com.telegramapp.dao;

import java.sql.SQLException;
import java.util.List;

public interface TypingDAO {
    void setTyping(String chatId, String userId) throws SQLException;
    List<String> getTypingUsers(String chatId, String currentUserId, int secondsWindow) throws SQLException;
}

