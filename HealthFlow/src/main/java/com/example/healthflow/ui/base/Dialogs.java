package com.example.healthflow.ui.base;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Simple, centralized dialogs utility.
 * Keep UI text minimal here; controllers can localize/compose messages.
 */
public final class Dialogs {
    private Dialogs() {}

    public static void error(String title, Throwable ex) {
        if (ex != null) ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex == null ? null : ex.getMessage());
        a.showAndWait();
    }

    public static void warn(String title, String msg) {
        show(Alert.AlertType.WARNING, title, msg);
    }

    public static void info(String title, String msg) {
        show(Alert.AlertType.INFORMATION, title, msg);
    }

    public static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
