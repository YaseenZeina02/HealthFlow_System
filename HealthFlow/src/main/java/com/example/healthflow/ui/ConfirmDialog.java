package com.example.healthflow.ui;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class ConfirmDialog {

    // النسخة القديمة (احتفظ فيها)
    public static boolean show(String title, String message, String iconPath) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ✅ النسخة الجديدة — تربط النافذة (Stage) بالـ Alert
    public static boolean show(Stage owner, String title, String message, String iconPath) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (owner != null) {
            alert.initOwner(owner); // تربط الـ dialog بنفس نافذة البرنامج
            alert.initModality(Modality.WINDOW_MODAL);
        }

        // (اختياري) لو بدك تضيف أيقونة مخصصة:
        // if (iconPath != null) {
        //     Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        //     stage.getIcons().add(new Image(iconPath));
        // }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}