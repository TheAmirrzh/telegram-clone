package com.telegramapp.service;

import com.telegramapp.dao.UserDAO;
import com.telegramapp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public Optional<User> login(String username, String plainPassword){
        return userDAO.findByUsername(username)
                .filter(u -> BCrypt.checkpw(plainPassword, u.getPasswordHash()));
    }

    public Optional<User> register(String username, String plainPassword, String profileName){
        if (username==null || username.length()<3) return Optional.empty();
        if (plainPassword==null || plainPassword.length()<8) return Optional.empty();
        if (!plainPassword.matches(".*[A-Z].*") || !plainPassword.matches(".*[0-9].*")) return Optional.empty();
        if (userDAO.findByUsername(username).isPresent()) return Optional.empty();
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User u = new User(UUID.randomUUID(), username, hash);
        u.setProfileName(profileName);
        boolean ok = userDAO.create(u);
        return ok ? Optional.of(u) : Optional.empty();
    }
}
