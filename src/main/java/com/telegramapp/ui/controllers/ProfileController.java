package com.telegramapp.ui.controllers;

import com.telegramapp.App;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.User;
import com.telegramapp.util.FX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.UUID;

public class ProfileController {

    @FXML private ImageView profileImageView;
    @FXML private TextField displayNameField;
    @FXML private TextArea bioField;
    @FXML private Label messageLabel;

    private User currentUser;
    private UserDAOImpl userDAO = new UserDAOImpl();
    private File selectedImageFile;
    private Stage stage;

    @FXML
    public void initialize() {
        Circle clip = new Circle(50, 50, 50);
        profileImageView.setClip(clip);
    }

    public void initData(User user) {
        this.currentUser = user;
        displayNameField.setText(user.getDisplayName());
        bioField.setText(user.getBio());
        loadAvatar(user, profileImageView);
    }

    @FXML
    private void onChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        selectedImageFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedImageFile != null) {
            try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                profileImageView.setImage(new Image(fis));
                messageLabel.setText("New image selected.");
            } catch (Exception e) {
                e.printStackTrace();
                messageLabel.setText("Failed to load image.");
            }
        }
    }

    @FXML
    private void onSave() {
        String newDisplayName = displayNameField.getText().trim();
        String newBio = bioField.getText().trim();

        if (newDisplayName.isEmpty()) {
            messageLabel.setText("Display name cannot be empty.");
            return;
        }

        FX.runAsync(() -> {
                    try {
                        // Update text fields
                        currentUser.setDisplayName(newDisplayName);
                        currentUser.setBio(newBio);

                        // Handle image update
                        if (selectedImageFile != null) {
                            Path storageDir = Paths.get("storage", "avatars");
                            if (!Files.exists(storageDir)) Files.createDirectories(storageDir);

                            String originalName = selectedImageFile.getName();
                            String extension = "";
                            int i = originalName.lastIndexOf('.');
                            if (i > 0) {
                                extension = originalName.substring(i);
                            }
                            String newFileName = UUID.randomUUID().toString() + extension;
                            Path destPath = storageDir.resolve(newFileName);
                            Files.copy(selectedImageFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                            currentUser.setProfilePicPath(destPath.toString());
                        }

                        userDAO.update(currentUser);
                        return "Profile updated successfully!";
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to save profile: " + e.getMessage());
                    }
                },
                successMessage -> {
                    messageLabel.setText(successMessage);
                    // Close the dialog after a short delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(this::closeDialog);
                        } catch (InterruptedException ignored) {}
                    }).start();
                },
                error -> {
                    messageLabel.setText(error.getMessage());
                });
    }

    @FXML
    private void onLogout() {
        closeDialog();

        // Find the main stage and close it
        Stage mainStage = (Stage) Window.getWindows().stream()
                .filter(window -> window instanceof Stage && window.isShowing())
                .findFirst()
                .orElse(null);

        if (mainStage != null) {
            mainStage.close();
            // Restart the application by showing the login screen
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
                Scene scene = new Scene(loader.load());
                Stage loginStage = new Stage();
                loginStage.setScene(scene);
                loginStage.setTitle("Telegram - Login");
                loginStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeDialog() {
        stage = (Stage) profileImageView.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void loadAvatar(User user, ImageView imageView) {
        if (user == null || imageView == null) return;
        String picPath = user.getProfilePicPath();
        Image avatarImage = null;
        if (picPath != null && !picPath.isBlank()) {
            try (FileInputStream fis = new FileInputStream(new File(picPath))) {
                avatarImage = new Image(fis);
            } catch (Exception e) { /* Fallback */ }
        }
        if (avatarImage == null) {
            try (InputStream defaultAvatarStream = getClass().getResourceAsStream("/assets/default_avatar.png")) {
                if (defaultAvatarStream != null) {
                    avatarImage = new Image(defaultAvatarStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        imageView.setImage(avatarImage);
    }
}

