package com.telegramapp.ui.controllers;

import com.telegramapp.dao.GroupDAO;
import com.telegramapp.dao.UserDAO;
import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.Group;
import com.telegramapp.model.User;
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
import java.util.List;

public class MainController {
    @FXML private ListView<User> usersListView;
    @FXML private ListView<Group> groupsListView;
    @FXML private ListView<Channel> channelsListView;
    @FXML private Label profileName;
    @FXML private Label profileUsername;
    @FXML private Region spacer; // <-- ADD THIS LINE

    private User currentUser;
    private UserDAO userDAO;
    private GroupDAO groupDAO;
    private ChannelDAOImpl channelDAO;

    public void setCurrentUser(User u) {
        this.currentUser = u;
        profileName.setText(u.getDisplayName());
        profileUsername.setText("@" + u.getUsername());

        // This is safe to run on the UI thread as it only configures existing components
        setupCellFactories();

        // Asynchronously initialize DAOs and load all data
        com.telegramapp.util.FX.runAsync(() -> {
            // --- THIS ENTIRE BLOCK NOW RUNS IN THE BACKGROUND ---
            try {
                // 1. Instantiate DAOs in the background (this creates the connection pool)
                UserDAO uDAO = new UserDAOImpl();
                GroupDAO gDAO = new GroupDAOImpl();
                ChannelDAOImpl cDAO = new ChannelDAOImpl();

                // 2. Fetch data using the new DAO instances
                List<User> users = uDAO.findAllExcept(currentUser.getId());
                List<Group> groups = gDAO.findByUser(currentUser.getId());
                List<Channel> channels = cDAO.findAll();

                // 3. Return a result object containing everything
                return new InitResult(uDAO, gDAO, cDAO, users, groups, channels);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, (result) -> {
            // --- THIS BLOCK RUNS ON THE UI THREAD ONCE THE BACKGROUND TASK IS DONE ---
            if (result != null) {
                // 1. Assign the fully initialized DAOs to the controller's fields
                this.userDAO = result.userDAO();
                this.groupDAO = result.groupDAO();
                this.channelDAO = result.channelDAO();

                // 2. Update the UI with the loaded data
                usersListView.getItems().setAll(result.users());
                groupsListView.getItems().setAll(result.groups());
                channelsListView.getItems().setAll(result.channels());
            }
        }, (error) -> {
            // Handle any unexpected errors
            error.printStackTrace();
        });
    }

    // Ensure this record is at the end of your MainController class
    private record LoadedData(List<User> users, List<Group> groups, List<Channel> channels) {}

    private void setupCellFactories() {
        usersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) setText(null);
                else setText(u.getDisplayName() == null || u.getDisplayName().isBlank() ? u.getUsername() : u.getDisplayName());
            }
        });

        groupsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? null : g.getName());
            }
        });

        channelsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Channel c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getName());
            }
        });

        usersListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                User sel = usersListView.getSelectionModel().getSelectedItem();
                if (sel != null) openPrivateChat(sel);
            }
        });

        groupsListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Group sel = groupsListView.getSelectionModel().getSelectedItem();
                if (sel != null) openGroupChat(sel);
            }
        });

        channelsListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Channel sel = channelsListView.getSelectionModel().getSelectedItem();
                if (sel != null) openChannel(sel);
            }
        });
    }

    private void refreshUsers() throws SQLException {
        usersListView.getItems().clear();
        List<User> others = userDAO.findAllExcept(currentUser.getId());
        usersListView.getItems().addAll(others);
    }

    private void refreshGroups() throws SQLException {
        groupsListView.getItems().clear();
        List<Group> groups = groupDAO.findByUser(currentUser.getId());
        groupsListView.getItems().addAll(groups);
    }

    private void refreshChannels() throws SQLException {
        channelsListView.getItems().clear();
        // show all channels for now; you can filter by subscriptions/owner later
        List<Channel> channels = channelDAO.findAll();
        channelsListView.getItems().addAll(channels);
    }

    private void openPrivateChat(User other) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load());
            ChatController ctrl = loader.getController();
            ctrl.init(currentUser, "USER", other.getId());

            Stage stage = new Stage();
            stage.setTitle("Chat with @" + other.getUsername());
            stage.setScene(scene);
            stage.show();
            stage.setOnCloseRequest(ev -> ctrl.onClose());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void openGroupChat(Group g) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load());
            ChatController ctrl = loader.getController();
            ctrl.init(currentUser, "GROUP", g.getId());

            Stage stage = new Stage();
            stage.setTitle("Group: " + g.getName());
            stage.setScene(scene);
            stage.show();
            stage.setOnCloseRequest(ev -> ctrl.onClose());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void openChannel(Channel c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load());
            ChatController ctrl = loader.getController();
            ctrl.init(currentUser, "CHANNEL", c.getId());

            Stage stage = new Stage();
            stage.setTitle("Channel: " + c.getName());
            stage.setScene(scene);
            stage.show();
            stage.setOnCloseRequest(ev -> ctrl.onClose());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void onNewGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Group");
            dialog.setScene(new Scene(loader.load()));
            GroupCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();
            refreshGroups();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void onNewChannel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/channel_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Channel");
            dialog.setScene(new Scene(loader.load()));
            ChannelCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();
            refreshChannels();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
    private record InitResult(UserDAO userDAO, GroupDAO groupDAO, ChannelDAOImpl channelDAO, List<User> users, List<Group> groups, List<Channel> channels) {}

}
