package com.telegramapp.service;

import com.telegramapp.model.User;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to handle privacy settings and security features
 */
public class PrivacyService {
    private final ContactService contactService;

    // Rate limiting for contact requests
    private final Map<String, LocalDateTime> lastContactRequest = new ConcurrentHashMap<>();
    private static final int CONTACT_REQUEST_COOLDOWN_MINUTES = 5;

    public enum PrivacySetting {
        EVERYONE,      // Anyone can add you
        CONTACTS_ONLY, // Only your contacts can message you
        NOBODY         // No one can add you (manual approval)
    }

    public PrivacyService(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Check if user can be added as contact based on privacy settings
     */
    public boolean canAddAsContact(String requesterId, String targetUserId) throws SQLException {
        // Check rate limiting
        if (isRateLimited(requesterId)) {
            return false;
        }

        // Check if already blocked
        if (isBlocked(requesterId, targetUserId)) {
            return false;
        }

        // Get target user's privacy settings (you'd store this in user preferences)
        PrivacySetting setting = getUserPrivacySetting(targetUserId);

        switch (setting) {
            case EVERYONE:
                return true;
            case CONTACTS_ONLY:
                return contactService.isContact(targetUserId, requesterId);
            case NOBODY:
                return false; // Would require manual approval
            default:
                return true;
        }
    }

    /**
     * Block a user
     */
    public void blockUser(String userId, String blockedUserId) throws SQLException {
        // Remove from contacts if present
        contactService.removeContact(userId, blockedUserId);

        // Add to blocked list (you'd implement this in database)
        addToBlockedList(userId, blockedUserId);
    }

    /**
     * Unblock a user
     */
    public void unblockUser(String userId, String unblockedUserId) {
        removeFromBlockedList(userId, unblockedUserId);
    }

    /**
     * Report a user for spam/abuse
     */
    public void reportUser(String reporterId, String reportedUserId, String reason) {
        // Log the report (implement proper reporting system)
        System.out.println("User " + reporterId + " reported " + reportedUserId + " for: " + reason);

        // Could automatically block after certain threshold
        // Could notify administrators
    }

    /**
     * Check if a user is being rate limited for contact requests
     */
    private boolean isRateLimited(String userId) {
        LocalDateTime lastRequest = lastContactRequest.get(userId);
        if (lastRequest == null) {
            lastContactRequest.put(userId, LocalDateTime.now());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.minusMinutes(CONTACT_REQUEST_COOLDOWN_MINUTES).isBefore(lastRequest)) {
            return true; // Still in cooldown period
        }

        lastContactRequest.put(userId, now);
        return false;
    }

    /**
     * Check if user is blocked
     */
    private boolean isBlocked(String userId, String targetUserId) {
        // You'd implement this with a proper blocked_users table
        return false; // Placeholder
    }

    /**
     * Get user's privacy setting
     */
    private PrivacySetting getUserPrivacySetting(String userId) {
        // You'd store this in user_settings table
        return PrivacySetting.EVERYONE; // Default
    }

    /**
     * Add user to blocked list
     */
    private void addToBlockedList(String userId, String blockedUserId) {
        // Implement database storage for blocked users
    }

    /**
     * Remove user from blocked list
     */
    private void removeFromBlockedList(String userId, String unblockedUserId) {
        // Implement database removal for blocked users
    }

    /**
     * Get privacy recommendations
     */
    public PrivacyRecommendations getPrivacyRecommendations(String userId) throws SQLException {
        int contactCount = contactService.getContactCount(userId);

        PrivacyRecommendations recommendations = new PrivacyRecommendations();

        if (contactCount == 0) {
            recommendations.addRecommendation(
                    "Add some contacts to get started with messaging!",
                    PrivacyRecommendations.RecommendationType.INFO
            );
        }

        if (contactCount > 100) {
            recommendations.addRecommendation(
                    "Consider organizing your contacts into categories.",
                    PrivacyRecommendations.RecommendationType.TIP
            );
        }

        return recommendations;
    }

    public static class PrivacyRecommendations {
        public enum RecommendationType { INFO, WARNING, TIP }

        private final Map<String, RecommendationType> recommendations = new HashMap<>();

        public void addRecommendation(String message, RecommendationType type) {
            recommendations.put(message, type);
        }

        public Map<String, RecommendationType> getRecommendations() {
            return recommendations;
        }

        public boolean hasRecommendations() {
            return !recommendations.isEmpty();
        }
    }
}