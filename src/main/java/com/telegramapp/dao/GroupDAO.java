package com.telegramapp.dao;

import com.telegramapp.model.Group;
import com.telegramapp.model.GroupMemberInfo;
import java.sql.SQLException;
import java.util.List;

public interface GroupDAO {
    void save(Group group) throws SQLException;
    Group findById(String id) throws SQLException;
    List<Group> findByUser(String userId) throws SQLException;
    void addMember(String groupId, String userId, String role) throws SQLException;
    void removeMember(String groupId, String userId) throws SQLException;
    void updateMemberRole(String groupId, String userId, String role) throws SQLException;
    List<GroupMemberInfo> findMembersWithInfo(String groupId) throws SQLException;
    List<Group> searchGroups(String query) throws SQLException;

}

