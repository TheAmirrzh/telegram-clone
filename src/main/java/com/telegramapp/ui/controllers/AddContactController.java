
package com.telegramapp.ui.controllers;

import com.telegramapp.model.User;
import com.telegramapp.service.ContactService;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class AddContactController {

    @FXML private TextField searchField;
    @FXML private ListView<User> usersListView;
    @FXML private Label messageLabel;
    @FXML private Button addButton;

    private ContactService contactService;
    private User currentUser;

    @FXML
    public void initialize() {
        setupUsersList();
        setupSearchListener();
        setupSelectionListener();

        searchUsers("");
        searchField.requestFocus();
    }

    public void initData(User currentUser, ContactService contactService) {
        this.currentUser = currentUser;
        this.contactService = contactService;
        searchUsers("");
    }

    private void setupUsersList() {
        usersListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox content;
            private final ImageView avatar;
            private final Label nameLabel;
            private final Label usernameLabel;
            private final Label statusLabel;

            {
                avatar = new ImageView();
                avatar.setFitHeight(36);
                avatar.setFitWidth(36);
                avatar.setClip(new Circle(18, 18, 18));

                nameLabel = new Label();
                nameLabel.getStyleClass().add("chat-list-name-label");

                usernameLabel = new Label();
                usernameLabel.getStyleClass().add("chat-list-message-label");

                statusLabel = new Label();
                statusLabel.getStyleClass().add("chat-status-label");

                VBox textContainer = new VBox(2, nameLabel, usernameLabel, statusLabel);

                content = new HBox(10, avatar, textContainer);
                content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                content.setPadding(new javafx.geometry.Insets(5));
            }

            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(item.getDisplayName());
                    usernameLabel.setText("@" + item.getUsername());
                    statusLabel.setText(item.getStatus());

                    // Show if already a contact
                    try {
                        if (contactService.isContact(currentUser.getId(), item.getId())) {
                            statusLabel.setText("Already in contacts");
                            statusLabel.getStyleClass().add("success-label");
                        }
                    } catch (SQLException e) {
                        // Ignore for display purposes
                    }

                    loadAvatar(item, avatar);
                    setGraphic(content);
                }
            }
        });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            searchUsers(newVal.trim());
        });
    }

    private void setupSelectionListener() {
        usersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null) {
                // Check if already a contact
                FX.runAsync(() -> {
                    try {
                        return contactService.isContact(currentUser.getId(), newUser.getId());
                    } catch (SQLException e) {
                        return false;
                    }
                }, isAlreadyContact -> {
                    addButton.setDisable(isAlreadyContact);
                    if (isAlreadyContact) {
                        messageLabel.setText(newUser.getDisplayName() + " is already in your contacts");
                    } else {
                        messageLabel.setText("Select a user to add to contacts");
                    }
                }, null);
            } else {
                addButton.setDisable(true);
                messageLabel.setText("Select a user to add to contacts");
            }
        });
    }

    private void searchUsers(String query) {
        FX.runAsync(() -> {
            try {

                List<User> results = contactService.searchUsersForContacts(query, currentUser.getId());

                // Filter out existing contacts for cleaner UI
                List<User> filteredResults = results.stream()
                        .filter(user -> {
                            try {
                                return !contactService.isContact(currentUser.getId(), user.getId());
                            } catch (SQLException e) {
                                return true; // Include in case of error
                            }
                        })
                        .collect(Collectors.toList());

                return filteredResults;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to search users");
            }
        }, users -> {
            usersListView.getItems().setAll(users);
            if (users.isEmpty()) {
                if (query.isEmpty()) {
                    messageLabel.setText("No new users to add.");
                } else {
                    messageLabel.setText("No users found matching '" + query + "'");
                }
            } else {
                if (query.isEmpty()) {
                    messageLabel.setText("Showing all available users.");
                } else {
                    messageLabel.setText("Found " + users.size() + " user(s)");
                }
            }
        }, error -> {
            messageLabel.setText("Error searching users: " + error.getMessage());
        });
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
                return contactService.addContact(currentUser.getId(), selectedUser.getId());
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to add contact");
            }
        }, success -> {
            if (success) {
                messageLabel.setText(selectedUser.getDisplayName() + " added to contacts!");

                // Refresh the list to show updated status
                searchUsers(searchField.getText().trim());

                // Close dialog after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(this::closeDialog);
                    } catch (InterruptedException ignored) {}
                }).start();

            } else {
                messageLabel.setText("User is already in your contacts.");
            }
        }, error -> {
            messageLabel.setText("Error adding contact: " + error.getMessage());
        });
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) searchField.getScene().getWindow()).close();
    }

    private void loadAvatar(User user, ImageView imageView) {
        if (user == null || imageView == null) return;

        String picPath = user.getProfilePicPath();
        Image avatarImage = null;

        if (picPath != null && !picPath.isBlank()) {
            try (FileInputStream fis = new FileInputStream(new File(picPath))) {
                avatarImage = new Image(fis);
            } catch (Exception e) {
                // Fallback to default
            }
        }

        if (avatarImage == null) {
            try (InputStream defaultAvatarStream = getClass().getResourceAsStream("/assets/default_avatar.png")) {
                if (defaultAvatarStream != null) {
                    avatarImage = new Image(defaultAvatarStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        imageView.setImage(avatarImage);
    }
}