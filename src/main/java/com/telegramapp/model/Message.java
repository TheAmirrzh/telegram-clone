package com.telegramapp.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Message {
    private final String id;
    private String senderId;
    private String receiverId;
    private String receiverType;
    private String content;
    private String mediaType;
    private String mediaPath;
    private LocalDateTime timestamp;
    private String readStatus;
    private String replyToMessageId; // Added for reply feature

    // Constructor for new text messages
    public Message(String senderId, String receiverId, String receiverType, String content) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.content = content;
        this.mediaType = "TEXT";
        this.mediaPath = null;
        this.timestamp = LocalDateTime.now();
        this.readStatus = "UNREAD";
        this.replyToMessageId = null;
    }

    // Constructor for new media messages
    public Message(String senderId, String receiverId, String receiverType, String content, String mediaType, String mediaPath) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.content = content;
        this.mediaType = mediaType;
        this.mediaPath = mediaPath;
        this.timestamp = LocalDateTime.now();
        this.readStatus = "UNREAD";
        this.replyToMessageId = null;
    }

    // Full constructor for reading from database
    public Message(String id, String senderId, String receiverId, String receiverType, String content, String mediaType, String mediaPath, LocalDateTime timestamp, String readStatus, String replyToMessageId) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.content = content;
        this.mediaType = mediaType;
        this.mediaPath = mediaPath;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.readStatus = readStatus;
        this.replyToMessageId = replyToMessageId;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getReceiverType() { return receiverType; }
    public String getContent() { return content; }
    public String getMediaType() { return mediaType; }
    public String getMediaPath() { return mediaPath; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getReadStatus() { return readStatus; }
    public String getReplyToMessageId() { return replyToMessageId; }

    // --- Setters ---
    public void setReadStatus(String s){ this.readStatus = s; }
    public void setContent(String content) { this.content = content; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        return Objects.equals(id, ((Message) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

