package com.telegramapp.dao;

import com.telegramapp.model.GroupChat;
import com.telegramapp.util.DB;

import java.sql.*;
import java.util.*;

public class GroupDAO {
    public List<GroupChat> listGroupsForUser(UUID userId){
        String sql = "SELECT g.id, g.name, g.creator, g.profile_pic FROM groups g JOIN group_members m ON g.id=m.group_id WHERE m.user_id=? ORDER BY g.name";
        List<GroupChat> list = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()) list.add(map(rs));
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return list;
    }

    public boolean create(GroupChat g){
        String sql = "INSERT INTO groups (id, name, creator, profile_pic) VALUES (?, ?, ?, ?)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, g.getId());
            ps.setString(2, g.getName());
            ps.setObject(3, g.getCreator());
            ps.setString(4, g.getProfilePic());
            return ps.executeUpdate()==1;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public boolean addMember(UUID groupId, UUID userId){
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, groupId); ps.setObject(2, userId); return ps.executeUpdate()>=0;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    private GroupChat map(ResultSet rs) throws SQLException {
        GroupChat g = new GroupChat();
        g.setId(rs.getObject("id", UUID.class));
        g.setName(rs.getString("name"));
        g.setCreator(rs.getObject("creator", UUID.class));
        g.setProfilePic(rs.getString("profile_pic"));
        return g;
    }
}
