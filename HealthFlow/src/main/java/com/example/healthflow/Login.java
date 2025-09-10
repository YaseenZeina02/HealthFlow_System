package com.example.healthflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Login extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // تحميل واجهة تسجيل الدخول
        FXMLLoader fxmlLoader = new FXMLLoader(Login.class.getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        // تعيين العنوان لواجهة تسجيل الدخول
        stage.setTitle("HealthFlow");

        // منع تغيير حجم نافذة تسجيل الدخول
        stage.setResizable(false);  // منع تكبير نافذة تسجيل الدخول

        // تعيين المشهد وتفعيل نافذة تسجيل الدخول
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }

}
