package com.example.healthflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.*;
import java.io.IOException;

public class Login extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try (java.sql.Connection conn = com.example.healthflow.db.Database.get();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT id, full_name, email FROM users LIMIT 5")) {

            System.out.println("✅ Connected to DB: " + conn.getMetaData().getURL());
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("full_name");
                String email = rs.getString("email");
                System.out.printf("User[id=%d, name=%s, email=%s]%n", id, name, email);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Load the login interface
        FXMLLoader fxmlLoader = new FXMLLoader(Login.class.getResource("/com/example/healthflow/views/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        // Set the title for the login interface
        stage.setTitle("HealthFlow");

        // Prevent resizing of the login window
        stage.setResizable(false);

        // Set the scene and show the login window
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}
