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
        closeConnQuietly();
        conn = DriverManager.getConnection(pgUrl(), pgUser(), pgPassword()); // NOT from pool
        conn.setAutoCommit(true);
        pgConn = conn.unwrap(PGConnection.class);

        try (Statement st = conn.createStatement()) {
            for (String ch : channels) {
                try { st.execute("LISTEN " + ch); }
                catch (SQLException e) { System.err.println("[DbNotifications] LISTEN failed for " + ch + " : " + e); }
            }
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
                if (notifications != null) {
                    for (var n : notifications) {
                        var h = handlers.get(n.getName());
                        if (h != null) {
                            try { h.accept(n.getParameter()); }
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







//package com.example.healthflow.db.notify;
//
//import com.example.healthflow.db.Database;
//import org.postgresql.PGConnection;
//import org.postgresql.PGNotification;
//
//import java.sql.*;
//import java.time.Duration;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.*;
//import java.util.function.Consumer;
//
///**
// * Durable LISTEN/NOTIFY helper:
// * - Dedicated connection kept open
// * - LISTEN multiple channels
// * - Periodic keep-alive (SELECT 1) to avoid serverless idle timeouts
// * - Auto-reconnect on EOF / network errors
// */
//public final class DbNotifications implements AutoCloseable {
//
//    private final Map<String, Consumer<String>> handlers = new ConcurrentHashMap<>();
//    private final ScheduledExecutorService exec =
//            Executors.newSingleThreadScheduledExecutor(r -> {
//                Thread t = new Thread(r, "pg-listen");
//                t.setDaemon(true);
//                return t;
//            });
//
//    private volatile Connection conn;        // dedicated connection
//    private volatile PGConnection pg;
//    private volatile boolean running;
//
//    /** ابدأ الاستماع لقناة معينة. يمكن استدعاؤها عدة مرات لقنوات مختلفة. */
//    public synchronized void listen(String channel, Consumer<String> onPayload) {
//        Objects.requireNonNull(channel, "channel");
//        handlers.put(channel, onPayload);
//        ensureConnected();
//        exec.execute(() -> doListen(channel));           // نفّذ LISTEN للقناة الآن
//    }
//
//    /** تأكد من وجود اتصال حي، وإلا أعد الاتصال وأطلق مهام القراءة والكيبالايف. */
//    private synchronized void ensureConnected() {
//        if (running && conn != null) return;
//        reconnectLoop();
//        // polling خفيف لقراءة الإشعارات + keep-alive؛ تكفي كل 20-25 ثانية على Neon
//        exec.scheduleWithFixedDelay(this::pumpNotificationsSafe, 0, 25, TimeUnit.SECONDS);
//    }
//
//    /** نفّذ أمر LISTEN للقناة المعطاة. */
//    private void doListen(String channel) {
//        try (Statement st = conn.createStatement()) {
//            st.execute("LISTEN " + channel);
//        } catch (SQLException e) {
//            // لو فشل لأن الاتصال انقطع، أعد الاتصال وجرّب مرة أخرى
//            reconnectLoop();
//            try (Statement st = conn.createStatement()) {
//                st.execute("LISTEN " + channel);
//            } catch (SQLException ex) {
//                ex.printStackTrace();
//            }
//        }
//    }
//
//    /** حاول إنشاء اتصال مخصص وإعداده، مع إعادة المحاولة عند الفشل. */
//    private void reconnectLoop() {
//        closeSilently();
//        while (true) {
//            try {
//                conn = Database.get();                 // خذ اتصال من الـ pool واستخدمه مخصصًا لهذا الكلاس
//                conn.setAutoCommit(true);              // ضروري لـ NOTIFY
//                // (اختياري) سهّل تتبع الاتصال في لوحة Neon
//                try (Statement s = conn.createStatement()) {
//                    s.execute("SET application_name = 'HealthFlow-Listener'");
//                } catch (SQLException ignore) {}
//
//                pg = conn.unwrap(PGConnection.class);
//                running = true;
//                System.out.println("[DbNotifications] connected");
//                // أعد LISTEN لكل القنوات المسجلة سابقًا
//                for (String ch : handlers.keySet()) doListen(ch);
//                break;
//            } catch (SQLException e) {
//                System.err.println("[DbNotifications] reconnect failed: " + e.getMessage());
//                sleep(Duration.ofSeconds(3));
//            }
//        }
//    }
//
//    /** اقرأ الإشعارات + أبقِ الاتصال حيًا عبر SELECT 1. */
//    private void pumpNotificationsSafe() {
//        if (!running || conn == null) return;
//        try (Statement st = conn.createStatement()) {
//            // keep-alive خفيف يمنع غلق الاتصال للخمول
//            st.execute("SELECT 1");
//            // اسحب أي إشعارات متراكمة
//            PGNotification[] ns = pg.getNotifications();
//            if (ns != null) {
//                for (PGNotification n : ns) {
//                    Consumer<String> h = handlers.get(n.getName());
//                    if (h != null) {
//                        try { h.accept(n.getParameter()); } catch (Throwable ignore) {}
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("[DbNotifications] pump error: " + e.getMessage());
//            reconnectLoop(); // أي EOF/IO → أعد الاتصال
//        }
//    }
//
//    private static void sleep(Duration d) {
//        try { Thread.sleep(d.toMillis()); } catch (InterruptedException ignored) {}
//    }
//
//    private synchronized void closeSilently() {
//        running = false;
//        if (conn != null) {
//            try { conn.close(); } catch (Exception ignore) {}
//        }
//        conn = null; pg = null;
//    }
//
//    @Override public synchronized void close() {
//        exec.shutdownNow();
//        closeSilently();
//    }
//}
