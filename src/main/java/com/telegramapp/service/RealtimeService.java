package com.telegramapp.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.telegramapp.model.Message;
import com.telegramapp.realtime.PgNotifyClient;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * RealtimeService wires database NOTIFY payloads into the UI list model.
 * It expects NOTIFY payloads with JSON content like:
 * {"chatType":"private","id":"<chatId>","messageId":"<uuid>"}
 *
 * The onNewMessage consumer receives a Message instance constructed with the id.
 * In your UI code you can fetch the full message by id or simply refresh the view.
 */
public class RealtimeService implements AutoCloseable {
    private final PgNotifyClient client;
    private final Consumer<Message> onNewMessage;

    public RealtimeService(Consumer<Message> onNewMessage) {
        this.onNewMessage = onNewMessage;
        this.client = new PgNotifyClient(this::handlePayload);
        try {
            this.client.start();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start realtime client", e);
        }
    }

    public void subscribePrivate(UUID chatId) {
        client.listen("private_" + chatId.toString().replace("-", ""));
    }

    public void subscribeGroup(UUID groupId) {
        client.listen("group_" + groupId.toString().replace("-", ""));
    }

    public void subscribeChannel(UUID channelId) {
        client.listen("channel_" + channelId.toString().replace("-", ""));
    }

    private void handlePayload(String payload) {
        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            if (!obj.has("messageId")) return;
            UUID messageId = UUID.fromString(obj.get("messageId").getAsString());
            // Callback on FX thread to allow UI-safe operations
            Platform.runLater(() -> {
                try {
                    onNewMessage.accept(new Message(messageId));
                } catch (Throwable t) {
                    // swallow
                }
            });
        } catch (Exception e) {
            // ignore malformed payloads
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
