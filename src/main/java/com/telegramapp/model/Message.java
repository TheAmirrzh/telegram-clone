package com.telegramapp.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Message {
    private UUID id;
    private UUID senderId;
    private UUID receiverPrivateChat;
    private UUID receiverGroup;
    private UUID receiverChannel;
    private String content;
    private String imagePath;
    private LocalDateTime timestamp;
    private String readStatus;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }
    public UUID getReceiverPrivateChat() { return receiverPrivateChat; }
    public void setReceiverPrivateChat(UUID receiverPrivateChat) { this.receiverPrivateChat = receiverPrivateChat; }
    public UUID getReceiverGroup() { return receiverGroup; }
    public void setReceiverGroup(UUID receiverGroup) { this.receiverGroup = receiverGroup; }
    public UUID getReceiverChannel() { return receiverChannel; }
    public void setReceiverChannel(UUID receiverChannel) { this.receiverChannel = receiverChannel; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }

    @Override public boolean equals(Object o){ return o instanceof Message && Objects.equals(id, ((Message)o).getId()); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
