package com.example.healthflow.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class Database {
    private static HikariDataSource ds;

    static {
        init();
    }

    private static void init() {
        try (InputStream in = Database.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (in == null) {
                throw new RuntimeException("⚠️ application.properties not found in resources/");
            }

            Properties props = new Properties();
            props.load(in);

            String url  = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");

            if (url == null || user == null) {
                throw new IllegalStateException("db.url / db.user missing from application.properties");
            }

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);

            cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "10")));
            cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "2")));

            // تأكد إنه يشتغل مع Neon
            cfg.addDataSourceProperty("sslmode", "require");

            ds = new HikariDataSource(cfg);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public static DataSource getDataSource() {
        return ds;
    }

    /** Get a pooled connection (always close() after use). */
    public static Connection get() throws SQLException {
        return ds.getConnection();
    }

    /** Graceful shutdown */
    public static void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}