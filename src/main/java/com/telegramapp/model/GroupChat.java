package com.telegramapp.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class GroupChat {
    private final String id;
    private String groupId;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;

    public GroupChat(String groupId, String senderId, String content) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public GroupChat(String id, String groupId, String senderId, String content, LocalDateTime timestamp) {
        this.id = id;
        this.groupId = groupId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupChat)) return false;
        GroupChat other = (GroupChat) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "GroupChat{id='" + id + "', groupId='" + groupId + "', senderId='" + senderId + "', content='" + content + "', ts=" + timestamp + "}";
    }
}
