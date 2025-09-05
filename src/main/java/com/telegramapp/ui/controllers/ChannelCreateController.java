package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.model.Channel;
import com.telegramapp.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ChannelCreateController {
    @FXML private TextField channelNameField;
    @FXML private Label messageLabel;

    private User currentUser;

    public void init(User currentUser) {
        this.currentUser = currentUser;
    }

    @FXML
    private void onCreate() {
        String name = channelNameField.getText().trim();
        if (name.isEmpty()) {
            messageLabel.setText("Channel name required.");
            return;
        }
        try {
            ChannelDAOImpl channelDAO = new ChannelDAOImpl();
            Channel c = new Channel(name, currentUser.getId());
            // Save the channel first
            channelDAO.save(c);
            // Then, add the creator as the OWNER
            channelDAO.addSubscriber(c.getId(), currentUser.getId(), "OWNER");

            messageLabel.setText("Channel created.");
            Stage s = (Stage) channelNameField.getScene().getWindow();
            s.close();
        } catch (SQLException e) {
            messageLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        Stage s = (Stage) channelNameField.getScene().getWindow();
        s.close();
    }
}
