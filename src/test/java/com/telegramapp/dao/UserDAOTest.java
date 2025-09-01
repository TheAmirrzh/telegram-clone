package com.telegramapp.dao;

import com.telegramapp.model.User;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {
    private static final UserDAO userDAO = new UserDAO();
    private static User testUser;

    @BeforeAll
    public static void setup(){
        testUser = new User(UUID.randomUUID(), "testuser", "$2a$10$testhashtesthashtesthashtesthash");
        testUser.setProfileName("Test User");
        userDAO.create(testUser);
    }

    @Test
    @Order(1)
    public void testFindByUsername(){
        Optional<User> u = userDAO.findByUsername("testuser");
        assertTrue(u.isPresent());
        assertEquals("testuser", u.get().getUsername());
    }

    @Test
    @Order(2)
    public void testFindById(){
        Optional<User> u = userDAO.findById(testUser.getId());
        assertTrue(u.isPresent());
        assertEquals(testUser.getId(), u.get().getId());
    }
}
