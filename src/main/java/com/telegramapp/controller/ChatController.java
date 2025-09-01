package com.telegramapp.controller;

import com.telegramapp.dao.ChatDAO; // <-- IMPORT ADDED
import com.telegramapp.dao.MessageDAO;
import com.telegramapp.dao.TypingDAO;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.ui.MessageCell;
import com.telegramapp.util.FX;
import com.telegramapp.util.ImageStorage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class ChatController {
    @FXML private Label chatTitle;
    @FXML private Label typingLabel;
    @FXML private ListView<Message> messageList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button attachButton;

    private User currentUser;
    private UUID privateChatId;
    private UUID groupId;
    private UUID channelId;
    private final MessageDAO messageDAO = new MessageDAO();
    private final TypingDAO typingDAO = new TypingDAO();
    private final ChatDAO chatDAO = new ChatDAO(); // This line now works
    private File pendingAttachment = null;
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    private ScheduledFuture<?> typingPollTask;

    public void setCurrentUser(User u){ this.currentUser = u; ensureCellFactory(); }

    public void setPrivateChatTarget(User targetUser) {
        this.chatTitle.setText(targetUser.getProfileName());
        // Find or create the private chat and then load its history
        FX.runAsync(() -> chatDAO.findOrCreatePrivateChat(currentUser.getId(), targetUser.getId()),
                chatId -> {
                    this.privateChatId = (UUID) chatId; // <-- EXPLICIT CAST ADDED
                    loadHistory();
                    startTypingPoll(); // Start polling only after we have a chat ID
                },
                Throwable::printStackTrace);
    }

    public void setGroupId(UUID id){ this.groupId = id; chatTitle.setText("Group: " + id); loadHistory(); startTypingPoll(); }
    public void setChannelId(UUID id){ this.channelId = id; chatTitle.setText("Channel: " + id); loadHistory(); /* No typing in channels */ }

    private void ensureCellFactory(){
        if (messageList.getCellFactory()==null && currentUser!=null) {
            messageList.setCellFactory(lv -> new MessageCell(currentUser));
            messageList.setOnMouseClicked(ev -> {
                if (ev.isSecondaryButtonDown()) {
                    Message m = messageList.getSelectionModel().getSelectedItem();
                    if (m==null) return;
                    if (m.getSenderId()!=null && m.getSenderId().equals(currentUser.getId())) {
                        ContextMenu cm = new ContextMenu();
                        MenuItem edit = new MenuItem("Edit");
                        MenuItem del = new MenuItem("Delete");
                        edit.setOnAction(ae -> promptEdit(m));
                        del.setOnAction(ad -> promptDelete(m));
                        cm.getItems().addAll(edit, del);
                        cm.show(messageList, ev.getScreenX(), ev.getScreenY());
                    }
                }
            });
        }
    }

    private void promptEdit(Message m){
        TextInputDialog d = new TextInputDialog(m.getContent());
        d.setTitle("Edit message");
        d.setHeaderText("Edit your message");
        d.showAndWait().ifPresent(newText -> {
            FX.runAsync(() -> messageDAO.editMessage(m.getId(), newText), ok -> loadHistory(), Throwable::printStackTrace);
        });
    }

    private void promptDelete(Message m){
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete message?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(bt -> {
            if (bt==ButtonType.YES) {
                FX.runAsync(() -> messageDAO.deleteMessage(m.getId()), ok -> loadHistory(), Throwable::printStackTrace);
            }
        });
    }

    private void loadHistory(){
        ensureCellFactory();
        FX.runAsync(() -> {
            if (privateChatId != null) {
                return messageDAO.loadPrivateChatHistory(privateChatId, 500);
            } else if (groupId != null) {
                return messageDAO.loadGroupHistory(groupId, 500);
            } else if (channelId != null) {
                return messageDAO.loadChannelHistory(channelId, 500);
            }
            // Return an empty list that is explicitly typed as holding Messages
            return new ArrayList<Message>();
        }, // Inside the 'loadHistory' method, in the success part of the lambda
                (List<Message> list) -> {
                    // Store the old size
                    int oldSize = messageList.getItems().size();

                    messageList.getItems().setAll(list);

                    if (!list.isEmpty()) {
                        messageList.scrollTo(list.size() - 1);

                        // If we are just adding one new message, animate it
                        if (list.size() == oldSize + 1 && oldSize > 0) {
                            // This is a more advanced feature that requires getting the last cell
                            // For now, we will skip the animation to keep it simple, but this is where it would go.
                            // A simple animation would be on the entire list view.
                        }
                    }
                }
        , Throwable::printStackTrace);
    }

    @FXML public void onAttach(){
        FileChooser fc = new FileChooser();
        fc.setTitle("Attach Image");
        pendingAttachment = fc.showOpenDialog(messageList.getScene().getWindow());
    }

    @FXML public void onSend(){
        String text = messageField.getText().trim();
        if (text.isEmpty() && pendingAttachment==null) return;
        disableInputs(true);
        FX.runAsync(() -> {
            Message m = new Message();
            m.setId(UUID.randomUUID());
            m.setSenderId(currentUser.getId());
            m.setReceiverPrivateChat(privateChatId);
            m.setReceiverGroup(groupId);
            m.setReceiverChannel(channelId);
            m.setContent(text.isEmpty()? null : text);
            if (pendingAttachment != null) {
                validateAttachment(pendingAttachment);
                try { m.setImagePath(ImageStorage.saveAttachment(pendingAttachment)); }
                catch (IOException e) { throw new RuntimeException(e); }
            }
            m.setTimestamp(LocalDateTime.now());
            m.setReadStatus("SENT");
            boolean ok = messageDAO.insert(m);
            if (!ok) throw new RuntimeException("DB insert failed");
            return true;
        }, ok -> {
            messageField.clear();
            pendingAttachment = null;
            disableInputs(false);
            loadHistory();
            // notify typing stopped
            UUID currentChatId = privateChatId != null ? privateChatId : groupId;
            if (currentChatId != null) typingDAO.setTyping(currentChatId, currentUser.getId()); // update last_ts
        }, err -> {
            disableInputs(false);
            err.printStackTrace();
        });
    }

    private void disableInputs(boolean b){ sendButton.setDisable(b); attachButton.setDisable(b); }

    private void validateAttachment(File f){
        if (f.length() > 10 * 1024 * 1024) throw new IllegalArgumentException("File too large (max 10MB)");
        String name = f.getName().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")))
            throw new IllegalArgumentException("Unsupported file type");
    }

    // Typing indicator: update DB when typing and poll DB to show other typers
    private void startTypingPoll(){
        if (typingPollTask!=null && !typingPollTask.isCancelled()) typingPollTask.cancel(true);

        UUID currentChatId = privateChatId != null ? privateChatId : groupId;
        if (currentChatId == null) return; // Don't start polling if there's no chat ID

        typingPollTask = poller.scheduleWithFixedDelay(() -> {
            try {
                // poll typing users in last 5 seconds
                var users = typingDAO.getTypingUsers(currentChatId, 5);
                // remove self
                users.removeIf(u -> u.equals(currentUser.getId()));
                String text = "";
                if (!users.isEmpty()) {
                    text = users.size() + " user(s) typing...";
                }
                final String txt = text;
                Platform.runLater(() -> typingLabel.setText(txt));
            } catch (Throwable t){ t.printStackTrace(); }
        }, 1, 2, TimeUnit.SECONDS);

        // Also, when user types, update DB
        messageField.textProperty().addListener((obs, oldv, newv) -> {
            if (currentChatId !=null && newv!=null && !newv.isBlank()) {
                FX.runAsync(() -> typingDAO.setTyping(currentChatId, currentUser.getId()), r-> {}, Throwable::printStackTrace);
            }
        });
    }
}