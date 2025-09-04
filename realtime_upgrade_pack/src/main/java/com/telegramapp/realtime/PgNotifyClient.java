package com.telegramapp.realtime;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * PgNotifyClient manages a dedicated Postgres connection that LISTENs on channels
 * and dispatches NOTIFY payloads to a callback on a background thread.
 *
 * Channel naming convention:
 *   private_{chatId}
 *   group_{groupId}
 *   channel_{channelId}
 *
 * Payload format (JSON expected): {"chatType":"private|group|channel", "id":"<uuid>", "messageId":"<uuid>"}
 */
public class PgNotifyClient implements AutoCloseable {
    private final Consumer<String> onNotify;
    private volatile boolean running = false;
    private Thread loopThread;
    private Connection conn;
    private PGConnection pgConn;
    private final Set<String> channels = new HashSet<>();

    public PgNotifyClient(Consumer<String> onNotify) {
        this.onNotify = onNotify;
    }

    public synchronized void start() throws SQLException {
        if (running) return;
        conn = DB.getDataSource().getConnection();
        pgConn = conn.unwrap(PGConnection.class);
        running = true;
        loopThread = new Thread(this::loop, "PgNotifyClient-Loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public synchronized void listen(String channel) {
        if (!running) throw new IllegalStateException("Start the client before listen()");
        if (channels.contains(channel)) return;
        try (Statement st = conn.createStatement()) {
            st.execute("LISTEN " + channel);
            channels.add(channel);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to LISTEN " + channel, e);
        }
    }

    public synchronized void unlisten(String channel) {
        if (!running || !channels.contains(channel)) return;
        try (Statement st = conn.createStatement()) {
            st.execute("UNLISTEN " + channel);
            channels.remove(channel);
        } catch (SQLException e) {
            // log and continue
        }
    }

    private void loop() {
        while (running) {
            try {
                // getNotifications(int timeoutMillis) is available since PG JDBC 42.2.6
                PGNotification[] notifications = pgConn.getNotifications(5000);
                if (notifications != null) {
                    for (PGNotification n : notifications) {
                        if (onNotify != null) onNotify.accept(n.getParameter());
                    }
                }
                // keep connection alive
                try (Statement st = conn.createStatement()) { st.execute("SELECT 1"); }
            } catch (SQLException e) {
                // try to reconnect once
                try {
                    if (conn != null) conn.close();
                } catch (Exception ignore) {}
                try {
                    conn = DB.getDataSource().getConnection();
                    pgConn = conn.unwrap(PGConnection.class);
                } catch (SQLException re) {
                    // backoff
                    try { Thread.sleep(Duration.ofSeconds(2).toMillis()); } catch (InterruptedException ie) { /* ignore */ }
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        try {
            if (loopThread != null) loopThread.interrupt();
        } catch (Exception ignore) {}
        try {
            if (conn != null) conn.close();
        } catch (Exception ignore) {}
    }
}