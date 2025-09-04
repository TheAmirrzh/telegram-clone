package com.telegramapp.ui.controllers;

import com.telegramapp.dao.ChannelDAO;
import com.telegramapp.dao.GroupDAO;
import com.telegramapp.dao.UserDAO;
import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.Group;
import com.telegramapp.model.User;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class MainController {
    @FXML private ListView<User> usersListView;
    @FXML private ListView<Group> groupsListView;
    @FXML private ListView<Channel> channelsListView;
    @FXML private Label profileName;
    @FXML private Label profileUsername;
    @FXML private Region spacer;

    private User currentUser;
    private UserDAO userDAO;
    private GroupDAO groupDAO;
    private ChannelDAO channelDAO;

    private record InitResult(UserDAO userDAO, GroupDAO groupDAO, ChannelDAO channelDAO, List<User> users, List<Group> groups, List<Channel> channels) {}

    public void setCurrentUser(User u) {
        this.currentUser = u;
        profileName.setText(u.getDisplayName());
        profileUsername.setText("@" + u.getUsername());
        setupCellFactories();

        FX.runAsync(() -> {
            try {
                UserDAO uDAO = new UserDAOImpl();
                GroupDAO gDAO = new GroupDAOImpl();
                ChannelDAO cDAO = new ChannelDAOImpl();
                List<User> users = uDAO.findAllExcept(currentUser.getId());
                List<Group> groups = gDAO.findByUser(currentUser.getId());
                List<Channel> channels = cDAO.findAll();
                return new InitResult(uDAO, gDAO, cDAO, users, groups, channels);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, (result) -> {
            if (result != null) {
                this.userDAO = result.userDAO();
                this.groupDAO = result.groupDAO();
                this.channelDAO = result.channelDAO();
                usersListView.getItems().setAll(result.users());
                groupsListView.getItems().setAll(result.groups());
                channelsListView.getItems().setAll(result.channels());
            }
        }, Throwable::printStackTrace);
    }

    private void setupCellFactories() {
        usersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        groupsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Group item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        channelsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Channel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        usersListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && usersListView.getSelectionModel().getSelectedItem() != null) {
                openPrivateChat(usersListView.getSelectionModel().getSelectedItem());
            }
        });
        groupsListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && groupsListView.getSelectionModel().getSelectedItem() != null) {
                openGroupChat(groupsListView.getSelectionModel().getSelectedItem());
            }
        });
        channelsListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && channelsListView.getSelectionModel().getSelectedItem() != null) {
                openChannel(channelsListView.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void openPrivateChat(User other) {
        openChatWindow("Chat with @" + other.getUsername(), "USER", other.getId());
    }

    private void openGroupChat(Group g) {
        openChatWindow("Group: " + g.getName(), "GROUP", g.getId());
    }

    private void openChannel(Channel c) {
        openChatWindow("Channel: " + c.getName(), "CHANNEL", c.getId());
    }

    private void openChatWindow(String title, String receiverType, String receiverId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load());
            ChatController ctrl = loader.getController();
            ctrl.init(currentUser, receiverType, receiverId);
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setOnCloseRequest(ev -> ctrl.onClose());
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onNewGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Group");
            dialog.setScene(new Scene(loader.load()));
            GroupCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();

            FX.runAsync(() -> {
                try {
                    return groupDAO.findByUser(currentUser.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                    return Collections.<Group>emptyList();
                }
            }, (groups) -> groupsListView.getItems().setAll(groups), Throwable::printStackTrace);

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNewChannel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/channel_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Channel");
            dialog.setScene(new Scene(loader.load()));
            ChannelCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();

            FX.runAsync(() -> {
                try {
                    return channelDAO.findAll();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return Collections.<Channel>emptyList();
                }
            }, (channels) -> channelsListView.getItems().setAll(channels), Throwable::printStackTrace);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}