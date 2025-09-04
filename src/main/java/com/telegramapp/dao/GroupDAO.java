package com.telegramapp.dao;

import com.telegramapp.model.Group;

import java.sql.SQLException;
import java.util.List;

public interface GroupDAO {
    void save(Group group) throws SQLException;
    Group findById(String id) throws SQLException;
    List<Group> findAll() throws SQLException;
    List<Group> findByUser(String userId) throws SQLException;
    void addMember(String groupId, String userId) throws SQLException;
    void removeMember(String groupId, String userId) throws SQLException;
    List<String> findMembers(String groupId) throws SQLException;
}
