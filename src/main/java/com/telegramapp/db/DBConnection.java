package com.telegramapp.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static DBConnection instance;
    private final HikariDataSource ds;

    private DBConnection() {
        try {
            System.out.println("DEBUG: Initializing DBConnection...");

            Properties prop = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (input != null) {
                    prop.load(input);
                    System.out.println("DEBUG: Loaded db.properties");
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Could not load db.properties: " + e.getMessage());
            }

            String url = prop.getProperty("db.url", System.getenv("DB_URL"));
            String user = prop.getProperty("db.user", System.getenv("DB_USER"));
            String pass = prop.getProperty("db.password", System.getenv("DB_PASS"));

            // Fallback to default values if nothing is configured
            if (url == null) url = "jdbc:postgresql://localhost:5432/telegramdb";
            if (user == null) user = "telegram_user";
            if (pass == null) pass = "telegram_pass";

            System.out.println("DEBUG: DB URL: " + url);
            System.out.println("DEBUG: DB User: " + user);

            System.out.println("DEBUG: Creating HikariConfig...");
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);

            // OPTIMIZED: Faster initialization settings
            cfg.setConnectionTimeout(5000);       // 5 seconds (reduced from 10)
            cfg.setValidationTimeout(2000);       // 2 seconds (reduced from 5)
            cfg.setInitializationFailTimeout(8000); // 8 seconds (reduced from 15)
            cfg.setLeakDetectionThreshold(0);      // Disable for faster startup

            // OPTIMIZED: Minimal pool for faster startup
            cfg.setMaximumPoolSize(3);             // Reduced from 10
            cfg.setMinimumIdle(1);                 // Start with just 1 connection

            cfg.setAutoCommit(true);
            cfg.setPoolName("telegram-hikari-pool");
            cfg.setIdleTimeout(300000);            // 5 minutes
            cfg.setMaxLifetime(900000);            // 15 minutes

            // OPTIMIZED: Fast connection test
            cfg.setConnectionTestQuery("SELECT 1");

            // OPTIMIZED: Don't validate connections on startup
            cfg.setConnectionInitSql(null);

            System.out.println("DEBUG: Creating HikariDataSource...");
            this.ds = new HikariDataSource(cfg);
            System.out.println("DEBUG: HikariDataSource created successfully");

            // REMOVED: Don't test connection during initialization to speed up startup
            // The first actual query will test the connection

        } catch (Exception e) {
            System.err.println("DEBUG: Failed to initialize DB pool: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize DB pool", e);
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            System.out.println("DEBUG: Creating new DBConnection instance");
            instance = new DBConnection();
        }
        return instance;
    }

    public DataSource getDataSource() {
        return ds;
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = ds.getConnection();
            System.out.println("DEBUG: Successfully obtained connection: " + conn);
            return conn;
        } catch (SQLException e) {
            System.err.println("DEBUG: Failed to get connection: " + e.getMessage());
            throw e;
        }
    }

    public void close() {
        System.out.println("DEBUG: Closing HikariDataSource");
        if (ds != null) ds.close();
    }
}