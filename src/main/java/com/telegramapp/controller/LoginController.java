package com.telegramapp.controller;

import com.telegramapp.model.User;
import com.telegramapp.service.AuthService;
import com.telegramapp.service.NotificationService;
import com.telegramapp.util.FX;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML public void onLogin(){
        String u = usernameField.getText();
        String p = passwordField.getText();
        loginButton.setDisable(true);
        FX.runAsync(() -> authService.login(u, p), optUser -> {
            loginButton.setDisable(false);
            if (optUser.isPresent()) {
                openMain(optUser.get());
            } else {
                errorLabel.setText("Invalid credentials");
            }
        }, err -> {
            loginButton.setDisable(false);
            errorLabel.setText("Login error");
            err.printStackTrace();
        });
    }

    @FXML public void onRegister(){
        String u = usernameField.getText();
        String p = passwordField.getText();
        registerButton.setDisable(true);
        FX.runAsync(() -> authService.register(u, p, u), optUser -> {
            registerButton.setDisable(false);
            if (optUser.isPresent()) {
                openMain(optUser.get());
            } else {
                errorLabel.setText("Registration failed (policy/duplicate)");
            }
        }, err -> {
            registerButton.setDisable(false);
            errorLabel.setText("Registration error");
            err.printStackTrace();
        });
    }

    private void openMain(User user){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());
            MainController mc = loader.getController();
            mc.setCurrentUser(user);
            Stage st = (Stage) usernameField.getScene().getWindow();
            st.setScene(scene);
            st.show();
            // start notifications
            NotificationService ns = new NotificationService(user);
            ns.start();
            // store ns in controller for stopping? for simplicity we don't keep a reference here.
        } catch (IOException e){ e.printStackTrace(); }
    }
}
