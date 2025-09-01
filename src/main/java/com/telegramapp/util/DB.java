package com.telegramapp.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DB {
    // Create a logger instance
    private static final Logger logger = LoggerFactory.getLogger(DB.class);
    private static HikariDataSource ds;

    static {
        try {
            HikariConfig config = new HikariConfig();
            String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/telegramdb");
            String user = System.getenv().getOrDefault("DB_USER", "telegram_user");
            String pass = System.getenv().getOrDefault("DB_PASS", "telegram_pass");

            // Log the connection details we are about to use
            logger.info("Attempting to connect to database at URL: {}", url);
            logger.info("Database user: {}", user);

            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(5000); // Set a 5-second connection timeout
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            ds = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully.");

        } catch (Exception e) {
            // If ANY exception happens during initialization, log it critically
            logger.error("CRITICAL: Failed to initialize database connection pool", e);
            // Re-throw the exception to ensure the application fails fast
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static DataSource getDataSource() {
        return ds;
    }
}