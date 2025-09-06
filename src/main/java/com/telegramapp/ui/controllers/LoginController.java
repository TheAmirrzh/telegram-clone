package com.telegramapp.ui.controllers;

import com.telegramapp.model.User;
import com.telegramapp.service.AuthService;
import com.telegramapp.util.FX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private AuthService authService;

    @FXML
    private void initialize() {
        // Pre-load the AuthService in the background so it's ready when the user clicks.
        // This avoids a delay on the first click.
        FX.runAsync(() -> {
            try {
                if (authService == null) {
                    authService = new AuthService();
                }
            } catch (SQLException e) {
                // We can't do much if the DB isn't available, show error on UI.
                Platform.runLater(() -> messageLabel.setText("Error: Database not available."));
                e.printStackTrace();
            }
            return null;
        }, null, null);
    }

    @FXML
    private void onLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            messageLabel.setText("Enter username and password.");
            return;
        }
        messageLabel.setText("Logging in...");
        FX.runAsync(() -> {
            try {
                // Ensure authService is initialized
                if (authService == null) {
                    authService = new AuthService();
                }
                return authService.login(u, p);
            } catch (Exception e) {
                throw new RuntimeException("Database error during login", e);
            }
        }, (userOptional) -> {
            if (userOptional.isPresent()) {
                messageLabel.setText("Login successful. Opening...");
                openMainWindow(userOptional.get());
            } else {
                messageLabel.setText("Login failed. Wrong credentials.");
            }
        }, (error) -> {
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
        FX.runAsync(() -> {
            try {
                if (authService == null) {
                    authService = new AuthService();
                }
                // Assuming a simple display name for registration
                return authService.register(u, p, u);
            } catch (Exception e) {
                // The exception message from AuthService is user-friendly ("Username already exists")
                throw new RuntimeException(e.getMessage(), e);
            }
        }, (newUser) -> {
            messageLabel.setText("Registered user: " + newUser.getUsername());
            openMainWindow(newUser);
        }, (error) -> {
            messageLabel.setText("Register failed: " + error.getMessage());
            error.printStackTrace();
        });
    }

    private void openMainWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            // Added default dimensions (1200x800) to the Scene constructor.
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            MainController ctrl = loader.getController();
            ctrl.setCurrentUser(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Telegram - " + user.getDisplayName());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException ex) {
            ex.printStackTrace();
            messageLabel.setText("Failed to load main window.");
            FX.showError("Critical Error: Could not load main application window. Check FXML files.");
        }
    }
}

