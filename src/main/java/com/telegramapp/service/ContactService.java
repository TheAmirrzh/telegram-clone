package com.telegramapp.service;

import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service class to handle contact management operations
 */
public class ContactService {
    private final UserDAOImpl userDAO;

    public ContactService() throws SQLException {
        this.userDAO = new UserDAOImpl();
    }

    /**
     * Add a user to the current user's contacts
     */
    public boolean addContact(String currentUserId, String contactUserId) throws SQLException {
        if (currentUserId.equals(contactUserId)) {
            throw new IllegalArgumentException("Cannot add yourself as a contact");
        }

        Optional<User> currentUserOpt = userDAO.findById(currentUserId);
        Optional<User> contactUserOpt = userDAO.findById(contactUserId);

        if (currentUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Current user not found");
        }
        if (contactUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Contact user not found");
        }

        User currentUser = currentUserOpt.get();
        boolean added = currentUser.addContact(contactUserId);

        if (added) {
            userDAO.update(currentUser);
        }

        return added;
    }

    /**
     * Remove a user from the current user's contacts
     */
    public boolean removeContact(String currentUserId, String contactUserId) throws SQLException {
        Optional<User> currentUserOpt = userDAO.findById(currentUserId);

        if (currentUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Current user not found");
        }

        User currentUser = currentUserOpt.get();
        boolean removed = currentUser.removeContact(contactUserId);

        if (removed) {
            userDAO.update(currentUser);
        }

        return removed;
    }

    /**
     * Get all contacts for a user
     */
    public List<User> getContacts(String userId) throws SQLException {
        return userDAO.getContacts(userId);
    }

    /**
     * Check if a user is in contacts
     */
    public boolean isContact(String currentUserId, String userId) throws SQLException {
        Optional<User> currentUserOpt = userDAO.findById(currentUserId);

        if (currentUserOpt.isEmpty()) {
            return false;
        }

        return currentUserOpt.get().isContact(userId);
    }

    /**
     * Search users for adding contacts (excludes current user and existing contacts)
     */
    public List<User> searchUsersForContacts(String query, String currentUserId) throws SQLException {
        return userDAO.searchUsersForContacts(query, currentUserId);
    }

    /**
     * Get contact count for a user
     */
    public int getContactCount(String userId) throws SQLException {
        Optional<User> userOpt = userDAO.findById(userId);
        return userOpt.map(User::getContactCount).orElse(0);
    }

    /**
     * Get mutual contacts between two users
     */
    public List<User> getMutualContacts(String userId1, String userId2) throws SQLException {
        Optional<User> user1Opt = userDAO.findById(userId1);
        Optional<User> user2Opt = userDAO.findById(userId2);

        if (user1Opt.isEmpty() || user2Opt.isEmpty()) {
            return List.of();
        }

        User user1 = user1Opt.get();
        User user2 = user2Opt.get();

        // Find intersection of contact sets
        var mutualContactIds = user1.getContactIds();
        mutualContactIds.retainAll(user2.getContactIds());

        return userDAO.findByIds(List.copyOf(mutualContactIds));
    }
}