package com.telegramapp.ui;

import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class MessageCell extends ListCell<Message> {
    private final User currentUser;

    public MessageCell(User currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    protected void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        boolean mine = item.getSenderId() != null && item.getSenderId().equals(currentUser.getId());
        StringBuilder sb = new StringBuilder();
        if (item.getContent() != null) sb.append(item.getContent());
        if (item.getImagePath() != null) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("[image] ").append(item.getImagePath());
        }

        Label bubble = new Label(sb.toString());
        bubble.setWrapText(true);
        bubble.setPadding(new Insets(8));
        bubble.setMaxWidth(420);
        bubble.getStyleClass().add(mine ? "message-self" : "message-other");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = mine ? new HBox(spacer, bubble) : new HBox(bubble, spacer);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setGraphic(row);
        setText(null);
    }
}
