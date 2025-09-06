package com.telegramapp.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class User {
    private final String id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String bio;
    private String profilePicPath;
    private String status;

    private Set<String> contactIds;

    public User(String username, String passwordHash, String displayName) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.bio = "";
        this.profilePicPath = "";
        this.status = "Offline";
        this.contactIds = new HashSet<>();
    }

    public User(String id, String username, String passwordHash, String displayName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.contactIds = new HashSet<>();
    }

    // getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getBio(){ return bio; }
    public String getProfilePicPath(){ return profilePicPath; }
    public String getStatus(){ return status; }

    // setters
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setBio(String bio){ this.bio = bio; }
    public void setProfilePicPath(String p){ this.profilePicPath = p; }
    public void setStatus(String s){ this.status = s; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }

    public Set<String> getContactIds() {
        return new HashSet<>(contactIds);
    }

    public void setContactIds(Set<String> contactIds) {
        this.contactIds = contactIds != null ? new HashSet<>(contactIds) : new HashSet<>();
    }

    public boolean addContact(String userId) {
        if (userId != null && !userId.equals(this.id)) {
            return contactIds.add(userId);
        }
        return false;
    }

    public boolean removeContact(String userId) {
        return contactIds.remove(userId);
    }

    public boolean isContact(String userId) {
        return contactIds.contains(userId);
    }

    public int getContactCount() {
        return contactIds.size();
    }

    // Helper method to convert contacts to string for database storage
    public String getContactsAsString() {
        if (contactIds.isEmpty()) return "";
        return String.join(",", contactIds);
    }

    // Helper method to set contacts from string (from database)
    public void setContactsFromString(String contactsStr) {
        this.contactIds = new HashSet<>();
        if (contactsStr != null && !contactsStr.trim().isEmpty()) {
            String[] ids = contactsStr.split(",");
            for (String id : ids) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    contactIds.add(trimmed);
                }
            }
        }
    }

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