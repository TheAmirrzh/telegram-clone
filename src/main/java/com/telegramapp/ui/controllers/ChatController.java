package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.service.MessageService;
import com.telegramapp.util.FX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatController {

    @FXML private ListView<Message> messagesList;
    @FXML private TextField messageField;
    @FXML private HBox messageInputContainer;
    @FXML private Button joinChannelButton;
    @FXML private StackPane bottomContainer;

    private MessageService messageService;
    private User currentUser;
    private String otherId;
    private String receiverType;

    private volatile LocalDateTime lastLoaded = LocalDateTime.now().minusYears(1);
    private ScheduledExecutorService scheduler;
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    private record ChannelPermissions(boolean isOwner, boolean isSubscribed) {}

    @FXML
    public void initialize() {
        // This method is called by JavaFX AFTER all @FXML fields are injected.
        setupMessageListCellFactory();
        // Defensive null check to prevent any future crashes, although the root cause should now be fixed.
        if (messageInputContainer != null) {
            messageInputContainer.managedProperty().bind(messageInputContainer.visibleProperty());
        }
        if (joinChannelButton != null) {
            joinChannelButton.managedProperty().bind(joinChannelButton.visibleProperty());
        }
    }

    public void loadChatData(User currentUser, String receiverType, String otherId) {
        this.currentUser = currentUser;
        this.receiverType = receiverType;
        this.otherId = otherId;

        try {
            this.messageService = new MessageService();
        } catch (SQLException e) {
            e.printStackTrace();
            FX.showError("Failed to initialize message service.");
            return;
        }

        configureInputMode();
        loadInitialMessages();
    }

    private void configureInputMode() {
        // Ensure components are not null before using them.
        if (messageInputContainer == null || joinChannelButton == null) {
            System.err.println("FATAL: ChatController UI components are not injected. Check chat.fxml.");
            return;
        }

        messageInputContainer.setVisible(true);
        joinChannelButton.setVisible(false);

        if ("CHANNEL".equalsIgnoreCase(receiverType)) {
            messageInputContainer.setVisible(false);
            joinChannelButton.setVisible(false);

            FX.runAsync(() -> {
                try {
                    ChannelDAOImpl channelDAO = new ChannelDAOImpl();
                    String ownerId = channelDAO.findById(otherId).getOwnerId();
                    boolean isOwner = ownerId.equals(currentUser.getId());
                    boolean isSubscribed = isOwner || channelDAO.isSubscriber(otherId, currentUser.getId());
                    return new ChannelPermissions(isOwner, isSubscribed);
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ChannelPermissions(false, false);
                }
            }, permissions -> {
                if (permissions.isOwner()) {
                    messageInputContainer.setVisible(true);
                } else if (!permissions.isSubscribed()) {
                    joinChannelButton.setVisible(true);
                }
            }, null);
        }
    }

    @FXML
    private void onJoinChannel() {
        FX.runAsync(() -> {
            try {
                new ChannelDAOImpl().addSubscriber(otherId, currentUser.getId());
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, success -> {
            if (success) {
                configureInputMode();
            } else {
                FX.showError("Failed to join the channel.");
            }
        }, null);
    }

    private void setupMessageListCellFactory() {
        messagesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message m, boolean empty) {
                super.updateItem(m, empty);
                setText(null);
                if (empty || m == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createMessageBubble(m));
                }
            }
        });
    }

    private void loadInitialMessages() {
        FX.runAsync(
                () -> {
                    try {
                        return messageService.loadConversation(receiverType, otherId, currentUser.getId());
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return Collections.<Message>emptyList();
                    }
                },
                all -> {
                    populateList(all);
                    if (!all.isEmpty()) {
                        lastLoaded = all.get(all.size() - 1).getTimestamp();
                    }
                    startPollingForNewMessages();
                },
                ex -> FX.showError("Failed to load message history.")
        );
    }

    private void startPollingForNewMessages() {
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Message> newMsgs = messageService.loadNewSince(receiverType, otherId, currentUser.getId(), lastLoaded);
                if (!newMsgs.isEmpty()) {
                    lastLoaded = newMsgs.get(newMsgs.size() - 1).getTimestamp();
                    Platform.runLater(() -> appendList(newMsgs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    private HBox createMessageBubble(Message m) {
        boolean mine = m.getSenderId().equals(currentUser.getId());
        HBox row = new HBox(8);
        row.setPadding(new Insets(6, 12, 6, 12));
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setClip(new Circle(18, 18, 18));

        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.getStyleClass().add("message-bubble");
        bubble.getStyleClass().add(mine ? "mine" : "other");

        Text content = new Text(m.getContent() == null ? "" : m.getContent());
        content.getStyleClass().add("message-text");
        content.wrappingWidthProperty().bind(messagesList.widthProperty().subtract(180));

        User sender = resolveUser(m.getSenderId());
        String senderName = mine ? "You" : (sender != null ? sender.getDisplayName() : "Unknown");
        Label meta = new Label(senderName + " • " + m.getTimestamp().toString());
        meta.getStyleClass().add("message-meta");

        bubble.getChildren().addAll(content, meta);
        loadAvatar(sender, avatar);

        if (!mine) {
            row.getChildren().add(avatar);
        }
        row.getChildren().add(bubble);

        return row;
    }

    private User resolveUser(String userId) {
        return userCache.computeIfAbsent(userId, id -> {
            try {
                return new UserDAOImpl().findById(id).orElse(null);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private void loadAvatar(User user, ImageView imageView) {
        if (user == null || imageView == null) return;
        String picPath = user.getProfilePicPath();
        Image avatarImage = null;
        if (picPath != null && !picPath.isBlank()) {
            try (FileInputStream fis = new FileInputStream(new File(picPath))) {
                avatarImage = new Image(fis);
            } catch (Exception e) { /* Fallback */ }
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

    private void populateList(List<Message> list) {
        messagesList.getItems().setAll(list);
        if(!list.isEmpty()) messagesList.scrollTo(list.size() - 1);
    }

    private void appendList(List<Message> list) {
        messagesList.getItems().addAll(list);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        FX.runAsync(() -> {
            try {
                Message m = new Message(currentUser.getId(), otherId, receiverType, text);
                messageService.sendMessage(m);
                return m;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to send message", e);
            }
        }, (sentMessage) -> {
            messagesList.getItems().add(sentMessage);
            messagesList.scrollTo(messagesList.getItems().size() - 1);
            lastLoaded = sentMessage.getTimestamp();
            messageField.clear();
        }, (error) -> {
            error.printStackTrace();
            FX.showError("Send failed: " + error.getMessage());
        });
    }

    @FXML
    private void onAttach() {
        // Implementation for attaching files would go here.
    }

    public void onClose() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}

