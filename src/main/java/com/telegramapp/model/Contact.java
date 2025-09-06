package com.telegramapp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Enhanced Contact model with additional features
 */
public class Contact {
    private final String id;
    private String userId;
    private String contactUserId;
    private String customName; // Allow custom names for contacts
    private boolean isFavorite;
    private boolean isBlocked;
    private ContactCategory category;
    private LocalDateTime addedDate;
    private LocalDateTime lastInteraction;
    private String notes; // Personal notes about the contact

    public enum ContactCategory {
        FAMILY("Family", "👨‍👩‍👧‍👦"),
        FRIENDS("Friends", "👥"),
        WORK("Work", "💼"),
        BUSINESS("Business", "🏢"),
        OTHER("Other", "📱");

        private final String displayName;
        private final String emoji;

        ContactCategory(String displayName, String emoji) {
            this.displayName = displayName;
            this.emoji = emoji;
        }

        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }

        @Override
        public String toString() { return emoji + " " + displayName; }
    }

    public Contact(String userId, String contactUserId) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.customName = null;
        this.isFavorite = false;
        this.isBlocked = false;
        this.category = ContactCategory.OTHER;
        this.addedDate = LocalDateTime.now();
        this.lastInteraction = LocalDateTime.now();
        this.notes = "";
    }

    // Full constructor
    public Contact(String id, String userId, String contactUserId, String customName,
                   boolean isFavorite, boolean isBlocked, ContactCategory category,
                   LocalDateTime addedDate, LocalDateTime lastInteraction, String notes) {
        this.id = id;
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.customName = customName;
        this.isFavorite = isFavorite;
        this.isBlocked = isBlocked;
        this.category = category != null ? category : ContactCategory.OTHER;
        this.addedDate = addedDate != null ? addedDate : LocalDateTime.now();
        this.lastInteraction = lastInteraction != null ? lastInteraction : LocalDateTime.now();
        this.notes = notes != null ? notes : "";
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getContactUserId() { return contactUserId; }
    public String getCustomName() { return customName; }
    public boolean isFavorite() { return isFavorite; }
    public boolean isBlocked() { return isBlocked; }
    public ContactCategory getCategory() { return category; }
    public LocalDateTime getAddedDate() { return addedDate; }
    public LocalDateTime getLastInteraction() { return lastInteraction; }
    public String getNotes() { return notes; }

    // Setters
    public void setCustomName(String customName) { this.customName = customName; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setBlocked(boolean blocked) { this.isBlocked = blocked; }
    public void setCategory(ContactCategory category) { this.category = category; }
    public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;
        Contact contact = (Contact) o;
        return Objects.equals(id, contact.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", contactUserId='" + contactUserId + '\'' +
                ", customName='" + customName + '\'' +
                ", category=" + category +
                '}';
    }
}