package com.telegramapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(App.class.getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        }
        catch (Exception e) {
            System.out.println("--- JAVAFX APPLICATION FAILED TO START ---");
            e.printStackTrace(); // This will print the real error!
            System.out.println("-----------------------------------------");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
