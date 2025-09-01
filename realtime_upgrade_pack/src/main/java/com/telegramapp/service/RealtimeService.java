package com.telegramapp.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.telegramapp.model.Message;
import com.telegramapp.realtime.PgNotifyClient;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * RealtimeService wires database NOTIFY payloads into the UI list model.
 * It expects NOTIFY payloads with JSON content like:
 * {"chatType":"private","id":"<chatId>","messageId":"<uuid>"}
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
            UUID messageId = UUID.fromString(obj.get("messageId").getAsString());
            // In a real app, we would fetch the message by ID; here we just trigger UI refresh via callback
            Platform.runLater(() -> onNewMessage.accept(new Message(messageId)));
        } catch (Exception e) {
            // Ignore malformed payloads
        }
    }

    @Override
    public void close() {
        client.close();
    }
}