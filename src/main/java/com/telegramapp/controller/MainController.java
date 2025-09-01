package com.telegramapp.controller;

import com.telegramapp.dao.ChannelDAO;
import com.telegramapp.dao.ChatDAO;
import com.telegramapp.dao.GroupDAO;
import com.telegramapp.dao.UserDAO;
import com.telegramapp.model.*;
import com.telegramapp.service.NotificationService;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class MainController {
    @FXML private ListView<String> chatList;
    @FXML private ListView<String> groupList;
    @FXML private ListView<String> channelList;
    @FXML private TextField searchField;
    @FXML private ListView<String> searchResults;

    private User currentUser;
    private final GroupDAO groupDAO = new GroupDAO();
    private final ChannelDAO channelDAO = new ChannelDAO();
    private final ChatDAO chatDAO = new ChatDAO();
    private final UserDAO userDAO = new UserDAO();
    private NotificationService notificationService;

    public void setCurrentUser(User u){
        this.currentUser = u;
        // start notification service
        notificationService = new NotificationService(u);
        notificationService.start();
        loadUserData();
    }

    private void loadUserData(){
        chatList.getItems().clear(); groupList.getItems().clear(); channelList.getItems().clear();
        FX.runAsync(() -> new Object[]{
            groupDAO.listGroupsForUser(currentUser.getId()),
            channelDAO.listAllChannels(),
            fetchOtherUsers()
        }, res -> {
            @SuppressWarnings("unchecked") var groups = (List<GroupChat>) res[0];
            @SuppressWarnings("unchecked") var channels = (List<Channel>) res[1];
            @SuppressWarnings("unchecked") var users = (List<User>) res[2];
            groupList.getItems().addAll(groups.stream().map(GroupChat::getName).toList());
            channelList.getItems().addAll(channels.stream().map(Channel::getName).toList());
            chatList.getItems().addAll(users.stream().map(User::getUsername).toList());
        }, Throwable::printStackTrace);
    }

    private List<User> fetchOtherUsers(){
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, profile_name FROM users WHERE id <> ? ORDER BY username";
        try (Connection c = com.telegramapp.util.DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setObject(1, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    User u = new User();
                    u.setId(rs.getObject("id", UUID.class));
                    u.setUsername(rs.getString("username"));
                    u.setProfileName(rs.getString("profile_name"));
                    list.add(u);
                }
            }
        } catch (Exception e){ e.printStackTrace(); }
        return list;
    }

    @FXML public void onChatSelect(){
        String username = chatList.getSelectionModel().getSelectedItem();
        if (username==null) return;
        FX.runAsync(() -> userDAO.findByUsername(username).orElse(null), other -> {
            if (other==null) return;
            UUID chatId = chatDAO.findOrCreatePrivateChat(currentUser.getId(), other.getId());
            openChat(chatId, null, null);
        }, Throwable::printStackTrace);
    }

    @FXML public void onGroupSelect(){
        String name = groupList.getSelectionModel().getSelectedItem();
        if (name==null) return;
        FX.runAsync(() -> {
            try (Connection c = com.telegramapp.util.DB.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id FROM groups WHERE name=?")){
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) return rs.getObject(1, UUID.class);
                }
            }
            return null;
        }, groupId -> openChat(null, groupId, null), Throwable::printStackTrace);
    }

    @FXML public void onChannelSelect(){
        String name = channelList.getSelectionModel().getSelectedItem();
        if (name==null) return;
        FX.runAsync(() -> {
            try (Connection c = com.telegramapp.util.DB.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id FROM channels WHERE name=?")){
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) return rs.getObject(1, UUID.class);
                }
            }
            return null;
        }, channelId -> openChat(null, null, channelId), Throwable::printStackTrace);
    }

    private void openChat(UUID privateChatId, UUID groupId, UUID channelId){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load());
            ChatController cc = loader.getController();
            cc.setCurrentUser(currentUser);
            if (privateChatId!=null) cc.setPrivateChatId(privateChatId);
            else if (groupId!=null) cc.setGroupId(groupId);
            else cc.setChannelId(channelId);
            Stage st = (Stage) chatList.getScene().getWindow();
            st.setScene(scene); st.show();
        } catch (IOException e){ e.printStackTrace(); }
    }

    @FXML public void onSearch(){
        String q = searchField.getText();
        if (q==null || q.isBlank()) return;
        FX.runAsync(() -> new com.telegramapp.dao.MessageDAO().searchMessages(q, currentUser.getId(), 50),
                list -> {
                    searchResults.getItems().clear();
                    for (com.telegramapp.model.Message m : list){
                        String label = String.format("%s | %s | %s", m.getTimestamp(), m.getSenderId(), m.getContent()==null?"[image]":m.getContent());
                        searchResults.getItems().add(label);
                    }
                }, Throwable::printStackTrace);
    }

    @FXML public void onSearchResultClick(){
        String sel = searchResults.getSelectionModel().getSelectedItem();
        if (sel==null) return;
        // Not implementing precise mapping UI->chat in this simple view; user can use info to navigate manually.
    }
}
