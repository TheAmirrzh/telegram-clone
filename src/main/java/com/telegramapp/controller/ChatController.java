package com.telegramapp.controller;

import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class ChatController {
    @FXML private ListView<Message> messagesList;
    @FXML private TextField messageField;
    @FXML private Button attachButton;
    @FXML private Button sendButton;

    private final MessageDAOImpl messageDAO;
    private final UserDAOImpl userDAO;

    private String currentUserId; // stored as String
    private String receiverId;    // stored as String
    private String receiverType;  // "USER" | "GROUP" | "CHANNEL"

    private volatile LocalDateTime lastLoaded = LocalDateTime.now().minusYears(1);
    private ScheduledExecutorService scheduler;
    private File selectedAttachment;

    public ChatController() {
        this.messageDAO = new MessageDAOImpl();
        this.userDAO = new UserDAOImpl();
    }

    /**
     * Initialize chat controller. Accepts UUIDs (your callers may pass UUID objects).
     */
    public void init(UUID currentUserUuid, String receiverType, UUID otherUuid) {
        this.currentUserId = currentUserUuid == null ? null : currentUserUuid.toString();
        this.receiverType = receiverType;
        this.receiverId = otherUuid == null ? null : otherUuid.toString();

        // cell factory: simply show message content + media indicator
        messagesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    String who = m.getSenderId().equals(currentUserId) ? "You" : resolveName(m.getSenderId());
                    String time = m.getTimestamp() == null ? "" : m.getTimestamp().toString();
                    String text = "[" + time + "] " + who + ": " + (m.getContent() == null ? "" : m.getContent());
                    if (m.getMediaPath() != null && !m.getMediaPath().isEmpty()) text += " (attachment)";
                    setText(text);
                    setGraphic(box);
                }
            }
        });

        // load current conversation
        reloadConversation();

        // schedule polling for new messages
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Message> newMsgs = messageDAO.findNewMessagesAfter(receiverType, receiverId, currentUserId, lastLoaded);
                if (!newMsgs.isEmpty()) {
                    Platform.runLater(() -> {
                        messagesList.getItems().addAll(newMsgs);
                        lastLoaded = newMsgs.get(newMsgs.size() - 1).getTimestamp();
                        messagesList.scrollTo(messagesList.getItems().size() - 1);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 1500, 1500, TimeUnit.MILLISECONDS);
    }

    private void reloadConversation() {
        try {
            List<Message> all = messageDAO.findConversation(receiverType, receiverId, currentUserId);
            messagesList.getItems().clear();
            messagesList.getItems().addAll(all);
            if (!all.isEmpty()) lastLoaded = all.get(all.size()-1).getTimestamp();
            messagesList.scrollTo(messagesList.getItems().size() - 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String resolveName(String userId) {
        if (userId == null) return "Unknown";
        if (userId.equals(currentUserId)) return "You";
        try {
            return userDAO.findById(userId).map(u -> {
                String d = u.getDisplayName();
                return (d == null || d.isBlank()) ? u.getUsername() : d;
            }).orElse(userId);
        } catch (SQLException ex) {
            return userId;
        }
    }

    @FXML
    private void onAttach() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select file to attach");
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

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if ((text.isEmpty() || text == null) && selectedAttachment == null) return;

        try {
            if (selectedAttachment != null) {
                Path storageDir = Paths.get("storage");
                if (!Files.exists(storageDir)) Files.createDirectories(storageDir);
                String orig = selectedAttachment.getName();
                String ext = "";
                int i = orig.lastIndexOf('.');
                if (i >= 0) ext = orig.substring(i);
                String storedName = UUID.randomUUID().toString() + ext;
                Path dest = storageDir.resolve(storedName);
                Files.copy(selectedAttachment.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                String mediaType = Files.probeContentType(dest);
                if (mediaType == null) mediaType = "file";
                String mediaPath = dest.toString();

                Message m = new Message(currentUserId, receiverId, receiverType, text, mediaType, mediaPath);
                messageDAO.save(m);
                messagesList.getItems().add(m);
                lastLoaded = m.getTimestamp();
                selectedAttachment = null;
                messageField.clear();
            } else {
                Message m = new Message(currentUserId, receiverId, receiverType, text);
                messageDAO.save(m);
                messagesList.getItems().add(m);
                lastLoaded = m.getTimestamp();
                messageField.clear();
            }
            messagesList.scrollTo(messagesList.getItems().size() - 1);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Send failed: " + e.getMessage(), ButtonType.OK);
            a.showAndWait();
        }
    }

    public void onClose() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}
