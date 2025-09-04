package com.telegramapp.service;

import com.telegramapp.dao.UserDAO;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService() throws SQLException {
        this.userDAO = new UserDAOImpl();
    }

    public User register(String username, String password, String displayName) throws SQLException {
        Optional<User> existing = userDAO.findByUsername(username);
        if (existing.isPresent()) throw new IllegalArgumentException("Username already exists");

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, hash, displayName);
        userDAO.save(user);
        return user;
    }

    public Optional<User> login(String username, String password) throws SQLException {
        Optional<User> opt = userDAO.findByUsername(username);
        if (opt.isEmpty()) return Optional.empty();
        User u = opt.get();
        if (BCrypt.checkpw(password, u.getPasswordHash())) {
            return Optional.of(u);
        }
        return Optional.empty();
    }
}
