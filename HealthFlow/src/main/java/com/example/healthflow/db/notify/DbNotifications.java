package com.example.healthflow.db.notify;

import org.postgresql.PGConnection;

import java.sql.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import java.io.InputStream;
import java.util.Properties;

/**
 * Robust PostgreSQL LISTEN/NOTIFY helper.
 * - Dedicated DriverManager connection (NOT from pool)
 * - autocommit=true (required for async notifications)
 * - Background loop wakes the connection and dispatches notifications
 * - Auto-reconnect with backoff
 */
public final class DbNotifications implements AutoCloseable {
    // Load DB props from (priority): JVM props -> ENV -> application.properties (on classpath)
    private static final Properties PROPS = new Properties();
    static {
        try (InputStream in = DbNotifications.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) PROPS.load(in);
        } catch (Exception ignore) { }
    }

    private static String prop(String key, String envKey, String defVal) {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;
        v = PROPS.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        return defVal;
    }
    private final ConcurrentMap<String, Consumer<String>> handlers = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<String> channels = new CopyOnWriteArraySet<>();

    private volatile Connection    conn;   // dedicated connection
    private volatile PGConnection  pgConn; // PG extension
    private final ExecutorService  loopExec;
    private final AtomicBoolean    running = new AtomicBoolean(false);

    private static String pgUrl()      { return prop("db.url",      "DB_URL",      "jdbc:postgresql://localhost:5432/healthflow"); }
    private static String pgUser()     { return prop("db.user",     "DB_USER",     "postgres"); }
    private static String pgPassword() { return prop("db.password", "DB_PASSWORD", "postgres"); }

    public DbNotifications() {
        this.loopExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pg-listen-loop");
            t.setDaemon(true);
            return t;
        });
        startLoop();
    }

    /** Register handler and ensure LISTEN (now or on reconnect). */
    public void listen(String channel, Consumer<String> handler) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        handlers.put(channel, handler);
        channels.add(channel);
        ensureListen(channel);
    }

    private void startLoop() {
        running.set(true);
        loopExec.submit(this::loop);
    }

    private void connect() throws SQLException {
        System.out.println("[DbNotifications] connecting to " + pgUrl() + " as " + pgUser());
        closeConnQuietly();
        conn = DriverManager.getConnection(pgUrl(), pgUser(), pgPassword()); // NOT from pool
        conn.setAutoCommit(true);
        pgConn = conn.unwrap(PGConnection.class);

        try (Statement st = conn.createStatement()) {
            for (String ch : channels) {
                try { st.execute(
                        "LISTEN " + ch);
                    System.out.println("[DbNotifications] LISTEN " + ch);

                }
                catch (SQLException e) { System.err.println("[DbNotifications] LISTEN failed for " + ch + " : " + e); }
            }
            // Proactive resync after (re)connect
            try {
                handlers.forEach((ch, h) -> {
                    try { h.accept("resync"); } catch (Throwable t) {
                        System.err.println("[DbNotifications] resync handler error on channel " + ch + ": " + t);
                    }
                });
            } catch (Throwable ignore) { }
        }
        System.out.println("[DbNotifications] connected (dedicated)");
    }

    private void ensureListen(String channel) {
        if (conn == null) return; // will LISTEN after connect()
        try (Statement st = conn.createStatement()) { st.execute("LISTEN " + channel); }
        catch (SQLException e) { System.err.println("[DbNotifications] ensureListen " + channel + " : " + e); }
    }

    private void loop() {
        long backoffMs = 500, maxBackoff = 10_000;
        while (running.get()) {
            try {
                if (conn == null || conn.isClosed()) {
                    connect();
                    backoffMs = 500; // reset
                }
                // Wake the socket so notifications get delivered
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) { /* no-op */ }


                var notifications = pgConn.getNotifications();

                if (notifications != null && notifications.length > 0)
                    System.out.println("[DbNotifications] received " + notifications.length + " notification(s)");

                if (notifications != null) {
                    for (var n : notifications) {
                        var h = handlers.get(n.getName());
                        if (h != null) {
                            try {
                                System.out.println("[DbNotifications] NOTIFY " + n.getName() + " payload=" + n.getParameter());
                                h.accept(n.getParameter());
                            }
                            catch (Throwable t) { System.err.println("[DbNotifications] handler error: " + t); }
                        }
                    }
                }
                Thread.sleep(300);
            } catch (SQLException se) {
                System.err.println("[DbNotifications] loop sql error: " + se);
                closeConnQuietly();
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                backoffMs = Math.min(maxBackoff, backoffMs * 2);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                System.err.println("[DbNotifications] loop error: " + t);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }

        closeConnQuietly();
    }

    private void closeConnQuietly() {
        try { if (conn != null) conn.close(); } catch (Exception ignore) {}
        conn = null; pgConn = null;
    }

    @Override public void close() {
        running.set(false);
        loopExec.shutdownNow();
        closeConnQuietly();
    }
}