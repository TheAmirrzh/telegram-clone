package com.telegramapp.ui.controllers;

import com.telegramapp.App;
import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.ChatListItem;
import com.telegramapp.model.Group;
import com.telegramapp.model.User;
import com.telegramapp.model.Message;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {
    @FXML private ListView<ChatListItem> usersListView;
    @FXML private ListView<ChatListItem> groupsListView;
    @FXML private ListView<ChatListItem> channelsListView;
    @FXML private StackPane chatArea;
    @FXML private Label chatAreaPlaceholder;
    @FXML private ImageView profileImageView;
    @FXML private Label profileNameLabel;
    @FXML private Label profileUsernameLabel;
    @FXML private HBox profileContainer;

    private User currentUser;
    private ChatController activeChatController;
    private UserDAOImpl userDAO = new UserDAOImpl();
    private GroupDAOImpl groupDAO = new GroupDAOImpl();
    private ChannelDAOImpl channelDAO = new ChannelDAOImpl();
    private MessageDAOImpl messageDAO = new MessageDAOImpl();

    @FXML
    public void initialize() {
        if (profileImageView != null) {
            Circle clip = new Circle(20, 20, 20);
            profileImageView.setClip(clip);
        }
    }

    public void setCurrentUser(User u) {
        this.currentUser = u;
        refreshProfileView();
        setupSelectionListeners();
        loadAllChatLists();
    }

    private void refreshProfileView() {
        if (currentUser == null) return;
        profileNameLabel.setText(currentUser.getDisplayName());
        profileUsernameLabel.setText("@" + currentUser.getUsername());
        loadAvatar(currentUser, profileImageView);
    }

    private void loadAvatar(User user, ImageView imageView) {
        if (user == null || imageView == null) {
            if (imageView != null) imageView.setImage(null);
            return;
        }
        String picPath = user.getProfilePicPath();
        Image avatarImage = null;

        if (picPath != null && !picPath.isBlank()) {
            try (FileInputStream fis = new FileInputStream(new File(picPath))) {
                avatarImage = new Image(fis);
            } catch (Exception e) {
                System.err.println("Failed to load custom avatar: " + picPath);
                avatarImage = null;
            }
        }

        if (avatarImage == null) {
            try (InputStream defaultAvatarStream = getClass().getResourceAsStream("/assets/default_avatar.png")) {
                if (defaultAvatarStream != null) {
                    avatarImage = new Image(defaultAvatarStream);
                } else {
                    System.err.println("FATAL: Could not find default avatar resource: /assets/default_avatar.png");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        imageView.setImage(avatarImage);
    }

    private ListCell<ChatListItem> createChatListCell() {
        return new ListCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label nameLabel = new Label();
            private final Label lastMessageLabel = new Label();
            private final VBox textVBox = new VBox(nameLabel, lastMessageLabel);
            private final HBox contentHBox = new HBox(10, imageView, textVBox);

            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setClip(new Circle(20, 20, 20));
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                lastMessageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
                contentHBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(ChatListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(item.getDisplayName());
                    lastMessageLabel.setText(item.getLastMessage());
                    loadAvatar(item.getUser(), imageView);
                    setGraphic(contentHBox);
                }
            }
        };
    }

    private void setupSelectionListeners() {
        usersListView.setCellFactory(param -> createChatListCell());
        groupsListView.setCellFactory(param -> createChatListCell());
        channelsListView.setCellFactory(param -> createChatListCell());

        usersListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                groupsListView.getSelectionModel().clearSelection();
                channelsListView.getSelectionModel().clearSelection();
                openPrivateChat((User) item.getChatObject());
            }
        });

        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                usersListView.getSelectionModel().clearSelection();
                channelsListView.getSelectionModel().clearSelection();
                openGroupChat((Group) item.getChatObject());
            }
        });

        channelsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                usersListView.getSelectionModel().clearSelection();
                groupsListView.getSelectionModel().clearSelection();
                openChannel((Channel) item.getChatObject());
            }
        });
    }

    private void loadAllChatLists() {
        FX.runAsync(() -> {
            try {
                return userDAO.findAllExcept(currentUser.getId()).stream()
                        .map(user -> {
                            try {
                                String lastMsg = messageDAO.findLastMessageForChat("USER", user.getId(), currentUser.getId())
                                        .map(Message::getContent).orElse("No messages yet");
                                return new ChatListItem(user, lastMsg);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, usersListView.getItems()::setAll, ex -> {
            ex.printStackTrace();
            FX.showError("Failed to load user list.");
        });

        FX.runAsync(() -> {
            try {
                return groupDAO.findByUser(currentUser.getId()).stream()
                        .map(group -> {
                            try {
                                String lastMsg = messageDAO.findLastMessageForChat("GROUP", group.getId(), currentUser.getId())
                                        .map(Message::getContent).orElse("No messages yet");
                                return new ChatListItem(group, lastMsg);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, groupsListView.getItems()::setAll, ex -> {
            ex.printStackTrace();
            FX.showError("Failed to load group list.");
        });

        FX.runAsync(() -> {
            try {
                return channelDAO.findAll().stream()
                        .map(channel -> {
                            try {
                                String lastMsg = messageDAO.findLastMessageForChat("CHANNEL", channel.getId(), currentUser.getId())
                                        .map(Message::getContent).orElse("No messages yet");
                                return new ChatListItem(channel, lastMsg);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, channelsListView.getItems()::setAll, ex -> {
            ex.printStackTrace();
            FX.showError("Failed to load channel list.");
        });
    }


    private void openPrivateChat(User other) {
        openChatView("USER", other.getId());
    }

    private void openGroupChat(Group g) {
        openChatView("GROUP", g.getId());
    }

    private void openChannel(Channel c) {
        openChatView("CHANNEL", c.getId());
    }

    private void openChatView(String receiverType, String receiverId) {
        if (activeChatController != null) {
            activeChatController.onClose();
        }
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/chat.fxml"));
            Node chatView = loader.load();
            activeChatController = loader.getController();

            // CRITICAL FIX: Pass data to the controller AFTER it's fully initialized.
            activeChatController.loadChatData(currentUser, receiverType, receiverId);

            chatArea.getChildren().setAll(chatView);
            chatAreaPlaceholder.setVisible(false);
        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Failed to load chat interface. Check FXML file and controller.");
        }
    }

    @FXML
    private void onNewGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/group_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Group");
            dialog.setScene(new Scene(loader.load()));
            GroupCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();
            loadAllChatLists();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            FX.showError("Could not open the new group dialog.");
        }
    }

    @FXML
    private void onNewChannel() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/channel_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Channel");
            dialog.setScene(new Scene(loader.load()));
            ChannelCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();
            loadAllChatLists();
        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Could not open the new channel dialog.");
        }
    }

    @FXML
    private void onEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/profile.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Edit Profile");
            dialog.setScene(new Scene(loader.load()));
            ProfileController ctrl = loader.getController();
            ctrl.initData(currentUser);
            dialog.showAndWait();
            refreshProfileView();
        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Could not open profile settings.");
        }
    }
}

