package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.Group;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.service.MessageService;
import com.telegramapp.service.TypingService;
import com.telegramapp.util.FX;
import com.telegramapp.util.ImageStorage;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Cursor;
import java.awt.Desktop;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ChatController {

    @FXML private ListView<Message> messagesList;
    @FXML private TextField messageField;
    @FXML private HBox messageInputContainer;
    @FXML private Button joinChannelButton;
    @FXML private StackPane bottomContainer;
    @FXML private HBox chatHeader;
    @FXML private ImageView chatAvatarImageView;
    @FXML private Label chatNameLabel;
    @FXML private Label chatStatusLabel;
    @FXML private Button manageMembersButton;
    @FXML private HBox replyPreviewBox;
    @FXML private Label replyPreviewHeader;
    @FXML private Label replyPreviewContent;

    private MessageService messageService;
    private TypingService typingService;
    private User currentUser;
    private String receiverId;
    private String receiverType;
    private Object chatEntity;
    private MainController mainController;
    private Message messageToReplyTo = null;

    private volatile LocalDateTime lastLoaded = LocalDateTime.now().minusYears(1);
    private ScheduledExecutorService scheduler;
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    private final Map<String, Message> messageCache = new ConcurrentHashMap<>();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        try {
            this.messageService = new MessageService();
            this.typingService = new TypingService();
        } catch (SQLException e) {
            e.printStackTrace();
            FX.showError("Failed to initialize services.");
        }
        setupMessageListCellFactory();
        setupTypingListener();
    }

    public void loadChatData(User currentUser, String receiverType, String receiverId) {
        this.currentUser = currentUser;
        this.receiverType = receiverType;
        this.receiverId = receiverId;

        configureHeader();
        configureInputMode();
        loadInitialMessages();
        startPolling();
    }

    private void setupTypingListener() {
        messageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (typingService != null && !"CHANNEL".equals(receiverType)) {
                typingService.updateTypingStatus(receiverId, currentUser.getId());
            }
        });
    }

    private void startPolling() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Message> newMsgs = messageService.loadNewSince(receiverType, receiverId, currentUser.getId(), lastLoaded);
                if (!newMsgs.isEmpty()) {
                    Platform.runLater(() -> {
                        appendMessages(newMsgs);
                        if (mainController != null) {
                            mainController.loadAllChatLists();
                        }
                    });
                    lastLoaded = newMsgs.get(newMsgs.size() - 1).getTimestamp();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 2, 2, TimeUnit.SECONDS);

        if (!"CHANNEL".equals(receiverType)) {
            scheduler.scheduleAtFixedRate(this::updateTypingStatus, 0, 3, TimeUnit.SECONDS);
        }
    }

    private void updateTypingStatus() {
        if (typingService == null) return;
        FX.runAsync(() -> {
            try {
                List<String> typingUserIds = typingService.getTypingUsers(receiverId, currentUser.getId(), 3);
                return userCache.values().stream()
                        .filter(u -> typingUserIds.contains(u.getId()))
                        .map(User::getDisplayName)
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.<String>emptyList();
            }
        }, (List<String> typingUsers) -> {
            if (chatEntity instanceof User) {
                chatStatusLabel.setText(((User) chatEntity).getStatus());
            } else {
                chatStatusLabel.setText(receiverType);
            }
            if (!typingUsers.isEmpty()) {
                String typingText = String.join(", ", typingUsers) + (typingUsers.size() > 1 ? " are" : " is") + " typing...";
                chatStatusLabel.setText(typingText);
            }
        }, null);
    }

    private void setupMessageListCellFactory() {
        messagesList.setCellFactory(lv -> {
            ListCell<Message> cell = new ListCell<>() {
                @Override
                protected void updateItem(Message m, boolean empty) {
                    super.updateItem(m, empty);
                    setGraphic(empty || m == null ? null : createMessageBubble(m));
                    setContextMenu(empty || m == null ? null : createContextMenuForCell(this));
                }
            };
            return cell;
        });
    }

    private ContextMenu createContextMenuForCell(ListCell<Message> cell) {
        ContextMenu contextMenu = new ContextMenu();
        Message message = cell.getItem();

        MenuItem replyItem = new MenuItem("Reply");
        replyItem.setOnAction(event -> {
            messageToReplyTo = message;
            replyPreviewHeader.setText("Replying to " + resolveUser(message.getSenderId()).getDisplayName());
            replyPreviewContent.setText(message.getContent());
            replyPreviewBox.setVisible(true);
            replyPreviewBox.setManaged(true);
        });
        contextMenu.getItems().add(replyItem);

        if (message != null && message.getSenderId().equals(currentUser.getId()) && !"DELETED".equals(message.getReadStatus())) {
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(event -> {
                TextInputDialog dialog = new TextInputDialog(message.getContent());
                dialog.setTitle("Edit Message");
                dialog.setHeaderText(null);
                dialog.setContentText("Enter new message text:");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(newContent -> {
                    if (!newContent.trim().isEmpty() && !newContent.equals(message.getContent())) {
                        message.setContent(newContent);
                        FX.runAsync(() -> {
                            try {
                                messageService.editMessage(message);
                            } catch (SQLException e) { throw new RuntimeException(e); }
                        }, this::loadInitialMessages, error -> FX.showError("Failed to edit message."));
                    }
                });
            });

            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(event -> FX.runAsync(() -> {
                try {
                    messageService.deleteMessage(message.getId(), currentUser.getId());
                } catch (SQLException e) { throw new RuntimeException(e); }
            }, this::loadInitialMessages, error -> FX.showError("Failed to delete message.")));

            contextMenu.getItems().addAll(editItem, deleteItem);
        }
        return contextMenu;
    }

    private Node createMessageBubble(Message m) {
        boolean mine = m.getSenderId().equals(currentUser.getId());
        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.getStyleClass().addAll("message-bubble", mine ? "mine" : "other");

        if (m.getReplyToMessageId() != null) {
            Message repliedMsg = getMessageById(m.getReplyToMessageId());
            if (repliedMsg != null) {
                VBox replySnippet = new VBox();
                replySnippet.setPadding(new Insets(4, 8, 4, 8));
                replySnippet.getStyleClass().add("reply-snippet");
                Label repliedUserLabel = new Label(resolveUser(repliedMsg.getSenderId()).getDisplayName());
                repliedUserLabel.getStyleClass().add("reply-header-label");
                Label repliedContentLabel = new Label(repliedMsg.getContent());
                repliedContentLabel.getStyleClass().add("reply-content-label");
                replySnippet.getChildren().addAll(repliedUserLabel, repliedContentLabel);
                bubble.getChildren().add(replySnippet);
            }
        }

        if ("IMAGE".equals(m.getMediaType()) && m.getMediaPath() != null) {
            ImageView imageView = new ImageView();
            imageView.setFitWidth(200);
            imageView.setPreserveRatio(true);
            try (FileInputStream fis = new FileInputStream(m.getMediaPath())) {
                imageView.setImage(new Image(fis));
            } catch (IOException e) { e.printStackTrace(); }

            // FIX: Add click handler to open the image
            imageView.setCursor(Cursor.HAND);
            imageView.setOnMouseClicked(event -> {
                try {
                    File file = new File(m.getMediaPath());
                    if (file.exists()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        FX.showError("Attachment not found at: " + m.getMediaPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    FX.showError("Could not open the attachment.");
                }
            });

            bubble.getChildren().add(imageView);
        }

        if (m.getContent() != null && !m.getContent().isBlank()) {
            Text content = new Text(m.getContent());
            content.getStyleClass().add("DELETED".equals(m.getReadStatus()) ? "deleted-message-text" : "message-text");
            content.wrappingWidthProperty().bind(messagesList.widthProperty().subtract(180));
            bubble.getChildren().add(content);
        }

        User sender = resolveUser(m.getSenderId());
        String senderName = mine ? "You" : (sender != null ? sender.getDisplayName() : "Unknown");
        Label meta = new Label(senderName + " • " + m.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
        meta.getStyleClass().add("message-meta");
        HBox metaContainer = new HBox(5, meta);
        metaContainer.setAlignment(Pos.CENTER_RIGHT);
        if (mine && !"DELETED".equals(m.getReadStatus())) {
            metaContainer.getChildren().add(createTickStatus(m));
        }
        bubble.getChildren().add(metaContainer);

        HBox row = new HBox(8);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 12, 4, 12));
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setClip(new Circle(18, 18, 18));
        loadAvatar(sender, avatar);
        if (!mine) row.getChildren().add(avatar);
        row.getChildren().add(bubble);

        return row;
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        FX.runAsync(() -> {
            try {
                Message m = new Message(currentUser.getId(), receiverId, receiverType, text);
                if (messageToReplyTo != null) {
                    m.setReplyToMessageId(messageToReplyTo.getId());
                }
                messageService.sendMessage(m);
                return m;
            } catch (SQLException e) { throw new RuntimeException(e); }
        }, sentMessage -> {
            messagesList.getItems().add(sentMessage);
            messagesList.scrollTo(messagesList.getItems().size() - 1);
            lastLoaded = sentMessage.getTimestamp();
            messageField.clear();
            onCancelReply();
        }, error -> FX.showError("Send failed: " + error.getMessage()));
    }

    @FXML
    private void onAttach() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(messageField.getScene().getWindow());

        if (selectedFile != null) {
            FX.runAsync(() -> {
                try {
                    String mediaPath = ImageStorage.saveAttachment(selectedFile);
                    Message m = new Message(currentUser.getId(), receiverId, receiverType, "", "IMAGE", mediaPath);
                    if (messageToReplyTo != null) {
                        m.setReplyToMessageId(messageToReplyTo.getId());
                    }
                    messageService.sendMessage(m);
                    return m;
                } catch (IOException | SQLException e) { throw new RuntimeException(e); }
            }, sentMessage -> {
                messagesList.getItems().add(sentMessage);
                messagesList.scrollTo(messagesList.getItems().size() - 1);
                lastLoaded = sentMessage.getTimestamp();
                onCancelReply();
            }, error -> FX.showError("Could not send image: " + error.getMessage()));
        }
    }

    @FXML
    private void onCancelReply() {
        messageToReplyTo = null;
        replyPreviewBox.setVisible(false);
        replyPreviewBox.setManaged(false);
    }

    private void configureHeader() {
        manageMembersButton.setVisible(false);
        FX.runAsync(() -> {
            try {
                if ("USER".equalsIgnoreCase(receiverType)) return new UserDAOImpl().findById(receiverId).orElse(null);
                if ("GROUP".equalsIgnoreCase(receiverType)) return new GroupDAOImpl().findById(receiverId);
                if ("CHANNEL".equalsIgnoreCase(receiverType)) return new ChannelDAOImpl().findById(receiverId);
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        }, entity -> {
            this.chatEntity = entity;
            if (entity instanceof User u) {
                chatNameLabel.setText(u.getDisplayName());
                chatStatusLabel.setText(u.getStatus());
                loadAvatar(u, chatAvatarImageView);
            } else if (entity instanceof Group g) {
                chatNameLabel.setText(g.getName());
                chatStatusLabel.setText("Group");
                manageMembersButton.setVisible(true);
            } else if (entity instanceof Channel c) {
                chatNameLabel.setText(c.getName());
                chatStatusLabel.setText("Channel");
                manageMembersButton.setVisible(true);
            }
        }, null);
    }

    @FXML
    private void onHeaderClick() {
        if (chatEntity instanceof User) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(chatHeader.getScene().getWindow());
                dialog.setTitle("User Profile");
                Scene scene = new Scene(loader.load());
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                dialog.setScene(scene);
                ProfileController ctrl = loader.getController();
                ctrl.initData((User) chatEntity, currentUser);
                dialog.showAndWait();
            } catch (IOException e) { e.printStackTrace(); FX.showError("Failed to open profile view."); }
        }
    }

    @FXML
    private void onManageMembers() {
        if (chatEntity instanceof Group || chatEntity instanceof Channel) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/members_view.fxml"));
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(chatHeader.getScene().getWindow());
                dialog.setTitle("Manage Members");
                Scene scene = new Scene(loader.load());
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                dialog.setScene(scene);
                MembersViewController ctrl = loader.getController();
                ctrl.loadMembers(currentUser, chatEntity);
                dialog.showAndWait();
            } catch (IOException e) { e.printStackTrace(); FX.showError("Failed to open members view."); }
        }
    }

    private void configureInputMode() {
        messageInputContainer.setVisible(true);
        joinChannelButton.setVisible(false);
        joinChannelButton.setManaged(false);

        if ("CHANNEL".equalsIgnoreCase(receiverType)) {
            messageInputContainer.setVisible(false);
            joinChannelButton.setVisible(false);
            FX.runAsync(() -> {
                try {
                    ChannelDAOImpl dao = new ChannelDAOImpl();
                    boolean isOwner = dao.findById(receiverId).getOwnerId().equals(currentUser.getId());
                    boolean isSubscribed = isOwner || dao.isSubscriber(receiverId, currentUser.getId());
                    return new Object[]{isOwner, isSubscribed};
                } catch (Exception e) { e.printStackTrace(); return new Object[]{false, false}; }
            }, result -> {
                boolean isOwner = (boolean) result[0];
                boolean isSubscribed = (boolean) result[1];
                if (isOwner) {
                    messageInputContainer.setVisible(true);
                } else if (!isSubscribed) {
                    joinChannelButton.setVisible(true);
                    joinChannelButton.setManaged(true);
                }
            }, null);
        }
    }

    @FXML
    private void onJoinChannel() {
        FX.runAsync(() -> {
            try { new ChannelDAOImpl().addSubscriber(receiverId, currentUser.getId(), "SUBSCRIBER"); return true;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, success -> {
            if (success) {
                joinChannelButton.setVisible(false);
                joinChannelButton.setManaged(false);
            } else { FX.showError("Failed to join the channel."); }
        }, null);
    }

    private void loadInitialMessages() {
        FX.runAsync(() -> {
            try { return messageService.loadConversation(receiverType, receiverId, currentUser.getId());
            } catch (SQLException e) { e.printStackTrace(); return Collections.<Message>emptyList(); }
        }, all -> {
            populateMessages(all);
            if (!all.isEmpty()) {
                lastLoaded = all.get(all.size() - 1).getTimestamp();
            }
            markMessagesAsRead();
        }, null);
    }

    private void markMessagesAsRead() {
        FX.runAsync(() -> {
            try { messageService.markMessagesAsRead(receiverType, receiverId, currentUser.getId());
            } catch (SQLException e) { e.printStackTrace(); }
        }, () -> {
            if (mainController != null) mainController.loadAllChatLists();
        }, Throwable::printStackTrace);
    }

    private HBox createTickStatus(Message m) {
        HBox tickContainer = new HBox();
        tickContainer.setAlignment(Pos.CENTER_LEFT);
        tickContainer.setSpacing(-7);
        SVGPath singleTick = new SVGPath();
        singleTick.setContent("M4 12.5l2.5 2.5 6-6");
        singleTick.setStrokeWidth(1.5);
        singleTick.setFill(Color.TRANSPARENT);
        if ("READ".equals(m.getReadStatus())) {
            singleTick.setStroke(Color.DODGERBLUE);
            SVGPath doubleTick = new SVGPath();
            doubleTick.setContent("M8 12.5l2.5 2.5 6-6");
            doubleTick.setStroke(Color.DODGERBLUE);
            doubleTick.setStrokeWidth(1.5);
            doubleTick.setFill(Color.TRANSPARENT);
            tickContainer.getChildren().addAll(singleTick, doubleTick);
        } else {
            singleTick.setStroke(Color.GRAY);
            tickContainer.getChildren().add(singleTick);
        }
        if ("EDITED".equals(m.getReadStatus())) {
            Label editedLabel = new Label("(edited)");
            editedLabel.getStyleClass().add("message-meta");
            tickContainer.getChildren().add(editedLabel);
        }
        return tickContainer;
    }

    private User resolveUser(String userId) {
        if (userId == null) return null;
        return userCache.computeIfAbsent(userId, id -> {
            try { return new UserDAOImpl().findById(id).orElse(null);
            } catch (Exception e) { return null; }
        });
    }

    private Message getMessageById(String messageId) {
        if (messageId == null) return null;
        return messageCache.computeIfAbsent(messageId, id -> {
            try { return messageService.getMessageById(id).orElse(null);
            } catch (SQLException e) { e.printStackTrace(); return null; }
        });
    }

    private void loadAvatar(User user, ImageView imageView) {
        if (user == null || imageView == null) return;
        String picPath = user.getProfilePicPath();
        Image avatarImage = null;
        if (picPath != null && !picPath.isBlank()) {
            try (FileInputStream fis = new FileInputStream(new File(picPath))) {
                avatarImage = new Image(fis);
            } catch (Exception e) { /* fallback */ }
        }
        if (avatarImage == null) {
            try (InputStream defaultAvatarStream = getClass().getResourceAsStream("/assets/default_avatar.png")) {
                if (defaultAvatarStream != null) {
                    avatarImage = new Image(defaultAvatarStream);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        imageView.setImage(avatarImage);
    }

    private void populateMessages(List<Message> list) {
        list.forEach(m -> messageCache.put(m.getId(), m));
        messagesList.getItems().setAll(list);
        if (!list.isEmpty()) messagesList.scrollTo(list.size() - 1);
    }

    private void appendMessages(List<Message> list) {
        list.forEach(m -> messageCache.put(m.getId(), m));
        messagesList.getItems().addAll(list);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    public void onClose() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}

