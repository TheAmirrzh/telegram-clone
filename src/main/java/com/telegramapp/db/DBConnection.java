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
            Properties prop = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (input != null) prop.load(input);
            }
            String url = prop.getProperty("db.url", System.getenv("DB_URL"));
            String user = prop.getProperty("db.user", System.getenv("DB_USER"));
            String pass = prop.getProperty("db.password", System.getenv("DB_PASS"));

            if (url == null || user == null || pass == null) {
                throw new IllegalStateException("DB configuration missing. Check db.properties or env vars DB_URL/DB_USER/DB_PASS");
            }

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(2);
            cfg.setAutoCommit(true);
            cfg.setPoolName("telegram-hikari-pool");
            cfg.setConnectionTimeout(10000);
            cfg.setIdleTimeout(600000);
            cfg.setMaxLifetime(1800000);
            this.ds = new HikariDataSource(cfg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DB pool", e);
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    public DataSource getDataSource() {
        return ds;
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public void close() {
        if (ds != null) ds.close();
    }
}
