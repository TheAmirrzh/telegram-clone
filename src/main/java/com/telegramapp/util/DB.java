package com.telegramapp.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DB {
    private static HikariDataSource ds;

    static {
        HikariConfig config = new HikariConfig();
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/telegramdb");
        String user = System.getenv().getOrDefault("DB_USER", "telegram_user");
        String pass = System.getenv().getOrDefault("DB_PASS", "telegram_pass");

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        ds = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static DataSource getDataSource() {
        return ds;
    }
}
