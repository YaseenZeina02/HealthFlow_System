package com.example.healthflow;

import com.example.healthflow.controllers.Navigation;
import com.example.healthflow.db.Database;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App extends Application {

    private final ConnectivityMonitor monitor = new ConnectivityMonitor(
            "https://www.google.com/generate_204",   // لاحقًا استبدله بـ /health تبع سيرفرك
            Duration.ofSeconds(5),
            Duration.ofSeconds(2)
    );
    // لإدارة مهمة الـ warm-up
    // لإدارة مهمة الـ warm-up
    private ExecutorService warmupExec;
    private Future<?> warmupTask;

    @Override
    public void start(Stage stage) throws Exception {
        // 1) شغّل مراقبة الاتصال
        monitor.start();
        monitor.checkNow();

        // 2) اعمل warm-up للداتابيز في Executor دايمون، وبنقدر نلغيه عند الإغلاق
        warmupExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "db-warmup");
            t.setDaemon(true);            // لو النافذة اتسكرت، ما يمنع JVM من الخروج
            return t;
        });
        warmupTask = warmupExec.submit(() -> {
            try (Connection c = Database.get()) {
                if (Thread.currentThread().isInterrupted()) return;
                System.out.println("Database warm-up connection successful!");
            } catch (Exception e) {
                System.err.println("Database warm-up failed: " + e.getMessage());
            }
        });

        // 3) حمّل شاشة الـ Login ومرّر الـ monitor للكنترولر
        FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
        loader.setControllerFactory(type -> {
            try {
                if (type == com.example.healthflow.controllers.LoginController.class) {
                    return new com.example.healthflow.controllers.LoginController(monitor);
                }
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Parent loginRoot = loader.load();
        var controller = (com.example.healthflow.controllers.LoginController) loader.getController();

        monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
            if (isOnline) controller.onBecameOnline();
        });

        var banner = new ConnectivityBanner(monitor);
        var root = new BorderPane();
        root.setTop(banner);
        root.setCenter(new StackPane(loginRoot));

        var scene = new Scene(root, 900, 600);
        stage.setTitle("HealthFlow");
        stage.setScene(scene);
        stage.show();

        // عند إغلاق النافذة: أوقف المونيتور + الغِ مهمة الـ warm-up فورًا
        stage.setOnCloseRequest(e -> {
            cleanupAsyncStuff();
        });
    }

    @Override
    public void stop() {
        // لو التطبيق اتقفل من غير onCloseRequest (مثلاً من النظام)، نضمن التنظيف
        cleanupAsyncStuff();
    }

    private void cleanupAsyncStuff() {
        try {
            monitor.stop();
        } catch (Throwable ignored) {}

        if (warmupTask != null) {
            warmupTask.cancel(true);   // يطلب إيقاف المهمة لو لسه شغالة
        }
        if (warmupExec != null) {
            warmupExec.shutdownNow();  // يمنع أي مهام جديدة ويحاول يوقف الحالية
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}