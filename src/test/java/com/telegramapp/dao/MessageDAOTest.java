package com.telegramapp.dao;

import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import org.junit.jupiter.api.*;
import com.telegramapp.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessageDAOTest {

    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private User sender;
    private User receiver;
    private Message message;

    @BeforeAll
    public void setup() throws SQLException {
        userDAO = new UserDAOImpl();
        messageDAO = new MessageDAOImpl();

        // Create users with unique usernames to ensure tests are isolated and don't collide.
        sender = new User("sender_" + UUID.randomUUID(), "hash1", "Test Sender");
        receiver = new User("receiver_" + UUID.randomUUID(), "hash2", "Test Receiver");
        userDAO.save(sender);
        userDAO.save(receiver);
    }

    @AfterEach
    void teardown() throws SQLException {
        // Actually delete messages from database (not just mark as deleted)
        if (message != null) {
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE id = ?")) {
                ps.setString(1, message.getId());
                ps.executeUpdate();
            }
        }

        // Now we can safely delete users
        if (sender != null) {
            userDAO.delete(sender.getId());
        }
        if (receiver != null) {
            userDAO.delete(receiver.getId());
        }
    }

    @Test
    public void testInsertAndLoadConversation() throws SQLException {
        // 1. Arrange: Create and save a new message
        message = new Message(sender.getId(), receiver.getId(), "USER", "hello test message");
        messageDAO.save(message);

        // 2. Act: Attempt to find the conversation containing the new message
        List<Message> conversation = messageDAO.findConversation("USER", receiver.getId(), sender.getId());

        // 3. Assert: The conversation list should not be empty and must contain our specific message
        assertFalse(conversation.isEmpty(), "Conversation list should not be empty after sending a message.");
        assertTrue(conversation.stream().anyMatch(m -> m.getId().equals(message.getId())),
                "The saved message should be found in the conversation.");
    }
}
