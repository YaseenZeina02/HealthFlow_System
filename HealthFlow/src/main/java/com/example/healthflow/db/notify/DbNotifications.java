package com.example.healthflow.db.notify;

import com.example.healthflow.db.Database;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.*;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Durable LISTEN/NOTIFY helper:
 * - Dedicated connection kept open
 * - LISTEN multiple channels
 * - Periodic keep-alive (SELECT 1) to avoid serverless idle timeouts
 * - Auto-reconnect on EOF / network errors
 */
public final class DbNotifications implements AutoCloseable {

    private final Map<String, Consumer<String>> handlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pg-listen");
                t.setDaemon(true);
                return t;
            });

    private volatile Connection conn;        // dedicated connection
    private volatile PGConnection pg;
    private volatile boolean running;

    /** ابدأ الاستماع لقناة معينة. يمكن استدعاؤها عدة مرات لقنوات مختلفة. */
    public synchronized void listen(String channel, Consumer<String> onPayload) {
        Objects.requireNonNull(channel, "channel");
        handlers.put(channel, onPayload);
        ensureConnected();
        exec.execute(() -> doListen(channel));           // نفّذ LISTEN للقناة الآن
    }

    /** تأكد من وجود اتصال حي، وإلا أعد الاتصال وأطلق مهام القراءة والكيبالايف. */
    private synchronized void ensureConnected() {
        if (running && conn != null) return;
        reconnectLoop();
        // polling خفيف لقراءة الإشعارات + keep-alive؛ تكفي كل 20-25 ثانية على Neon
        exec.scheduleWithFixedDelay(this::pumpNotificationsSafe, 0, 25, TimeUnit.SECONDS);
    }

    /** نفّذ أمر LISTEN للقناة المعطاة. */
    private void doListen(String channel) {
        try (Statement st = conn.createStatement()) {
            st.execute("LISTEN " + channel);
        } catch (SQLException e) {
            // لو فشل لأن الاتصال انقطع، أعد الاتصال وجرّب مرة أخرى
            reconnectLoop();
            try (Statement st = conn.createStatement()) {
                st.execute("LISTEN " + channel);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /** حاول إنشاء اتصال مخصص وإعداده، مع إعادة المحاولة عند الفشل. */
    private void reconnectLoop() {
        closeSilently();
        while (true) {
            try {
                conn = Database.get();                 // خذ اتصال من الـ pool واستخدمه مخصصًا لهذا الكلاس
                conn.setAutoCommit(true);              // ضروري لـ NOTIFY
                // (اختياري) سهّل تتبع الاتصال في لوحة Neon
                try (Statement s = conn.createStatement()) {
                    s.execute("SET application_name = 'HealthFlow-Listener'");
                } catch (SQLException ignore) {}

                pg = conn.unwrap(PGConnection.class);
                running = true;
                System.out.println("[DbNotifications] connected");
                // أعد LISTEN لكل القنوات المسجلة سابقًا
                for (String ch : handlers.keySet()) doListen(ch);
                break;
            } catch (SQLException e) {
                System.err.println("[DbNotifications] reconnect failed: " + e.getMessage());
                sleep(Duration.ofSeconds(3));
            }
        }
    }

    /** اقرأ الإشعارات + أبقِ الاتصال حيًا عبر SELECT 1. */
    private void pumpNotificationsSafe() {
        if (!running || conn == null) return;
        try (Statement st = conn.createStatement()) {
            // keep-alive خفيف يمنع غلق الاتصال للخمول
            st.execute("SELECT 1");
            // اسحب أي إشعارات متراكمة
            PGNotification[] ns = pg.getNotifications();
            if (ns != null) {
                for (PGNotification n : ns) {
                    Consumer<String> h = handlers.get(n.getName());
                    if (h != null) {
                        try { h.accept(n.getParameter()); } catch (Throwable ignore) {}
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DbNotifications] pump error: " + e.getMessage());
            reconnectLoop(); // أي EOF/IO → أعد الاتصال
        }
    }

    private static void sleep(Duration d) {
        try { Thread.sleep(d.toMillis()); } catch (InterruptedException ignored) {}
    }

    private synchronized void closeSilently() {
        running = false;
        if (conn != null) {
            try { conn.close(); } catch (Exception ignore) {}
        }
        conn = null; pg = null;
    }

    @Override public synchronized void close() {
        exec.shutdownNow();
        closeSilently();
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
//import java.util.concurrent.*;
//import java.util.function.Consumer;
//
//public class DbNotifications implements AutoCloseable {
//    private final Map<String, Consumer<String>> handlers = new ConcurrentHashMap<>();
//    private final ScheduledExecutorService exec =
//            Executors.newSingleThreadScheduledExecutor(r -> {
//                Thread t = new Thread(r, "pg-listen"); t.setDaemon(true); return t;
//            });
//
//    private volatile Connection conn;
//    private volatile PGConnection pg;
//    private volatile boolean running = false;
//
//    /** سجّل مستمع لقناة */
//    public void listen(String channel, Consumer<String> handler) {
//        handlers.put(channel, handler);
//        exec.execute(() -> {
//            ensureConnected();
//            try (Statement st = conn.createStatement()) {
//                st.execute("LISTEN " + channel);
//            } catch (SQLException e) { e.printStackTrace(); }
//        });
//    }
//
//    private void ensureConnected() {
//        if (running && conn != null) return;
//        reconnect();
//        // حلقة polling خفيفة للحصول على الإشعارات ولـ keep-alive
//        exec.scheduleWithFixedDelay(this::poll, 0, 25, TimeUnit.SECONDS);
//    }
//
//    private void reconnect() {
//        closeSilently();
//        while (true) {
//            try {
//                conn = Database.get();                 // خذ اتصال مخصص واتركه مفتوح
//                conn.setAutoCommit(true);              // مهم لـ NOTIFY
//                pg = conn.unwrap(PGConnection.class);  // PGConnection
//
//
//                // Listener من سائق PG: يصل عند وصول إشعار
////                pg.addNotificationListener((pid, channel, payload) -> {
//
//                    pg.getNotifications((pid, channel, payload) -> {
//                    Consumer<String> h = handlers.get(channel);
//                    if (h != null) {
//                        try { h.accept(payload); } catch (Throwable ignore) {}
//                    }
//                });
//
//                running = true;
//                System.out.println("[DbNotifications] connected & listening");
//                break;
//            } catch (SQLException e) {
//                System.err.println("[DbNotifications] reconnect failed: " + e.getMessage());
//                sleep(Duration.ofSeconds(3));
//            }
//        }
//    }
//
//    /** poll خفيف: يسمح للسائق بقراءة الباك إند + keep-alive للسيرفرات السيرفرلس */
//    private void poll() {
//        if (!running) return;
//        try (Statement st = conn.createStatement()) {
//            // استدعاء خفيف يُحرِّك السائق لقراءة القناة
//            st.execute("SELECT 1");
//            // optional: لو بدك تسحب إشعارات بطريقة synchronous
//            PGNotification[] arr = pg.getNotifications();
//            if (arr != null) {
//                for (PGNotification n : arr) {
//                    Consumer<String> h = handlers.get(n.getName());
//                    if (h != null) h.accept(n.getParameter());
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("[DbNotifications] poll error: " + e.getMessage());
//            reconnect(); // حاول إعادة الاتصال
//        }
//    }
//
//    private static void sleep(Duration d) {
//        try { Thread.sleep(d.toMillis()); } catch (InterruptedException ignored) {}
//    }
//
//    private void closeSilently() {
//        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
//        conn = null; pg = null; running = false;
//    }
//
//    @Override public void close() { exec.shutdownNow(); closeSilently(); }
//}
//---------------------

//package com.example.healthflow.db.notify;
//
//import com.example.healthflow.db.Database;
//import org.postgresql.PGConnection;
//import org.postgresql.PGNotification;
//
//import java.sql.Connection;
//import java.sql.Statement;
//import java.util.function.Consumer;
//
///**
// * Simple LISTEN/NOTIFY wrapper. One instance per channel.
// * Remember to close() on shutdown.
// */
//public final class DbNotifications implements AutoCloseable {
//    private volatile boolean stop = false;
//    private Thread thread;
//
//    public void listen(String channel, Consumer<String> onPayload) {
//        thread = new Thread(() -> {
//            try (Connection c = Database.get(); Statement st = c.createStatement()) {
//                st.execute("LISTEN " + channel);
//                PGConnection pg = c.unwrap(PGConnection.class);
//                while (!stop) {
//                    PGNotification[] ns = pg.getNotifications(5000); // 5s timeout
//                    if (ns == null) continue;
//                    for (PGNotification n : ns) {
//                        if (onPayload != null) onPayload.accept(n.getParameter());
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, "db-listen-" + channel);
//        thread.setDaemon(true);
//        thread.start();
//    }
//
//    @Override public void close() {
//        stop = true;
//        if (thread != null) thread.interrupt();
//    }
//}