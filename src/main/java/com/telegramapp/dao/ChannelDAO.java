package com.telegramapp.dao;

import com.telegramapp.model.Channel;
import com.telegramapp.util.DB;

import java.sql.*;
import java.util.*;

public class ChannelDAO {
    public List<Channel> listAllChannels(){
        String sql = "SELECT id, name, owner, profile_pic FROM channels ORDER BY name";
        List<Channel> list = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()) list.add(map(rs));
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return list;
    }

    public boolean create(Channel ch){
        String sql = "INSERT INTO channels (id, name, owner, profile_pic) VALUES (?, ?, ?, ?)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, ch.getId());
            ps.setString(2, ch.getName());
            ps.setObject(3, ch.getOwner());
            ps.setString(4, ch.getProfilePic());
            return ps.executeUpdate()==1;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    public boolean subscribe(UUID channelId, UUID userId){
        String sql = "INSERT INTO channel_subscribers (channel_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, channelId); ps.setObject(2, userId); return ps.executeUpdate()>=0;
        } catch (SQLException e){ e.printStackTrace(); return false; }
    }

    private Channel map(ResultSet rs) throws SQLException {
        Channel c = new Channel();
        c.setId(rs.getObject("id", UUID.class));
        c.setName(rs.getString("name"));
        c.setOwner(rs.getObject("owner", UUID.class));
        c.setProfilePic(rs.getString("profile_pic"));
        return c;
    }
}
