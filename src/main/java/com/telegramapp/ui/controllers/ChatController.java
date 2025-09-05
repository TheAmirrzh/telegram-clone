package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.Group;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.service.MessageService;
import com.telegramapp.util.FX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

    private MessageService messageService;
    private User currentUser;
    private String receiverId;
    private String receiverType;
    private Object chatEntity;

    private volatile LocalDateTime lastLoaded = LocalDateTime.now().minusYears(1);
    private ScheduledExecutorService scheduler;
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    @FXML
    public void initialize() {
        setupMessageListCellFactory();
        try {
            this.messageService = new MessageService();
        } catch (SQLException e) {
            e.printStackTrace();
            FX.showError("Failed to initialize message service.");
        }
    }

    public void loadChatData(User currentUser, String receiverType, String receiverId) {
        this.currentUser = currentUser;
        this.receiverType = receiverType;
        this.receiverId = receiverId;

        configureHeader();
        configureInputMode();
        loadInitialMessages();
    }


    private void configureHeader() {
        chatHeader.setOnMouseClicked(event -> onHeaderClick());
        FX.runAsync(() -> {
            try {
                if ("USER".equalsIgnoreCase(receiverType)) {
                    return new UserDAOImpl().findById(receiverId).orElse(null);
                } else if ("GROUP".equalsIgnoreCase(receiverType)) {
                    return new GroupDAOImpl().findById(receiverId);
                } else if ("CHANNEL".equalsIgnoreCase(receiverType)) {
                    return new ChannelDAOImpl().findById(receiverId);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }, (Object entity) -> {
            this.chatEntity = entity;
            if (entity instanceof User) {
                User u = (User) entity;
                chatNameLabel.setText(u.getDisplayName());
                chatStatusLabel.setText("@" + u.getUsername());
                loadAvatar(u, chatAvatarImageView);
            } else if (entity instanceof Group) {
                Group g = (Group) entity;
                chatNameLabel.setText(g.getName());
                chatStatusLabel.setText("Group");
            } else if (entity instanceof Channel) {
                Channel c = (Channel) entity;
                chatNameLabel.setText(c.getName());
                chatStatusLabel.setText("Channel");
            }
        }, null);
    }

    private void onHeaderClick() {
        if (chatEntity == null) return;

        try {
            if (chatEntity instanceof User) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(chatHeader.getScene().getWindow());
                dialog.setTitle("User Profile");
                dialog.setScene(new Scene(loader.load()));
                ProfileController ctrl = loader.getController();
                ctrl.initData((User) chatEntity, currentUser); // Pass both users to check for edit permissions
                dialog.showAndWait();
            } else if (chatEntity instanceof Group || chatEntity instanceof Channel) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/members_view.fxml"));
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(chatHeader.getScene().getWindow());
                dialog.setTitle("Members");
                dialog.setScene(new Scene(loader.load()));
                MembersViewController ctrl = loader.getController();
                ctrl.loadMembers(currentUser, chatEntity);
                dialog.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Failed to open details view.");
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
                    ChannelDAOImpl channelDAO = new ChannelDAOImpl();
                    String ownerId = channelDAO.findById(receiverId).getOwnerId();
                    boolean isOwner = ownerId.equals(currentUser.getId());
                    boolean isSubscribed = isOwner || channelDAO.isSubscriber(receiverId, currentUser.getId());
                    return new Object[]{isOwner, isSubscribed};
                } catch (Exception e) {
                    e.printStackTrace();
                    return new Object[]{false, false};
                }
            }, (Object[] result) -> {
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
            try {
                new ChannelDAOImpl().addSubscriber(receiverId, currentUser.getId());
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, success -> {
            if (success) {
                joinChannelButton.setVisible(false);
                joinChannelButton.setManaged(false);
            } else {
                FX.showError("Failed to join the channel.");
            }
        }, null);
    }

    private void setupMessageListCellFactory() {
        messagesList.setCellFactory(lv -> {
            ListCell<Message> cell = new ListCell<>() {
                @Override
                protected void updateItem(Message m, boolean empty) {
                    super.updateItem(m, empty);
                    if (empty || m == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setGraphic(createMessageBubble(m));
                    }
                }
            };

            // Create ContextMenu for edit/delete
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Edit");
            MenuItem deleteItem = new MenuItem("Delete");

            editItem.setOnAction(event -> {
                Message messageToEdit = cell.getItem();
                if (messageToEdit == null) return;

                TextInputDialog dialog = new TextInputDialog(messageToEdit.getContent());
                dialog.setTitle("Edit Message");
                dialog.setHeaderText(null);
                dialog.setContentText("Enter new message text:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(newContent -> {
                    if (!newContent.trim().isEmpty() && !newContent.equals(messageToEdit.getContent())) {
                        messageToEdit.setContent(newContent);
                        FX.runAsync(() -> {
                            try {
                                messageService.editMessage(messageToEdit);
                            } catch (SQLException e) { throw new RuntimeException(e); }
                            return null;
                        }, success -> loadInitialMessages(), error -> FX.showError("Failed to edit message."));
                    }
                });
            });

            deleteItem.setOnAction(event -> {
                Message messageToDelete = cell.getItem();
                if (messageToDelete == null) return;
                FX.runAsync(() -> {
                    try {
                        messageService.deleteMessage(messageToDelete.getId(), currentUser.getId());
                    } catch (SQLException e) { throw new RuntimeException(e); }
                    return null;
                }, success -> loadInitialMessages(), error -> FX.showError("Failed to delete message."));
            });

            contextMenu.getItems().addAll(editItem, deleteItem);
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    if (cell.getItem() != null &&
                            cell.getItem().getSenderId().equals(currentUser.getId()) &&
                            !"DELETED".equals(cell.getItem().getReadStatus())) {
                        cell.setContextMenu(contextMenu);
                    } else {
                        cell.setContextMenu(null);
                    }
                }
            });

            return cell;
        });
    }

    private void loadInitialMessages() {
        FX.runAsync(() -> {
            try {
                return messageService.loadConversation(receiverType, receiverId, currentUser.getId());
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.<Message>emptyList();
            }
        }, (List<Message> all) -> {
            populateList(all);
            if (!all.isEmpty()) {
                lastLoaded = all.get(all.size() - 1).getTimestamp();
            }
            startPollingForNewMessages();
            markMessagesAsRead();
        }, null);
    }

    private void markMessagesAsRead() {
        FX.runAsync(() -> {
            try {
                messageService.markMessagesAsRead(receiverType, receiverId, currentUser.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }, v -> {}, null);
    }

    private void startPollingForNewMessages() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Message> newMsgs = messageService.loadNewSince(receiverType, receiverId, currentUser.getId(), lastLoaded);
                if (!newMsgs.isEmpty()) {
                    Platform.runLater(() -> appendList(newMsgs));
                    lastLoaded = newMsgs.get(newMsgs.size() - 1).getTimestamp();
                    markMessagesAsRead();
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
        if ("DELETED".equals(m.getReadStatus())) {
            content.getStyleClass().add("deleted-message-text");
        } else {
            content.getStyleClass().add("message-text");
        }
        content.wrappingWidthProperty().bind(messagesList.widthProperty().subtract(180));

        User sender = resolveUser(m.getSenderId());
        String senderName = mine ? "You" : (sender != null ? sender.getDisplayName() : "Unknown");
        Label meta = new Label(senderName + " • " + m.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
        meta.getStyleClass().add("message-meta");

        HBox metaContainer = new HBox(5);
        metaContainer.setAlignment(Pos.CENTER_RIGHT);
        metaContainer.getChildren().add(meta);

        if (mine && !"DELETED".equals(m.getReadStatus())) {
            metaContainer.getChildren().add(createTickStatus(m));
        }

        bubble.getChildren().addAll(content, metaContainer);
        loadAvatar(sender, avatar);

        if (!mine) {
            row.getChildren().add(avatar);
        }
        row.getChildren().add(bubble);

        return row;
    }

    private HBox createTickStatus(Message m) {
        HBox tickContainer = new HBox();
        tickContainer.setAlignment(Pos.CENTER_LEFT);
        tickContainer.setSpacing(-7); // Overlap ticks

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
        return userCache.computeIfAbsent(userId, id -> {
            try {
                return new UserDAOImpl().findById(id).orElse(null);
            } catch (Exception e) {
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
                Message m = new Message(currentUser.getId(), receiverId, receiverType, text);
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
