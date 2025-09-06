package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.*;
import com.telegramapp.service.ContactService;
import com.telegramapp.util.FX;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainController {

    // --- FXML UI Elements ---
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
    @FXML private ImageView logoImageView;
    @FXML private TextField globalSearchField;
    @FXML private ListView<Object> searchResultsListView;
    @FXML private StackPane searchResultsContainer;


    // --- Services and DAOs ---
    private UserDAOImpl userDAO;
    private GroupDAOImpl groupDAO;
    private ChannelDAOImpl channelDAO;
    private MessageDAOImpl messageDAO;
    private ContactService contactService;

    // --- State Variables ---
    private User currentUser;
    private ChatController activeChatController;
    private boolean isDarkMode = false;
    private ScheduledExecutorService scheduler;
    private Image lightLogo;
    private Image darkLogo;


    @FXML
    public void initialize() {
        this.userDAO = new UserDAOImpl();
        this.groupDAO = new GroupDAOImpl();
        this.channelDAO = new ChannelDAOImpl();
        this.messageDAO = new MessageDAOImpl();

        try {
            this.contactService = new ContactService();
        } catch (SQLException e) {
            e.printStackTrace();
            FX.showError("Failed to initialize contact service.");
        }

        lightLogo = new Image(getClass().getResourceAsStream("/assets/telegram_logo.png"));
        darkLogo = new Image(getClass().getResourceAsStream("/assets/telegram_logo_dark.png"));

        setupSelectionListeners();
        setupGlobalSearch();
        profileContainer.setOnMouseClicked(event -> onEditProfile());
        themeToggleContainer.setOnMouseClicked(event -> toggleTheme());
        sunIcon.setOpacity(0);
        moonIcon.setOpacity(1);
    }

    private void setupGlobalSearch() {
        searchResultsListView.setVisible(false);
        searchResultsContainer.setVisible(false);

        globalSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                searchResultsListView.setVisible(false);
                searchResultsContainer.setVisible(false);
            } else {
                performSearch(newVal.trim());
            }
        });

        searchResultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item instanceof User user) {
                        setText(user.getDisplayName() + " (@" + user.getUsername() + ")");
                    } else if (item instanceof Group group) {
                        setText(group.getName() + " (Group)");
                    } else if (item instanceof Channel channel) {
                        setText(channel.getName() + " (Channel)");
                    }
                }
            }
        });

        searchResultsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Object selected = searchResultsListView.getSelectionModel().getSelectedItem();
                if (selected instanceof User user) {
                    openUserChat(user);
                } else if (selected instanceof Group group) {
                    openChatView("GROUP", group.getId());
                } else if (selected instanceof Channel channel) {
                    openChatView("CHANNEL", channel.getId());
                }
                globalSearchField.clear();
                searchResultsContainer.setVisible(false);
            }
        });
    }

    private void performSearch(String query) {
        FX.runAsync(() -> {
            try {
                List<User> users = userDAO.searchUsersForContacts(query, currentUser.getId());
                List<Group> groups = groupDAO.searchGroups(query);
                List<Channel> channels = channelDAO.searchChannels(query);
                List<Object> results = new java.util.ArrayList<>();
                results.addAll(users);
                results.addAll(groups);
                results.addAll(channels);
                return results;
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }, results -> {
            searchResultsListView.getItems().setAll(results);
            searchResultsListView.setVisible(!results.isEmpty());
            searchResultsContainer.setVisible(!results.isEmpty());
        }, null);
    }

    public void setCurrentUser(User u) {
        this.currentUser = u;
        if (this.currentUser != null) {
            refreshProfileView();
            loadAllChatLists();
            startPollingForChatListUpdates();
        }
    }

    private void startPollingForChatListUpdates() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::loadAllChatLists, 5, 5, TimeUnit.SECONDS);
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        FadeTransition sunFade = new FadeTransition(Duration.millis(300), sunIcon);
        FadeTransition moonFade = new FadeTransition(Duration.millis(300), moonIcon);

        if (isDarkMode) {
            mainContainer.getStyleClass().add("theme-dark");
            logoImageView.setImage(darkLogo);
            sunFade.setToValue(1);
            moonFade.setToValue(0);
        } else {
            mainContainer.getStyleClass().remove("theme-dark");
            logoImageView.setImage(lightLogo);
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
        if (imageView.getClip() == null || !(imageView.getClip() instanceof Circle)) {
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
                content.setPadding(new Insets(5));
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

    public void loadAllChatLists() {
        if (currentUser == null) return;
        FX.runAsync(() -> {
            try {
                List<User> contacts = contactService.getContacts(currentUser.getId());
                List<ChatListItem> userItems = contacts.stream()
                        .map(user -> {
                            try {
                                Optional<Message> lastMessageOpt = messageDAO.findLastMessageForChat("USER", user.getId(), currentUser.getId());
                                int unreadCount = messageDAO.getUnreadMessageCount("USER", user.getId(), currentUser.getId());
                                if (lastMessageOpt.isPresent()) {
                                    String msgText = lastMessageOpt.get().getContent();
                                    LocalDateTime ts = lastMessageOpt.get().getTimestamp();
                                    return new ChatListItem(user, msgText, unreadCount, ts);
                                } else {
                                    return null;
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(Objects::nonNull)
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

                List<Channel> channels = channelDAO.findSubscribedChannels(currentUser.getId());
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
                Platform.runLater(() -> {
                    usersListView.getItems().setAll((List<ChatListItem>) lists.get(0));
                    groupsListView.getItems().setAll((List<ChatListItem>) lists.get(1));
                    channelsListView.getItems().setAll((List<ChatListItem>) lists.get(2));
                });
            }
        }, Throwable::printStackTrace);
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
            activeChatController.setMainController(this);
            activeChatController.loadChatData(currentUser, type, id);
            chatArea.getChildren().setAll(chatNode);
            chatAreaPlaceholder.setVisible(false);
        } catch (IOException ex) {
            ex.printStackTrace();
            chatAreaPlaceholder.setText("Error loading chat.");
            chatAreaPlaceholder.setVisible(true);
        }
    }

    public void openUserChat(User user) {
        openChatView("USER", user.getId());
    }

    @FXML
    private void onNewGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Group");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(scene);
            GroupCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();
            loadAllChatLists();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNewChat() {
        onManageContacts();
    }


    @FXML
    private void onNewChannel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/channel_create.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Channel");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(scene);
            ChannelCreateController ctrl = loader.getController();
            ctrl.init(currentUser);
            dialog.showAndWait();
            loadAllChatLists();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onManageContacts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manage_contacts.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(mainContainer.getScene().getWindow());
            dialog.setTitle("Manage Contacts");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(scene);
            ManageContactsController ctrl = loader.getController();
            ctrl.initData(currentUser, this);
            dialog.showAndWait();

            loadAllChatLists();
        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Failed to open contacts manager.");
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
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(scene);
            ProfileController ctrl = loader.getController();
            ctrl.initData(currentUser, currentUser);
            dialog.showAndWait();
            refreshProfileView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClose() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}

