package com.telegramapp.dao;

import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TestContacts {
    public static void main(String[] args) {
        try {
            UserDAOImpl userDAO = new UserDAOImpl();

            // Create test users
            User user1 = new User("testuser1", "password", "Test User 1");
            User user2 = new User("testuser2", "password", "Test User 2");

            // Save users to DB
            userDAO.save(user1);
            userDAO.save(user2);

            System.out.println("=== Testing Contact Addition ===");

            // Add user2 as a contact of user1
            user1.addContact(user2.getId());
            userDAO.update(user1);

            System.out.println("User1's contact IDs after update: " + user1.getContactIds());

            // Re-fetch user1 to ensure data is persisted
            Optional<User> fetchedUser1Opt = userDAO.findById(user1.getId());
            if (fetchedUser1Opt.isPresent()) {
                User fetchedUser1 = fetchedUser1Opt.get();
                System.out.println("Fetched User1's contact IDs from DB: " + fetchedUser1.getContactIds());

                // Test getting contacts
                List<User> contacts = userDAO.getContacts(fetchedUser1.getId());
                System.out.println("Contacts for user-1: " + contacts.size());
                for (User contact : contacts) {
                    System.out.println("  - " + contact.getUsername() + " (" + contact.getDisplayName() + ")");
                }

                if (contacts.stream().anyMatch(c -> c.getId().equals(user2.getId()))) {
                    System.out.println("SUCCESS: Contact relationship verified.");
                } else {
                    System.out.println("FAILURE: Contact relationship not found.");
                }
            } else {
                System.out.println("FAILURE: Could not fetch user1 after update.");
            }

            // Clean up
            userDAO.delete(user1.getId());
            userDAO.delete(user2.getId());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}