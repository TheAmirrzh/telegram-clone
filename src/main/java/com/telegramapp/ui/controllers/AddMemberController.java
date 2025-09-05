package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.Group;
import com.telegramapp.model.User;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class AddMemberController {

    @FXML private TextField searchField;
    @FXML private ListView<User> usersListView;
    @FXML private Label messageLabel;

    private Object chatEntity; // The Group or Channel to add a member to
    private List<String> existingMemberIds;

    public void initData(Object chatEntity, List<String> existingMemberIds) {
        this.chatEntity = chatEntity;
        this.existingMemberIds = existingMemberIds;
        loadPotentialMembers();

        // Add a listener to the search field to filter the list
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
    }

    private void loadPotentialMembers() {
        FX.runAsync(() -> {
            try {
                // Fetch all users and filter out those who are already members
                return new UserDAOImpl().findAll().stream()
                        .filter(user -> !existingMemberIds.contains(user.getId()))
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }, users -> {
            if (users != null) {
                usersListView.getItems().setAll(users);
                usersListView.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(User item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getDisplayName() + " (@" + item.getUsername() + ")");
                    }
                });
            }
        }, ex -> messageLabel.setText("Error loading users."));
    }

    private void filterUsers(String filter) {
        FX.runAsync(() -> {
            try {
                return new UserDAOImpl().findAll().stream()
                        .filter(user -> !existingMemberIds.contains(user.getId()))
                        .filter(user -> user.getDisplayName().toLowerCase().contains(filter.toLowerCase()) ||
                                user.getUsername().toLowerCase().contains(filter.toLowerCase()))
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }, users -> {
            if (users != null) {
                usersListView.getItems().setAll(users);
            }
        }, null);
    }


    @FXML
    private void onAdd() {
        User selectedUser = usersListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            messageLabel.setText("Please select a user to add.");
            return;
        }

        FX.runAsync(() -> {
            try {
                if (chatEntity instanceof Group) {
                    new GroupDAOImpl().addMember(((Group) chatEntity).getId(), selectedUser.getId(), "MEMBER");
                } else if (chatEntity instanceof Channel) {
                    new ChannelDAOImpl().addSubscriber(((Channel) chatEntity).getId(), selectedUser.getId());
                }
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, success -> {
            if (success) {
                closeWindow();
            } else {
                messageLabel.setText("Failed to add member.");
            }
        }, ex -> messageLabel.setText("Error: " + ex.getMessage()));
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) searchField.getScene().getWindow()).close();
    }
}
