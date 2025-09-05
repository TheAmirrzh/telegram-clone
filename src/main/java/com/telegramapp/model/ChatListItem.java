package com.telegramapp.model;

public class ChatListItem {
    private final Object chatObject;
    private final String lastMessage;

    public ChatListItem(Object chatObject, String lastMessage) {
        if (!(chatObject instanceof User || chatObject instanceof Group || chatObject instanceof Channel)) {
            throw new IllegalArgumentException("ChatListItem must wrap a User, Group, or Channel");
        }
        this.chatObject = chatObject;
        this.lastMessage = lastMessage;
    }

    public Object getChatObject() {
        return chatObject;
    }

    public String getLastMessage() {
        return lastMessage;
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
            return (User) chatObject;
        }
        // For groups/channels, we might not have a single user to represent the avatar.
        // Returning null is acceptable, and the UI will use a default.
        return null;
    }
}

