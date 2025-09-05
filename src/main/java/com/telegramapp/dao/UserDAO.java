package com.telegramapp.dao;

import com.telegramapp.model.User;
import java.util.Optional;
import java.sql.SQLException;
import java.util.List;

public interface UserDAO {
    Optional<User> findByUsername(String username) throws SQLException;
    Optional<User> findById(String id) throws SQLException;
    void save(User user) throws SQLException;
    List<User> findAll() throws SQLException;
    List<User> findAllExcept(String id) throws SQLException;
    void delete(String id) throws SQLException;
    void update(User user) throws SQLException; // <-- ADD THIS LINE


}
