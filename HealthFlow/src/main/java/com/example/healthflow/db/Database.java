package com.example.healthflow.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.util.Properties;

public final class Database {

    private static volatile HikariDataSource ds;
    private static volatile Properties props;

    private Database() {}

    public enum Status {
        ONLINE, OFFLINE_NETWORK, BAD_CREDENTIALS, CONFIG_MISSING, UNKNOWN_ERROR
    }

    private static Properties loadProps() {
        if (props != null) return props;
        synchronized (Database.class) {
            if (props != null) return props;
            try (InputStream in = Database.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (in == null) throw new RuntimeException("⚠️ application.properties not found in resources/");
                Properties p = new Properties();
                p.load(in);
                props = p;
                return p;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load application.properties", e);
            }
        }
    }

    // أضف connectTimeout و sslmode إذا ناقصين
    private static String normalizeJdbcUrl(String rawUrl) {
        String url = rawUrl;
        if (!url.contains("sslmode=")) {
            url = url.contains("?") ? (url + "&sslmode=require") : (url + "?sslmode=require");
        }
        if (!url.matches(".*(?i)(^|[?&])connectTimeout=\\d+.*")) {
            url = url + (url.contains("?") ? "&" : "?") + "connectTimeout=5"; // بالثواني (درایفر PG)
        }
        return url;
    }

    private static void ensureInit() {
        if (ds != null && !ds.isClosed()) return;
        synchronized (Database.class) {
            if (ds != null && !ds.isClosed()) return;

            Properties p = loadProps();
            String url  = p.getProperty("db.url");
            String user = p.getProperty("db.user");
            String pass = p.getProperty("db.password");

            if (url == null || user == null) {
                throw new IllegalStateException("db.url / db.user missing from application.properties");
            }

            url = normalizeJdbcUrl(url);

            // 1) اختبار مباشر قبل الـpool (يكشف SSL/اسم DB/اعتماد/شبكة)
            try {
                DriverManager.setLoginTimeout(7);
                Properties jdbcProps = new Properties();
                jdbcProps.setProperty("user", user);
                if (pass != null) jdbcProps.setProperty("password", pass);
                try (Connection c = DriverManager.getConnection(url, jdbcProps)) {
                    // OK
                }
            } catch (SQLException e) {
                throw classifyAsRuntime(e);
            }

            // 2) بناء الـpool بإعدادات محافظة وثابتة
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);

            // حجم الـpool
            cfg.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.max", "5")));
            cfg.setMinimumIdle(Integer.parseInt(p.getProperty("db.pool.min", "1"))); // ← اتصال واحد جاهز دائمًا

            // مهَل أعلى قليلًا
            cfg.setConnectionTimeout(Long.parseLong(p.getProperty("db.conn.timeout.ms", "15000"))); // ← 15s
            cfg.setValidationTimeout(Long.parseLong(p.getProperty("db.validation.timeout.ms", "3000"))); // ← 3s
            cfg.setIdleTimeout(Long.parseLong(p.getProperty("db.idle.timeout.ms",
                    String.valueOf(Duration.ofMinutes(2).toMillis()))));
            cfg.setInitializationFailTimeout(-1); // لا تفشل أثناء الإنشاء

            // اختبار صحة واضح للاتصال
            cfg.setConnectionTestQuery("SELECT 1");
            // إبقِاء الاتصال حيًا لتفادي إغلاق السيرفرات الحصيفة
            cfg.setKeepaliveTime(Duration.ofMinutes(5).toMillis());
            cfg.setMaxLifetime(Duration.ofMinutes(10).toMillis()); // ← 10 دقائق عمر الاتصال

            // (أحيانًا يفيد مع JPMS)
            cfg.setDriverClassName("org.postgresql.Driver");

            // --- Extra PostgreSQL socket hardening (without altering current behavior) ---
            try {
                // Keep TCP alive at OS level (helps during sleep/idle networks)
                cfg.addDataSourceProperty("tcpKeepAlive", "true");
                // Fail faster on broken sockets (seconds)
                cfg.addDataSourceProperty("socketTimeout", "30");
                // Login/connect timeout (seconds)
                cfg.addDataSourceProperty("loginTimeout", "10");
                // Prefer query rewrite for batched inserts (no behavior change if unused)
                cfg.addDataSourceProperty("reWriteBatchedInserts", "true");
            } catch (Throwable ignore) {}

            // Ensure minIdle never exceeds max (in case of external props)
            try {
                if (cfg.getMinimumIdle() > cfg.getMaximumPoolSize()) {
                    cfg.setMinimumIdle(cfg.getMaximumPoolSize());
                }
            } catch (Throwable ignore) {}

            // Optional: explicit pool name helps reading logs without changing behavior
            try { cfg.setPoolName("HikariPool-Main"); } catch (Throwable ignore) {}

            HikariDataSource candidate = new HikariDataSource(cfg);

            // جرّب أخذ اتصال واحد فورًا من الـpool (يفشل الآن إن كان في مشكلة)
            try (Connection c = candidate.getConnection()) {
                // OK
            } catch (SQLException e) {
                candidate.close();
                throw classifyAsRuntime(e);
            }

            ds = candidate;
        }
    }

    private static RuntimeException classifyAsRuntime(SQLException e) {
        String state = e.getSQLState();
        String msg   = e.getMessage();

        if (state != null && state.startsWith("08")) {
            return new DbUnavailableException(Status.OFFLINE_NETWORK,
                    "Connection exception (" + state + "): " + msg, e);
        }
        if (state != null && state.startsWith("28")) {
            return new DbUnavailableException(Status.BAD_CREDENTIALS,
                    "Authentication failed (" + state + "): " + msg, e);
        }
        if ("57P03".equals(state)) {
            return new DbUnavailableException(Status.OFFLINE_NETWORK,
                    "Server not ready (" + state + "): " + msg, e);
        }
        if ("3D000".equals(state)) {
            return new DbUnavailableException(Status.UNKNOWN_ERROR,
                    "Database does not exist (" + state + "): " + msg, e);
        }
        if ("53300".equals(state)) {
            return new DbUnavailableException(Status.UNKNOWN_ERROR,
                    "Too many connections (" + state + "): " + msg, e);
        }
        return new DbUnavailableException(Status.UNKNOWN_ERROR,
                "Unclassified DB error (" + (state == null ? "no-sqlstate" : state) + "): " + msg, e);
    }

    /** فحص سريع بدون Pool */
    public static Status ping() {
        try {
            Properties p = loadProps();
            String url  = p.getProperty("db.url");
            String user = p.getProperty("db.user");
            String pass = p.getProperty("db.password");
            if (url == null || user == null) return Status.CONFIG_MISSING;

            url = normalizeJdbcUrl(url);

            DriverManager.setLoginTimeout(7);
            Properties jdbcProps = new Properties();
            jdbcProps.setProperty("user", user);
            if (pass != null) jdbcProps.setProperty("password", pass);

            try (Connection c = DriverManager.getConnection(url, jdbcProps)) {
                return Status.ONLINE;
            }
        } catch (SQLException e) {
            RuntimeException re = classifyAsRuntime(e);
            if (re instanceof DbUnavailableException dbe) return dbe.status;
            return Status.UNKNOWN_ERROR;
        } catch (RuntimeException re) {
            if (re instanceof DbUnavailableException dbe) return dbe.status;
            return Status.UNKNOWN_ERROR;
        }
    }

    public static void reinit() { shutdown(); }

    public static DataSource getDataSource() {
        ensureInit();
        return ds;
    }

    public static Connection get() throws SQLException {
        try {
            ensureInit();
            return ds.getConnection();
        } catch (DbUnavailableException ex) {
            throw new SQLException("DB unavailable: " + ex.status + " — " + ex.getMessage(), ex);
        }
    }

    public static void shutdown() {
        HikariDataSource local = ds;
        ds = null;
        if (local != null && !local.isClosed()) {
            local.close();
        }
    }

    public static class DbUnavailableException extends RuntimeException {
        public final Status status;
        public DbUnavailableException(Status s, String msg, Throwable cause) {
            super(msg, cause);
            this.status = s;
        }
    }
}