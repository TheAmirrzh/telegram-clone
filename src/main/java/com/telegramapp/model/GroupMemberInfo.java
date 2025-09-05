package com.telegramapp.model;

public class GroupMemberInfo {
    private final String userId;
    private final String role;

    public GroupMemberInfo(String userId, String role) {
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

