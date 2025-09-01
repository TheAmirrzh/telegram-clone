package com.telegramapp.controller;

import com.telegramapp.App;
import com.telegramapp.dao.ChannelDAO;
import com.telegramapp.dao.ChatDAO;
import com.telegramapp.dao.GroupDAO;
import com.telegramapp.dao.UserDAO;
import com.telegramapp.model.Channel;
import com.telegramapp.model.GroupChat;
import com.telegramapp.model.User;
import com.telegramapp.util.FX;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {
    @FXML private StackPane rootPane;
    @FXML private ListView<Object> chatList;
    @FXML private BorderPane chatViewPlaceholder;
    @FXML private TextField searchField;

    private User currentUser;
    private final GroupDAO groupDAO = new GroupDAO();
    private final ChannelDAO channelDAO = new ChannelDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ChatDAO chatDAO = new ChatDAO();

    public void setCurrentUser(User u){
        this.currentUser = u;
        setTheme("theme-day");
        setupChatList();
        loadUserData();
    }

    private void setupChatList() {
        chatList.setCellFactory(lv -> {
            ListCell<Object> cell = new ListCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else if (item instanceof User) {
                        setText(((User) item).getProfileName());
                    } else if (item instanceof GroupChat) {
                        setText(((GroupChat) item).getName());
                    } else if (item instanceof Channel) {
                        setText(((Channel) item).getName());
                    }
                }
            };

            // Create ContextMenu for deleting chats
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Conversation");
            deleteItem.setOnAction(event -> {
                Object item = cell.getItem();
                deleteConversation(item);
            });
            contextMenu.getItems().add(deleteItem);

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });

        chatList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                openChat(newVal);
            }
        });
    }

    private void deleteConversation(Object item) {
        // NOTE: This is a placeholder for the actual delete logic.
        // In a real app, this would call a DAO method to delete messages or memberships.
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this conversation?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                chatList.getItems().remove(item);
                chatViewPlaceholder.setCenter(null); // Clear the view
                System.out.println("Deleted conversation with: " + item);
            }
        });
    }

    private void loadUserData(){
        FX.runAsync(() -> {
            List<GroupChat> groups = groupDAO.listGroupsForUser(currentUser.getId());
            List<Channel> channels = channelDAO.listAllChannels();
            List<User> users = userDAO.findAllUsersExcept(currentUser.getId());
            return Stream.of(users, groups, channels)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }, combinedList -> {
            chatList.getItems().setAll(combinedList);
        }, Throwable::printStackTrace);
    }

    private void openChat(Object chatTarget) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Parent chatRoot = loader.load();

            // --- Add a fade-in animation ---
            FadeTransition ft = new FadeTransition(Duration.millis(300), chatRoot);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            ChatController activeChatController = loader.getController();
            activeChatController.setCurrentUser(currentUser);

            if (chatTarget instanceof User) {
                activeChatController.setPrivateChatTarget((User) chatTarget);
            } else if (chatTarget instanceof GroupChat) {
                activeChatController.setGroupId(((GroupChat) chatTarget).getId());
            } else if (chatTarget instanceof Channel) {
                activeChatController.setChannelId(((Channel) chatTarget).getId());
            }

            chatViewPlaceholder.setCenter(chatRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onViewProfile() {
        // Placeholder for profile view logic
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profile");
        alert.setHeaderText(currentUser.getProfileName());
        alert.setContentText("Username: " + currentUser.getUsername() + "\nStatus: Online");
        alert.showAndWait();
    }

    @FXML
    private void onLogout() {
        try {
            // Get the current stage
            Stage stage = (Stage) rootPane.getScene().getWindow();

            // Load the login screen
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(App.class.getResource("/css/styles.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Telegram Clone - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void setTheme(String theme) {
        rootPane.getStyleClass().removeIf(s -> s.startsWith("theme-"));
        rootPane.getStyleClass().add(theme);
    }
    @FXML private void setThemeDay() { setTheme("theme-day"); }
}