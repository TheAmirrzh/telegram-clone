package com.telegramapp.model;

import java.util.Objects;
import java.util.UUID;

public class User {
    private final String id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String bio;
    private String profilePicPath;
    private String status;

    public User(String username, String passwordHash, String displayName) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.bio = "";
        this.profilePicPath = "";
        this.status = "Offline";
    }

    public User(String id, String username, String passwordHash, String displayName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getBio(){ return bio; }
    public void setBio(String bio){ this.bio = bio; }
    public void setProfilePicPath(String p){ this.profilePicPath = p; }
    public String getProfilePicPath(){ return profilePicPath; }
    public void setStatus(String s){ this.status = s; }
    public String getStatus(){ return status; }

    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User u = (User) o;
        return Objects.equals(id, u.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "User{" + "id='" + id + '\'' + ", username='" + username + '\'' + '}';
    }
}
