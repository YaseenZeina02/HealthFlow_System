package com.example.healthflow.db;
import com.example.healthflow.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class Database {
    private static final HikariDataSource ds;

    static {
        try (InputStream in = Database.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new RuntimeException("application.properties not found in resources!");
            }
            Properties props = new Properties();
            props.load(in);

            HikariConfig cfg = DatabaseConfig.from(props);
            ds = new HikariDataSource(cfg);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database pool", e);
        }
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