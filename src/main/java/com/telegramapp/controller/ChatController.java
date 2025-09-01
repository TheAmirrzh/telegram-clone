package com.telegramapp.controller;

import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import com.telegramapp.service.RealtimeService;
import com.telegramapp.ui.MessageCell;
import com.telegramapp.dao.ChatDAO;
import com.telegramapp.dao.MessageDAO;
import com.telegramapp.dao.TypingDAO;
import com.telegramapp.util.FX;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.UUID;

/**
 * ChatController - controls chat UI. This version subscribes to realtime updates.
 */
public class ChatController {

    @FXML private Label chatTitle;
    @FXML private ListView<Message> messageList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button attachButton;

    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final MessageDAO messageDAO = new MessageDAO();
    private final ChatDAO chatDAO = new ChatDAO();
    private final TypingDAO typingDAO = new TypingDAO();

    private User currentUser;
    private UUID privateChatId;
    private UUID groupId;
    private UUID channelId;

    // realtime
    private RealtimeService realtime;

    public void initialize(){
        messageList.setFocusTraversable(false);
        messageList.setItems(messages);
        ensureCellFactory();

        // Initialize realtime client and subscribe callback
        realtime = new RealtimeService(msg -> {
            // simplest approach: reload history when a message arrives
            // you can replace with incremental fetch by messageId if preferred
            loadHistory();
        });
    }

    private void ensureCellFactory(){
        if (messageList.getCellFactory()==null && currentUser!=null) {
            messageList.setCellFactory(lv -> new MessageCell(currentUser));
        }
    }

    public void setCurrentUser(User u){
        this.currentUser = u;
        ensureCellFactory();
    }

    public void setPrivateChat(UUID chatId){
        this.privateChatId = chatId;
        this.groupId = null;
        this.channelId = null;
        chatTitle.setText("Private Chat");
        loadHistory();
        if (realtime != null) realtime.subscribePrivate(chatId);
        startTypingPoll();
    }

    public void setGroupId(UUID id){
        this.groupId = id;
        this.privateChatId = null;
        this.channelId = null;
        chatTitle.setText("Group");
        loadHistory();
        if (realtime != null) realtime.subscribeGroup(id);
        startTypingPoll();
    }

    public void setChannelId(UUID id){
        this.channelId = id;
        this.privateChatId = null;
        this.groupId = null;
        chatTitle.setText("Channel");
        loadHistory();
        if (realtime != null) realtime.subscribeChannel(id);
        // channels don't use typing indicators
    }

    public void loadHistory(){
        FX.runAsync(() -> {
            List<Message> list;
            if (privateChatId!=null) list = messageDAO.loadPrivateChatHistory(privateChatId, 200);
            else if (groupId!=null) list = messageDAO.loadGroupHistory(groupId, 200);
            else if (channelId!=null) list = messageDAO.loadChannelHistory(channelId, 200);
            else list = List.of();
            return list;
        }, list -> {
            messages.setAll(list);
            // scroll to bottom
            if (!messages.isEmpty()) {
                Platform.runLater(() -> messageList.scrollTo(messages.size()-1));
            }
        }, Throwable::printStackTrace);
    }

    @FXML
    public void onSend(){
        String txt = messageField.getText();
        if (txt==null || txt.isBlank()) return;
        Message m = new Message();
        m.setId(UUID.randomUUID());
        m.setSenderId(currentUser.getId());
        m.setContent(txt);
        if (privateChatId!=null) m.setReceiverPrivateChat(privateChatId);
        if (groupId!=null) m.setReceiverGroup(groupId);
        if (channelId!=null) m.setReceiverChannel(channelId);
        boolean ok = messageDAO.insert(m);
        if (ok) {
            messageField.clear();
            // optimistic UI update: append and scroll
            messages.add(m);
            Platform.runLater(() -> messageList.scrollTo(messages.size()-1));
        } else {
            // show error
            FX.showError("Failed to send message");
        }
    }

    private void startTypingPoll(){
        // keep existing implementation or replace with realtime typing later
    }

    public void dispose(){
        // called when controller is closed
        try { if (realtime!=null) realtime.close(); } catch (Exception ignore) {}
    }

    // other handlers (attach etc.) remain unchanged
}
