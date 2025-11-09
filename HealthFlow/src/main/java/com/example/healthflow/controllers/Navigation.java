package com.example.healthflow.controllers;

import com.example.healthflow.net.ConnectivityMonitor;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Navigation helper:
 * - يحاول يمرّر ConnectivityMonitor لو الكلاس عنده constructor مناسب.
 * - لو ما فيه، يرجع للـ no-arg constructor بدون ما نغيّر أي كلاس آخر.
 */

public class Navigation {

    public final String Login_Fxml     = "/com/example/healthflow/views/Login.fxml";
    public final String Reception_Fxml = "/com/example/healthflow/views/Reception.fxml";
    public final String Admin_Fxml     = "/com/example/healthflow/views/Admin.fxml";
    public final String Doctor_Fxml    = "/com/example/healthflow/views/Doctor.fxml";
    public final String Pharmacy_Fxml  = "/com/example/healthflow/views/pharmacy.fxml";

    // Fixed Login size (agreed) – tweaked narrower
    private static final double LOGIN_W = 1000;
    private static final double LOGIN_H = 600;

    /* ===================== Helpers ===================== */
    public static void maximize(Stage stage) {
        stage.setResizable(true);
        stage.setMaximized(true);
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        stage.setX(b.getMinX());
        stage.setY(b.getMinY());
        stage.setWidth(b.getWidth());
        stage.setHeight(b.getHeight());
    }

    /* ===================== Controller Factory (مرن) ===================== */
    private Object createController(Class<?> type, ConnectivityMonitor monitor) {
        try {
            // 1) جرّب constructor(ConnectivityMonitor)
            try {
                var ctor = type.getConstructor(ConnectivityMonitor.class);
                ctor.setAccessible(true);
                return ctor.newInstance(monitor);
            } catch (NoSuchMethodException ignore) {
                // 2) fallback: no-arg constructor
                var noArg = type.getDeclaredConstructor();
                noArg.setAccessible(true);
                return noArg.newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create controller: " + type.getName(), e);
        }
    }

    /* ===================== نفس الستيج ===================== */
    public void navigateToSameStage(Parent currentRoot, String fxmlPath, ConnectivityMonitor monitor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Factory عامة: ما بدها if/else لكل كلاس
            loader.setControllerFactory(type -> createController(type, monitor));

            Parent newRoot = loader.load();
            currentRoot.getScene().setRoot(newRoot);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Backward compat
    public void navigateToSameStage(Parent currentRoot, String fxmlPath) {
        navigateToSameStage(currentRoot, fxmlPath, new ConnectivityMonitor());
    }

    /* ===================== Login (fixed-size window) ===================== */
    public void showLoginFixed(Stage reuse, ConnectivityMonitor monitor) {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource(Login_Fxml));
            fx.setControllerFactory(type -> {
                try {
                    if (type == com.example.healthflow.controllers.LoginController.class) {
                        return new com.example.healthflow.controllers.LoginController(monitor);
                    }
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent root = fx.load();

            // اعادة استخدام نفس الـStage إن وُجد، وإلا أنشئ جديد
            Stage stage = (reuse != null) ? reuse : new Stage();

            // أوقف أي سلوك سابق قد يغيّر الحجم
            stage.setMaximized(false);
            stage.setFullScreen(false);
            stage.setResizable(false);

            // ثبّت الحجم القياسي المتفق عليه
            final double W = 900, H = 600;
            stage.setMinWidth(W);
            stage.setMinHeight(H);
            stage.setWidth(W);
            stage.setHeight(H);

            // ضع المشهد ثم اطلب ضبط الحجم على محتوى الـFXML (لن يزيد عن القيم أعلاه)
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.sizeToScene();

            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Overload without monitor
    public void showLoginFixed(Stage oldStage) {
        showLoginFixed(oldStage, new ConnectivityMonitor());
    }

    /* ===================== نافذة جديدة ===================== */
    public FXMLLoader navigateTo(Stage stage, String fxmlPath, ConnectivityMonitor monitor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(type -> createController(type, monitor));

            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("New Window");
            // Fill screen by default for post-login windows
            maximize(stage);
            stage.setResizable(true);
            stage.show();
            return loader;
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
        }
        return null;
    }

    // Backward compat
    public void navigateTo(Stage stage, String fxmlPath) {
        navigateTo(stage, fxmlPath, new ConnectivityMonitor());
    }

}
