package com.telegramapp.ui;

import com.telegramapp.model.Message;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

import java.io.File;
import java.awt.Desktop;

/**
 * Simple message cell that expects Message#getMediaPath()
 */
public class MessageCell extends ListCell<Message> {
    @Override
    protected void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        HBox row = new HBox(8);
        Label label = new Label(item.getContent() == null ? "" : item.getContent());
        row.getChildren().add(label);

        if (item.getMediaPath() != null && !item.getMediaPath().isEmpty()) {
            Button open = new Button("Open");
            open.setOnAction(ev -> {
                try {
                    File f = new File(item.getMediaPath());
                    if (!f.exists()) f = new File(System.getProperty("user.dir"), item.getMediaPath());
                    if (f.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            row.getChildren().add(open);
        }
        setGraphic(row);
    }
}
