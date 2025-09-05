package com.telegramapp.model;

import java.time.LocalDateTime;

// This class is a wrapper to hold all the necessary UI information for a single chat list entry.
public class ChatListItem {
    private final Object chatObject;
    private String lastMessage;
    private int unreadCount;
    private LocalDateTime lastMessageTimestamp;

    public ChatListItem(Object chatObject, String lastMessage, int unreadCount, LocalDateTime lastMessageTimestamp) {
        this.chatObject = chatObject;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public Object getChatObject() {
        return chatObject;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public LocalDateTime getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public String getDisplayName() {
        if (chatObject instanceof User) {
            return ((User) chatObject).getDisplayName();
        } else if (chatObject instanceof Group) {
            return ((Group) chatObject).getName();
        } else if (chatObject instanceof Channel) {
            return ((Channel) chatObject).getName();
        }
        return "Unknown";
    }

    public User getUser() {
        if (chatObject instanceof User) {
            return ((User) chatObject);
        }
        // For groups/channels, we don't have a single representative user avatar in this model
        return null;
    }
}

