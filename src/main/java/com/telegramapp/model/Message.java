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

    public Message(String senderId, String receiverId, String receiverType, String content) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.content = content;
        this.mediaType = null;
        this.mediaPath = null;
        this.timestamp = LocalDateTime.now();
        this.readStatus = "UNREAD";
    }

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
    }

    public Message(String id, String senderId, String receiverId, String receiverType, String content, String mediaType, String mediaPath, LocalDateTime timestamp, String readStatus) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.content = content;
        this.mediaType = mediaType;
        this.mediaPath = mediaPath;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.readStatus = readStatus;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getReceiverType() { return receiverType; }
    public String getContent() { return content; }
    public String getMediaType() { return mediaType; }
    public String getMediaPath() { return mediaPath; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getReadStatus() { return readStatus; }

    public void setReadStatus(String s){ this.readStatus = s; }

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
