package com.telegramapp;

import com.telegramapp.dao.impl.UserDAOImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static String currentUserId;

    @Override
    public void start(Stage stage) throws Exception {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(App.class.getResource("/css/styles.css").toExternalForm());
            stage.setTitle("Telegram");
            stage.setScene(scene);
            stage.show();
        }
        catch (Exception e) {
            System.out.println("--- JAVAFX APPLICATION FAILED TO START ---");
            e.printStackTrace(); // This will print the real error!
            System.out.println("-----------------------------------------");
        }
    }

    public static void setCurrentUserId(String userId) {
        currentUserId = userId;
    }

    @Override
    public void stop() throws Exception {
        // Set user status to Offline when the app closes
        if (currentUserId != null) {
            try {
                new UserDAOImpl().updateUserStatus(currentUserId, "Offline");
            } catch (Exception e) {
                System.err.println("Failed to update user status on exit.");
                e.printStackTrace();
            }
        }
        super.stop();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
