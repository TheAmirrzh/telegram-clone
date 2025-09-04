package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.service.MessageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatController {
    @FXML private ListView<Message> messagesList;
    @FXML private TextField messageField;

    private MessageService messageService;
    private User currentUser;
    private String otherId;
    private String receiverType;

    private volatile LocalDateTime lastLoaded = LocalDateTime.now().minusYears(1);
    private ScheduledExecutorService scheduler;

    private File selectedAttachment;
    private final Map<String, String> userCache = new ConcurrentHashMap<>();
    private final Map<String, Image> avatarCache = new ConcurrentHashMap<>();

    public ChatController() {
        try {
            this.messageService = new MessageService();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void init(User currentUser, String receiverType, String otherId) {
        this.currentUser = currentUser;
        this.receiverType = receiverType;
        this.otherId = otherId;

        messagesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean mine = m.getSenderId().equals(currentUser.getId());
                    HBox row = new HBox();
                    row.setPadding(new Insets(6));
                    row.setSpacing(8);
                    if (mine) row.setAlignment(Pos.CENTER_RIGHT);
                    else row.setAlignment(Pos.CENTER_LEFT);

                    // avatar (for non-mine)
                    ImageView avatar = null;
                    if (!mine) {
                        avatar = new ImageView();
                        avatar.setFitWidth(36);
                        avatar.setFitHeight(36);
                        String p = resolveAvatarPath(m.getSenderId());
                        if (p != null) {
                            try (FileInputStream fis = new FileInputStream(p)) {
                                Image img = new Image(fis, 36, 36, true, true);
                                avatar.setImage(img);
                                avatarCache.put(m.getSenderId(), img);
                            } catch (Exception ex) {
                                // leave avatar null (we'll show initials instead)
                                avatar = null;
                            }
                        }
                    } else {
                        // optionally show your avatar on right; omitted for compactness
                    }

                    // bubble
                    VBox bubble = new VBox(4);
                    bubble.setPadding(new Insets(8));
                    bubble.setMaxWidth(480);
                    bubble.getStyleClass().add("message-bubble");
                    if (mine) bubble.getStyleClass().add("mine");
                    else bubble.getStyleClass().add("other");
                    Text content = new Text(m.getContent() == null ? "" : m.getContent());
                    content.wrappingWidthProperty().bind(messagesList.widthProperty().subtract(180));
                    Label meta = new Label((mine ? "You" : resolveName(m.getSenderId())) + " • " + m.getTimestamp().toString());
                    meta.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

                    bubble.getChildren().addAll(content, meta);

                    if (m.getMediaPath() != null && !m.getMediaPath().isEmpty()) {
                        HBox attachRow = new HBox(6);
                        Button openBtn = new Button("Open");
                        Label fileLabel = new Label(Paths.get(m.getMediaPath()).getFileName().toString());
                        openBtn.setOnAction(ev -> {
                            try {
                                Path p = Paths.get(m.getMediaPath());
                                File f = p.toFile();
                                if (!f.exists()) {
                                    f = Paths.get(System.getProperty("user.dir")).resolve(m.getMediaPath()).toFile();
                                }
                                if (!f.exists()) {
                                    Alert a = new Alert(Alert.AlertType.WARNING, "File not found: " + m.getMediaPath(), ButtonType.OK);
                                    a.showAndWait();
                                    return;
                                }
                                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
                                else {
                                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Open not supported. Path:\n" + f.getAbsolutePath(), ButtonType.OK);
                                    a.showAndWait();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });
                        attachRow.getChildren().addAll(openBtn, fileLabel);
                        bubble.getChildren().add(attachRow);
                    }

                    if (mine) {
                        row.getChildren().addAll(bubble);
                    } else {
                        if (avatar != null) row.getChildren().addAll(avatar, bubble);
                        else {
                            // show initials as circle
                            String name = resolveName(m.getSenderId());
                            Label initials = new Label(initialsOf(name));
                            initials.getStyleClass().add("avatar-initials");                            initials.setStyle("-fx-background-color: #ddd; -fx-padding: 6px; -fx-background-radius: 18; -fx-alignment:center;");
                            row.getChildren().addAll(initials, bubble);
                        }
                    }

                    setGraphic(row);
                }
            }
        });

        // initial load
        try {
            List<Message> all = messageService.loadConversation(receiverType, otherId, currentUser.getId());
            populateList(all);
            if (!all.isEmpty()) lastLoaded = all.get(all.size()-1).getTimestamp();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Message> newMsgs = messageService.loadNewSince(receiverType, otherId, currentUser.getId(), lastLoaded);
                if (!newMsgs.isEmpty()) {
                    Platform.runLater(() -> {
                        appendList(newMsgs);
                        lastLoaded = newMsgs.get(newMsgs.size()-1).getTimestamp();
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 1500, 1500, TimeUnit.MILLISECONDS);
    }

    private void populateList(List<Message> list) {
        messagesList.getItems().clear();
        messagesList.getItems().addAll(list);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    private void appendList(List<Message> list) {
        messagesList.getItems().addAll(list);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    private String resolveName(String userId) {
        if (userId.equals(currentUser.getId())) return "You";
        return userCache.computeIfAbsent(userId, id -> {
            try {
                return new UserDAOImpl().findById(id).map(u -> (u.getDisplayName() == null || u.getDisplayName().isBlank()) ? u.getUsername() : u.getDisplayName()).orElse(id);
            } catch (Exception e) {
                return id;
            }
        });
    }

    private String resolveAvatarPath(String userId) {
        try {
            return new UserDAOImpl().findById(userId).map(User::getProfilePicPath).orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }

    private String initialsOf(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty() && selectedAttachment == null) return;

        try {
            if (selectedAttachment != null) {
                Path storageDir = Paths.get("storage");
                if (!Files.exists(storageDir)) Files.createDirectories(storageDir);
                String ext = "";
                String original = selectedAttachment.getName();
                int idx = original.lastIndexOf('.');
                if (idx >= 0) ext = original.substring(idx);
                String storedName = UUID.randomUUID().toString() + ext;
                Path dest = storageDir.resolve(storedName);
                Files.copy(selectedAttachment.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                String mediaType = Files.probeContentType(dest);
                if (mediaType == null) mediaType = "file";
                String relativePath = storageDir.resolve(storedName).toString();
                Message m = new Message(currentUser.getId(), otherId, receiverType, text, mediaType, relativePath);
                messageService.sendMessage(m);
                messagesList.getItems().add(m);
                lastLoaded = m.getTimestamp();
                selectedAttachment = null;
                messageField.clear();
            } else {
                Message m = new Message(currentUser.getId(), otherId, receiverType, text);
                messageService.sendMessage(m);
                messagesList.getItems().add(m);
                lastLoaded = m.getTimestamp();
                messageField.clear();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Send failed: " + e.getMessage(), ButtonType.OK);
                a.showAndWait();
            });
        }
    }

    @FXML
    private void onAttach() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select File to Attach");
        File file = fc.showOpenDialog(messageField.getScene().getWindow());
        if (file != null) {
            long maxBytes = 20L * 1024L * 1024L;
            if (file.length() > maxBytes) {
                Alert a = new Alert(Alert.AlertType.WARNING, "File too large (limit 20 MB).", ButtonType.OK);
                a.showAndWait();
                return;
            }
            selectedAttachment = file;
            messageField.setText("[Attached: " + file.getName() + "] " + messageField.getText());
        }
    }

    public void onClose() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    public void closeWindow() {
        Stage stage = (Stage) messageField.getScene().getWindow();
        stage.close();
    }
}
