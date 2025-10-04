package com.example.healthflow.config;

import com.zaxxer.hikari.HikariConfig;
import java.util.Properties;

public class DatabaseConfig {
    /**
     * Creates a HikariConfig from properties, with environment variable fallbacks for sensitive data.
     * Environment variables take precedence over properties file values.
     */
    public static HikariConfig from(Properties p) {
        HikariConfig c = new HikariConfig();

        // Get values from environment variables if available, otherwise use properties
        String dbUrl = getEnvOrProperty("DB_URL", p, "db.url");
        String dbUser = getEnvOrProperty("DB_USER", p, "db.user");
        String dbPassword = getEnvOrProperty("DB_PASSWORD", p, "db.password");

        c.setJdbcUrl(dbUrl);
        c.setUsername(dbUser);
        c.setPassword(dbPassword);
        c.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.max","10")));
        c.setMinimumIdle(Integer.parseInt(p.getProperty("db.pool.min","0")));
        c.setDriverClassName("org.postgresql.Driver");
        c.setPoolName("HealthFlowPool");

        return c;
    }


    private static String getEnvOrProperty(String envName, Properties props, String propName) {
        String envValue = System.getenv(envName);
        return (envValue != null && !envValue.isEmpty()) ? envValue : props.getProperty(propName);
    }
}
