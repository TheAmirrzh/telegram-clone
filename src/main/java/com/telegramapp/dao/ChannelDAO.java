package com.telegramapp.dao;

import com.telegramapp.model.Channel;
import com.telegramapp.model.ChannelSubscriberInfo;

import java.sql.SQLException;
import java.util.List;

public interface ChannelDAO {
    void save(Channel channel) throws SQLException;
    Channel findById(String id) throws SQLException;
    List<Channel> findAll() throws SQLException;
    List<Channel> findByOwner(String ownerId) throws SQLException;
    void delete(String id) throws SQLException;
    List<Channel> findByUserId(String userId) throws SQLException;

    void addSubscriber(String channelId, String userId, String role) throws SQLException;
    void removeSubscriber(String channelId, String userId) throws SQLException;
    void updateSubscriberRole(String channelId, String userId, String newRole) throws SQLException;
    boolean isSubscriber(String channelId, String userId) throws SQLException;
    List<String> findSubscribers(String channelId) throws SQLException;
    List<ChannelSubscriberInfo> findSubscribersWithInfo(String channelId) throws SQLException;
    List<Channel> searchChannels(String query) throws SQLException;
    List<Channel> findSubscribedChannels(String userId) throws SQLException;
}
