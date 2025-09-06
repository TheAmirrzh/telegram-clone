package com.telegramapp.ui.controllers;

import com.telegramapp.model.User;
import com.telegramapp.service.ContactService;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ManageContactsController {

    @FXML private ListView<User> contactsListView;
    @FXML private TextField searchField;
    @FXML private Label contactCountLabel;
    @FXML private Label messageLabel;
    @FXML private Button addContactButton;
    @FXML private Button removeContactButton;

    private ContactService contactService;
    private User currentUser;
    private MainController mainController;

    @FXML
    public void initialize() {
        try {
            this.contactService = new ContactService();
        } catch (SQLException e) {
            e.printStackTrace();
            FX.showError("Failed to initialize contact service.");
        }

        setupContactsList();
        setupSearchListener();
        setupSelectionListener();
    }

    public void initData(User currentUser, MainController mainController) {
        this.currentUser = currentUser;
        this.mainController = mainController;
        loadContacts();
        updateContactCount();
    }

    private void setupContactsList() {
        contactsListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox content;
            private final ImageView avatar;
            private final Label nameLabel;
            private final Label usernameLabel;
            private final Label statusLabel;

            {
                avatar = new ImageView();
                avatar.setFitHeight(40);
                avatar.setFitWidth(40);
                avatar.setClip(new Circle(20, 20, 20));

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
                    loadAvatar(item, avatar);
                    setGraphic(content);
                }
            }
        });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterContacts(newVal);
        });
    }

    private void setupSelectionListener() {
        contactsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            removeContactButton.setDisable(newUser == null);
        });
    }

    private void loadContacts() {
        FX.runAsync(() -> {
            try {
                return contactService.getContacts(currentUser.getId());
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load contacts");
            }
        }, contacts -> {
            contactsListView.getItems().setAll(contacts);
            updateContactCount();
        }, error -> {
            messageLabel.setText("Error loading contacts: " + error.getMessage());
        });
    }

    private void filterContacts(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            loadContacts();
            return;
        }

        FX.runAsync(() -> {
            try {
                List<User> allContacts = contactService.getContacts(currentUser.getId());
                return allContacts.stream()
                        .filter(user -> user.getDisplayName().toLowerCase().contains(filter.toLowerCase()) ||
                                user.getUsername().toLowerCase().contains(filter.toLowerCase()))
                        .toList();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to filter contacts");
            }
        }, filteredContacts -> {
            contactsListView.getItems().setAll(filteredContacts);
        }, error -> {
            messageLabel.setText("Error filtering contacts: " + error.getMessage());
        });
    }

    private void updateContactCount() {
        FX.runAsync(() -> {
            try {
                return contactService.getContactCount(currentUser.getId());
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }, count -> {
            contactCountLabel.setText(count + " contact" + (count != 1 ? "s" : ""));
        }, null);
    }

    @FXML
    private void onAddContact() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_contacts.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(contactsListView.getScene().getWindow());
            dialog.setTitle("Add Contact");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(scene);

            AddContactController controller = loader.getController();
            controller.initData(currentUser, contactService);

            dialog.showAndWait();

            // Refresh contacts list after dialog closes
            loadContacts();

            // Refresh main controller chat lists
            if (mainController != null) {
                mainController.loadAllChatLists();
            }

        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Failed to open add contact dialog.");
        }
    }

    @FXML
    private void onRemoveContact() {
        User selectedContact = contactsListView.getSelectionModel().getSelectedItem();
        if (selectedContact == null) {
            messageLabel.setText("Please select a contact to remove.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Remove Contact");
        confirmDialog.setHeaderText("Remove " + selectedContact.getDisplayName() + " from contacts?");
        confirmDialog.setContentText("This will remove them from your contacts list. You can still message them.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            FX.runAsync(() -> {
                try {
                    return contactService.removeContact(currentUser.getId(), selectedContact.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to remove contact");
                }
            }, success -> {
                if (success) {
                    messageLabel.setText("Contact removed successfully.");
                    loadContacts();

                    // Refresh main controller chat lists
                    if (mainController != null) {
                        mainController.loadAllChatLists();
                    }
                } else {
                    messageLabel.setText("Contact was not in your list.");
                }
            }, error -> {
                messageLabel.setText("Error removing contact: " + error.getMessage());
            });
        }
    }

    @FXML
    private void onViewProfile() {
        User selectedContact = contactsListView.getSelectionModel().getSelectedItem();
        if (selectedContact == null) {
            messageLabel.setText("Please select a contact to view profile.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(contactsListView.getScene().getWindow());
            dialog.setTitle("Contact Profile");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(scene);

            ProfileController controller = loader.getController();
            controller.initData(selectedContact, currentUser); // View-only mode

            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Failed to open contact profile.");
        }
    }

    @FXML
    private void onClose() {
        ((Stage) contactsListView.getScene().getWindow()).close();
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