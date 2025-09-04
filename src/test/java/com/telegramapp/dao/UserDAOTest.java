package com.telegramapp.dao;

import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.User;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {
    private static final UserDAO userDAO = new UserDAOImpl();
    private static User testUser;
    // Use a unique username for each test run to prevent duplicate key errors
    private static final String UNIQUE_USERNAME = "testuser_" + UUID.randomUUID().toString();

    @BeforeAll
    public static void setup() throws SQLException {
        testUser = new User(UNIQUE_USERNAME, "$2a$10$testhash", "Test User");
        userDAO.save(testUser);
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        if (testUser != null) {
            userDAO.delete(testUser.getId());
        }
    }

    @Test
    @Order(1)
    public void testFindByUsername() throws SQLException {
        Optional<User> u = userDAO.findByUsername(UNIQUE_USERNAME);
        assertTrue(u.isPresent());
        assertEquals(UNIQUE_USERNAME, u.get().getUsername());
    }

    @Test
    @Order(2)
    public void testFindById() throws SQLException {
        Optional<User> u = userDAO.findById(testUser.getId());
        assertTrue(u.isPresent());
        assertEquals(testUser.getId(), u.get().getId());
    }
}