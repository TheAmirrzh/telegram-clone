package com.telegramapp.ui.controllers;

import com.telegramapp.model.User;
import com.telegramapp.service.AuthService;
import com.telegramapp.util.FX; // Make sure this import is present
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    // Remove the AuthService from the constructor to prevent early DB initialization
    private AuthService authService;

    @FXML
    private void onLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            messageLabel.setText("Enter username and password.");
            return;
        }

        messageLabel.setText("Logging in...");

        // Perform all database operations in the background
        FX.runAsync(() -> {
            // This runs on a background thread
            try {
                // Initialize the AuthService here, in the background
                if (authService == null) {
                    authService = new AuthService();
                }
                return authService.login(u, p);
            } catch (SQLException e) {
                e.printStackTrace();
                // We'll pass the exception back to the UI thread to show an error
                throw new RuntimeException("Database error during login", e);
            }
        }, (userOptional) -> {
            // This runs back on the UI thread
            if (userOptional.isPresent()) {
                messageLabel.setText("Login successful. Opening...");
                openMainWindow(userOptional.get());
            } else {
                messageLabel.setText("Login failed. Wrong credentials.");
            }
        }, (error) -> {
            // This runs on the UI thread if an error occurred
            messageLabel.setText("Error: " + error.getMessage());
            error.printStackTrace();
        });
    }

    @FXML
    private void onRegister() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            messageLabel.setText("Enter username and password to register.");
            return;
        }

        messageLabel.setText("Registering...");

        // Perform all database operations in the background
        FX.runAsync(() -> {
            // This runs on a background thread
            try {
                if (authService == null) {
                    authService = new AuthService();
                }
                return authService.register(u, p, u);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
        }, (newUser) -> {
            // This runs back on the UI thread
            messageLabel.setText("Registered user: " + newUser.getUsername());
            openMainWindow(newUser);
        }, (error) -> {
            // This runs on the UI thread if an error occurred
            messageLabel.setText("Register failed: " + error.getMessage());
            error.printStackTrace();
        });
    }

    private void openMainWindow(User user) {
        try {
            // This part is safe as it only deals with UI
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());
            MainController ctrl = loader.getController();
            ctrl.setCurrentUser(user); // This will trigger the async loading in MainController

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Telegram - " + user.getDisplayName());
            stage.setScene(scene);
        } catch (IOException ex) {
            ex.printStackTrace();
            messageLabel.setText("Failed to load main window.");
        }
    }
}