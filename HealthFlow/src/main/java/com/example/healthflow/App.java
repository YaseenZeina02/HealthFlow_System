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

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
//بيانات تجريبية صالحة:
//maya.recep@healthflow.com   $2a$10$hash
public class App extends Application {

    private final ConnectivityMonitor monitor = new ConnectivityMonitor(
            "https://www.google.com/generate_204",   // لاحقًا استبدله بـ /health تبع سيرفرك
            Duration.ofSeconds(5),
            Duration.ofSeconds(2)
    );

    @Override
    public void start(Stage stage) throws Exception {
        monitor.start();
        monitor.checkNow();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/healthflow/views/Login.fxml")
        );

        // نمرر الـ monitor للـ LoginController عبر factory
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

        // لما يرجع النت: نفّذ إعادة تحميل في شاشة login
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

//        try (var c = Database.get()) {
//            c.setAutoCommit(false);
//            try {
//                // 1) أنشئ مريض جديد (user+patient)
//                var pdao = new PatientDAO();
//                var patient = pdao.createWithUser(
//                        c,
//                        "Ali Hasan",
//                        null,                               // email غير مطلوب للمريض
//                        BCrypt.hashpw("123456", BCrypt.gensalt()),
//                        "555666777",                        // أو null
//                        "0599000010",
//                        LocalDate.of(2001,3,15),
//                        Gender.MALE,
//                        "Diabetes type 2"
//                );
//
//                // 2) سجل حركة في اللوج
//                new ActivityLogDAO().log(c, null, "CREATE_PATIENT", "patient", patient.getId(), "{\"source\":\"reception\"}");
//
//                c.commit();
//            } catch (Exception ex) {
//                c.rollback();
//                throw ex;
//            } finally {
//                c.setAutoCommit(true);
//            }
//        }
    }
}
