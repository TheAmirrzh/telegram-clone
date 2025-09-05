package com.telegramapp.model;

public class ChannelSubscriberInfo {
    private final String userId;
    private final String role;

    public ChannelSubscriberInfo(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
