package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.*;
import com.telegramapp.util.FX;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML private VBox mainContainer;
    @FXML private ListView<ChatListItem> usersListView;
    @FXML private ListView<ChatListItem> groupsListView;
    @FXML private ListView<ChatListItem> channelsListView;
    @FXML private StackPane chatArea;
    @FXML private Label chatAreaPlaceholder;
    @FXML private HBox profileContainer;
    @FXML private ImageView profileImageView;
    @FXML private Label profileNameLabel;
    @FXML private Label profileUsernameLabel;
    @FXML private StackPane themeToggleContainer;
    @FXML private ImageView sunIcon;
    @FXML private ImageView moonIcon;

    private User currentUser;
    private UserDAOImpl userDAO;
    private GroupDAOImpl groupDAO;
    private ChannelDAOImpl channelDAO;
    private MessageDAOImpl messageDAO;
    private ChatController activeChatController;
    private boolean isDarkMode = false;


    @FXML
    public void initialize() {
        this.userDAO = new UserDAOImpl();
        this.groupDAO = new GroupDAOImpl();
        this.channelDAO = new ChannelDAOImpl();
        this.messageDAO = new MessageDAOImpl();
        setupSelectionListeners();
        profileContainer.setOnMouseClicked(event -> onEditProfile());

        // Setup theme toggle - This is the correct way to handle the click
        themeToggleContainer.setOnMouseClicked(event -> toggleTheme());
        sunIcon.setOpacity(0);
        moonIcon.setOpacity(1);
    }

    public void setCurrentUser(User u) {
        this.currentUser = u;
        refreshProfileView();
        loadAllChatLists();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;

        FadeTransition sunFade = new FadeTransition(Duration.millis(300), sunIcon);
        FadeTransition moonFade = new FadeTransition(Duration.millis(300), moonIcon);

        if (isDarkMode) {
            mainContainer.getStyleClass().add("theme-dark");
            sunFade.setToValue(1);
            moonFade.setToValue(0);
        } else {
            mainContainer.getStyleClass().remove("theme-dark");
            sunFade.setToValue(0);
            moonFade.setToValue(1);
        }
        sunFade.play();
        moonFade.play();
    }

    private void refreshProfileView() {
        if (currentUser == null) return;
        profileNameLabel.setText(currentUser.getDisplayName());
        profileUsernameLabel.setText("@" + currentUser.getUsername());
        loadChatAvatar(currentUser, profileImageView);
    }

    private void loadChatAvatar(Object chatObject, ImageView imageView) {
        if (imageView == null) return;

        Image avatarImage = null;
        String picPath = null;
        String defaultAvatarResource = "/assets/default_avatar.png";

        if (chatObject instanceof User) {
            picPath = ((User) chatObject).getProfilePicPath();
        } else if (chatObject instanceof Group) {
            defaultAvatarResource = "/assets/default_group_avatar.png";
        } else if (chatObject instanceof Channel) {
            defaultAvatarResource = "/assets/default_channel_avatar.png";
        }

        if (picPath != null && !picPath.isBlank()) {
            try (FileInputStream fis = new FileInputStream(new File(picPath))) {
                avatarImage = new Image(fis);
            } catch (Exception e) {
                System.err.println("Failed to load user avatar: " + picPath);
            }
        }

        if (avatarImage == null) {
            try (InputStream defaultAvatarStream = getClass().getResourceAsStream(defaultAvatarResource)) {
                if (defaultAvatarStream != null) {
                    avatarImage = new Image(defaultAvatarStream);
                } else {
                    System.err.println("CRITICAL: Default avatar not found: " + defaultAvatarResource);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        imageView.setImage(avatarImage);
        if (imageView.getClip() == null) {
            imageView.setClip(new Circle(20, 20, 20));
        }
    }

    private ListCell<ChatListItem> createChatListCell() {
        return new ListCell<>() {
            private final HBox content;
            private final ImageView avatar;
            private final Label nameLabel;
            private final Label lastMessageLabel;
            private final Label unreadCountLabel;
            private final StackPane notificationPane;

            // Initializer block to create the cell structure once
            {
                avatar = new ImageView();
                avatar.setFitHeight(40);
                avatar.setFitWidth(40);
                avatar.setClip(new Circle(20, 20, 20));

                nameLabel = new Label();
                nameLabel.getStyleClass().add("chat-list-name-label");

                lastMessageLabel = new Label();
                lastMessageLabel.getStyleClass().add("chat-list-message-label");

                VBox textContainer = new VBox(2, nameLabel, lastMessageLabel);

                unreadCountLabel = new Label();
                unreadCountLabel.getStyleClass().add("notification-count-label");
                StackPane badge = new StackPane(unreadCountLabel);
                badge.getStyleClass().add("notification-badge");
                notificationPane = new StackPane(badge);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox mainContent = new HBox(5, textContainer, spacer, notificationPane);
                mainContent.setAlignment(Pos.CENTER_LEFT);

                content = new HBox(10, avatar, mainContent);
                content.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(ChatListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(item.getDisplayName());
                    lastMessageLabel.setText(item.getLastMessage());

                    if (item.getUnreadCount() > 0) {
                        unreadCountLabel.setText(String.valueOf(item.getUnreadCount()));
                        notificationPane.setVisible(true);
                    } else {
                        notificationPane.setVisible(false);
                    }

                    loadChatAvatar(item.getChatObject(), avatar);
                    setGraphic(content);
                }
            }
        };
    }

    private void setupSelectionListeners() {
        usersListView.setCellFactory(lv -> createChatListCell());
        groupsListView.setCellFactory(lv -> createChatListCell());
        channelsListView.setCellFactory(lv -> createChatListCell());

        usersListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                groupsListView.getSelectionModel().clearSelection();
                channelsListView.getSelectionModel().clearSelection();
                openPrivateChat(item);
            }
        });

        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                usersListView.getSelectionModel().clearSelection();
                channelsListView.getSelectionModel().clearSelection();
                openGroupChat(item);
            }
        });

        channelsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                usersListView.getSelectionModel().clearSelection();
                groupsListView.getSelectionModel().clearSelection();
                openChannel(item);
            }
        });
    }

    private void loadAllChatLists() {
        FX.runAsync(() -> {
            try {
                List<User> users = userDAO.findAllExcept(currentUser.getId());
                List<ChatListItem> userItems = users.stream()
                        .map(user -> {
                            try {
                                Optional<Message> lastMessageOpt = messageDAO.findLastMessageForChat("USER", user.getId(), currentUser.getId());
                                int unreadCount = messageDAO.getUnreadMessageCount("USER", user.getId(), currentUser.getId());
                                String msgText = lastMessageOpt.map(Message::getContent).orElse("No messages yet");
                                LocalDateTime ts = lastMessageOpt.map(Message::getTimestamp).orElse(LocalDateTime.MIN);
                                return new ChatListItem(user, msgText, unreadCount, ts);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());

                List<Group> groups = groupDAO.findByUser(currentUser.getId());
                List<ChatListItem> groupItems = groups.stream()
                        .map(group -> {
                            try {
                                Optional<Message> lastMessageOpt = messageDAO.findLastMessageForChat("GROUP", group.getId(), currentUser.getId());
                                int unreadCount = messageDAO.getUnreadMessageCount("GROUP", group.getId(), currentUser.getId());
                                String msgText = lastMessageOpt.map(Message::getContent).orElse("No messages yet");
                                LocalDateTime ts = lastMessageOpt.map(Message::getTimestamp).orElse(LocalDateTime.MIN);
                                return new ChatListItem(group, msgText, unreadCount, ts);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());

                List<Channel> channels = channelDAO.findAll();
                List<ChatListItem> channelItems = channels.stream()
                        .map(channel -> {
                            try {
                                Optional<Message> lastMessageOpt = messageDAO.findLastMessageForChat("CHANNEL", channel.getId(), currentUser.getId());
                                int unreadCount = messageDAO.getUnreadMessageCount("CHANNEL", channel.getId(), currentUser.getId());
                                String msgText = lastMessageOpt.map(Message::getContent).orElse("No messages yet");
                                LocalDateTime ts = lastMessageOpt.map(Message::getTimestamp).orElse(LocalDateTime.MIN);
                                return new ChatListItem(channel, msgText, unreadCount, ts);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());

                Comparator<ChatListItem> sorter = Comparator.comparing(ChatListItem::getLastMessageTimestamp).reversed();
                userItems.sort(sorter);
                groupItems.sort(sorter);
                channelItems.sort(sorter);

                return List.of(userItems, groupItems, channelItems);

            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }, (lists) -> {
            if (lists.size() == 3) {
                usersListView.getItems().setAll((List<ChatListItem>) lists.get(0));
                groupsListView.getItems().setAll((List<ChatListItem>) lists.get(1));
                channelsListView.getItems().setAll((List<ChatListItem>) lists.get(2));
            }
        }, null);
    }

    private void openPrivateChat(ChatListItem item) {
        if (item.getChatObject() instanceof User) {
            openChatView("USER", ((User) item.getChatObject()).getId());
        }
    }

    private void openGroupChat(ChatListItem item) {
        if (item.getChatObject() instanceof Group) {
            openChatView("GROUP", ((Group) item.getChatObject()).getId());
        }
    }

    private void openChannel(ChatListItem item) {
        if (item.getChatObject() instanceof Channel) {
            openChatView("CHANNEL", ((Channel) item.getChatObject()).getId());
        }
    }

    private void openChatView(String type, String id) {
        if (activeChatController != null) {
            activeChatController.onClose();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Node chatNode = loader.load();
            activeChatController = loader.getController();
            activeChatController.loadChatData(currentUser, type, id);
            chatArea.getChildren().setAll(chatNode);
            chatAreaPlaceholder.setVisible(false);
        } catch (IOException ex) {
            ex.printStackTrace();
            chatAreaPlaceholder.setText("Error loading chat.");
            chatAreaPlaceholder.setVisible(true);
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
            loadAllChatLists();
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
            loadAllChatLists();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(profileContainer.getScene().getWindow());
            dialog.setTitle("Edit Profile");
            dialog.setScene(new Scene(loader.load()));
            ProfileController ctrl = loader.getController();
            ctrl.initData(currentUser, currentUser);
            dialog.showAndWait();
            refreshProfileView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

