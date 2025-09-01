package com.telegramapp.ui;

import com.telegramapp.model.Message;
import com.telegramapp.model.User;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

public class MessageCell extends ListCell<Message> {
    private final User currentUser;
    private final HBox graphic = new HBox();
    private final Label bubble = new Label();

    public MessageCell(User currentUser) {
        this.currentUser = currentUser;
        bubble.getStyleClass().add("message-bubble");
        graphic.getChildren().add(bubble);

        // The cell itself should be transparent
        getStyleClass().add("message-cell");
        setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        boolean mine = item.getSenderId() != null && item.getSenderId().equals(currentUser.getId());

        // Combine text and image path for display
        String content = item.getContent() != null ? item.getContent() : "";
        String imageInfo = item.getImagePath() != null ? "\n[Image Attachment]" : "";
        bubble.setText(content + imageInfo);

        // Remove old styles and apply new ones
        bubble.getStyleClass().removeAll("message-self", "message-other");
        bubble.getStyleClass().add(mine ? "message-self" : "message-other");

        // Align the HBox container, not the bubble itself
        graphic.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        setGraphic(graphic);
    }
}