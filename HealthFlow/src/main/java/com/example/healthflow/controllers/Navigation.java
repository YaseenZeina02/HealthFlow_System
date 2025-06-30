package com.example.healthflow.controllers;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Navigation {
    public final String Login_Fxml = "Login.fxml";
    /*
    public final String Doctor_Fxml = "Doctor.fxml";
    public final String Reception_Fxml = "Reception.fxml";
*/
    public void navigateToSameStage(Parent currentRoot, String fxmlPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            currentRoot.getScene().setRoot(newRoot);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void navigateTo(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("New Window");

            // جعل النافذة قابلة للتغيير الحجم إذا كانت بحاجة
            stage.setResizable(true);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
