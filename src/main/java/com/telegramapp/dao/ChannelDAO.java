package com.telegramapp.dao;

import com.telegramapp.model.Channel;
import java.sql.SQLException;
import java.util.List;

public interface ChannelDAO {
    void save(Channel channel) throws SQLException;
    Channel findById(String id) throws SQLException;
    List<Channel> findAll() throws SQLException;
    List<Channel> findByOwner(String ownerId) throws SQLException;
    void delete(String id) throws SQLException;

    // --- New Methods for Advanced Features ---
    void addSubscriber(String channelId, String userId) throws SQLException;
    boolean isSubscriber(String channelId, String userId) throws SQLException;
    List<String> findSubscribers(String channelId) throws SQLException; // Added missing method
}
