package com.example.healthflow;

import com.example.healthflow.dao.ActivityLogDAO;
import com.example.healthflow.dao.PatientDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.Gender;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;

public class App extends Application {

    private final ConnectivityMonitor monitor = new ConnectivityMonitor(
            "https://www.google.com/generate_204",   // لاحقًا استبدله بـ /health تبع سيرفرك
            Duration.ofSeconds(5),
            Duration.ofSeconds(2)
    );

    @Override
    public void start(Stage stage) throws Exception {
        // 1) شغل مراقبة الاتصال
        monitor.start();
        monitor.checkNow();

        // 2) اعمل warm-up للـ Database في ثريد منفصل
        new Thread(() -> {
            try (Connection c = Database.get()) {
                System.out.println("Database warm-up connection successful!");
            } catch (Exception e) {
                System.err.println("Database warm-up failed: " + e.getMessage());
            }
        }, "db-warmup").start();

        // 3) حمّل شاشة الـ Login
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/healthflow/views/Login.fxml")
        );

        // مرر الـ monitor للـ LoginController
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

        // اربط إعادة التحميل عند رجوع النت
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

        stage.setOnCloseRequest(e -> monitor.stop());
    }

    public static void main(String[] args) throws SQLException {
        launch(args);

    }
}
