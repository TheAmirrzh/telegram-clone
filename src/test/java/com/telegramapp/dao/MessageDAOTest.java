package com.telegramapp.dao;

import com.telegramapp.dao.impl.MessageDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageDAOTest {

    @Test
    public void insertAndLoad() throws SQLException {
        UserDAO userDAO = new UserDAOImpl();
        MessageDAO messageDAO = new MessageDAOImpl();

        User sender = new User("sender_msg_test_final", "hash1", "Sender");
        User receiver = new User("receiver_msg_test_final", "hash2", "Receiver");
        Message message = null;

        try {
            userDAO.save(sender);
            userDAO.save(receiver);

            message = new Message(sender.getId(), receiver.getId(), "USER", "hello test");
            messageDAO.save(message);

            // Create a final copy of the message variable for the lambda
            final Message finalMessage = message;

            List<Message> list = messageDAO.findConversation("USER", receiver.getId(), sender.getId());

            // Use the final variable inside the lambda expression
            assertTrue(list.stream().anyMatch(x -> x.getId().equals(finalMessage.getId())),
                    "The saved message should be found");

        } finally {
            // Cleanup in the correct order
            if (message != null) {
                messageDAO.delete(message.getId());
            }
            userDAO.delete(sender.getId());
            userDAO.delete(receiver.getId());
        }
    }
}