package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.db.DBConnection;
import com.telegramapp.model.Group;
import com.telegramapp.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class GroupCreateController {
    @FXML private TextField groupNameField;
    @FXML private ListView<User> availableUsersList;
    @FXML private ListView<User> selectedUsersList;
    @FXML private Label messageLabel;

    private User currentUser;

    public void init(User currentUser) throws SQLException {
        this.currentUser = currentUser;
        loadAvailableUsers();
    }

    private void loadAvailableUsers() throws SQLException {
        availableUsersList.getItems().clear();
        var userDAO = new UserDAOImpl();
        var others = userDAO.findAllExcept(currentUser.getId());
        availableUsersList.getItems().addAll(others);
        availableUsersList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) setText(null);
                else setText(u.getDisplayName() == null || u.getDisplayName().isBlank() ? u.getUsername() : u.getDisplayName());
            }
        });

        selectedUsersList.setCellFactory(availableUsersList.getCellFactory());
    }

    public void onAddSelected() {
        User sel = availableUsersList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            availableUsersList.getItems().remove(sel);
            selectedUsersList.getItems().add(sel);
        }
    }

    public void onRemoveSelected() {
        User sel = selectedUsersList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            selectedUsersList.getItems().remove(sel);
            availableUsersList.getItems().add(sel);
        }
    }

    public void onCreate() {
        String name = groupNameField.getText().trim();
        if (name.isEmpty()) {
            messageLabel.setText("Group name required.");
            return;
        }

        String groupId = UUID.randomUUID().toString();
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Create the group
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO groups (id, name, creator_id) VALUES (?, ?, ?)")) {
                ps.setString(1, groupId);
                ps.setString(2, name);
                ps.setString(3, currentUser.getId());
                ps.executeUpdate();
            }

            // 2. Add the creator as a member with the 'CREATOR' role
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)")) {
                ps.setString(1, groupId);
                ps.setString(2, currentUser.getId());
                ps.setString(3, "CREATOR"); // Assign creator role
                ps.executeUpdate();
            }

            // 3. Add all other selected users with the default 'MEMBER' role
            List<User> selected = List.copyOf(selectedUsersList.getItems());
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)")) {
                for (User u : selected) {
                    ps.setString(1, groupId);
                    ps.setString(2, u.getId());
                    ps.setString(3, "MEMBER"); // Assign member role
                    try { ps.executeUpdate(); } catch (SQLException ex) { if (!"23505".equals(ex.getSQLState())) throw ex; }
                }
            }

            conn.commit();
            messageLabel.setText("Group created.");
            Stage stage = (Stage) groupNameField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error creating group: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void onCancel() {
        Stage stage = (Stage) groupNameField.getScene().getWindow();
        stage.close();
    }
}
