package com.example.healthflow.controllers;
import com.example.healthflow.net.ConnectivityMonitor;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class Navigation {
    public final String Login_Fxml = "/com/example/healthflow/views/Login.fxml";
    public final String Reception_Fxml = "/com/example/healthflow/views/Reception.fxml";
    public final String Admin_Fxml = "/com/example/healthflow/views/Admin.fxml";
    public final String Doctor_Fxml = "/com/example/healthflow/views/Doctor.fxml";
    public final String Pharmacy_Fxml = "/com/example/healthflow/views/pharmacy.fxml";

    public void navigateToSameStage(Parent currentRoot, String fxmlPath, ConnectivityMonitor monitor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Set controller factory to pass the monitor
            loader.setControllerFactory(type -> {
                if (type == LoginController.class) {
                    return new LoginController(monitor);
                } else if (type == ReceptionController.class) {
                    return new ReceptionController(monitor);
                } else if (type == AdminController.class) {
                    return new AdminController(monitor);
                } else if (type == DoctorController.class) {
                    return new DoctorController(monitor);
                } else if (type == PharmacyController.class) {
                    return new PharmacyController(monitor);
                } else if (type == ParientController.class) {
                    return new ParientController(monitor);
                } else if (type == DrugWarehouse.class) {
                    return new DrugWarehouse(monitor);
                } else {
                    return null;
                }
            });

            Parent newRoot = loader.load();
            currentRoot.getScene().setRoot(newRoot);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // For backward compatibility
    public void navigateToSameStage(Parent currentRoot, String fxmlPath) {
        navigateToSameStage(currentRoot, fxmlPath, new ConnectivityMonitor());
    }

    public void navigateTo(Stage stage, String fxmlPath, ConnectivityMonitor monitor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Set controller factory to pass the monitor
            loader.setControllerFactory(type -> {
                if (type == LoginController.class) {
                    return new LoginController(monitor);
                } else if (type == ReceptionController.class) {
                    return new ReceptionController(monitor);
                } else if (type == AdminController.class) {
                    return new AdminController(monitor);
                } else if (type == DoctorController.class) {
                    return new DoctorController(monitor);
                } else if (type == PharmacyController.class) {
                    return new PharmacyController(monitor);
                } else if (type == ParientController.class) {
                    return new ParientController(monitor);
                } else if (type == DrugWarehouse.class) {
                    return new DrugWarehouse(monitor);
                } else {
                    return null;
                }
            });

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

    // For backward compatibility
    public void navigateTo(Stage stage, String fxmlPath) {
        navigateTo(stage, fxmlPath, new ConnectivityMonitor());
    }


}
