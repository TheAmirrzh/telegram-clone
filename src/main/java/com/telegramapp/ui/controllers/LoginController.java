package com.telegramapp.ui.controllers;

import com.telegramapp.model.User;
import com.telegramapp.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private AuthService authService;

    public LoginController() {
        try {
            this.authService = new AuthService();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            messageLabel.setText("Enter username and password.");
            return;
        }
        try {
            Optional<User> userOpt = authService.login(u, p);
            if (userOpt.isPresent()) {
                messageLabel.setText("Login successful. Opening...");
                openMainWindow(userOpt.get());
            } else {
                messageLabel.setText("Login failed. Wrong credentials.");
            }
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegister() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            messageLabel.setText("Enter username and password to register.");
            return;
        }
        try {
            User newUser = authService.register(u, p, u);
            messageLabel.setText("Registered user: " + newUser.getUsername());
            openMainWindow(newUser);
        } catch (Exception e) {
            messageLabel.setText("Register failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openMainWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/telegram/ui/views/main.fxml"));
            Scene scene = new Scene(loader.load());
            MainController ctrl = loader.getController();
            ctrl.setCurrentUser(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Telegram - " + user.getDisplayName());
            stage.setScene(scene);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
