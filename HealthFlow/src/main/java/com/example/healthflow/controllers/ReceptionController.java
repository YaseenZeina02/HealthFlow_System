package com.example.healthflow.controllers;
import com.example.healthflow.controllers.Navigation;
import com.example.healthflow.net.ConnectivityMonitor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

public class ReceptionController {

    @FXML private AnchorPane DashboardAnchorPane;

    @FXML private AnchorPane PatientAnchorPane;

    @FXML private AnchorPane AppointmentsAnchorPane;

    @FXML private Button DachboardButton;

    @FXML private Button PatientsButton;

    @FXML private Button AppointmentsButton;

    @FXML private Button BackButton;

    @FXML private TextField PatientIdTextField;

    @FXML private TextField FullNameTextField;

    @FXML private Button InsertButton;
    public enum Gender {
        MALE,
        FEMALE
    }
    @FXML private ComboBox<Gender> GenderComboBox;

    @FXML private DatePicker DateOfBirthPicker;

    @FXML private TextField MobileTextField;

    @FXML private TextField AddressTextField;

    @FXML
    private TextField search;



    private final Navigation navigation = new Navigation();

    @FXML
    private void initialize() {


        // ربط الزر بفتح الواجهة الرئيسية
        DachboardButton.setOnAction(e -> showDashboardPane());

        // ربط الزر بفتح واجهة المرضى
        PatientsButton.setOnAction(e -> showPatientsPane());
/*
        // كل ما يخص واجهة المرضى:
        // إضافة مستمع لحقل رقم الهوية
        PatientIdTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isValidId(newValue)) {
                // إذا كان الرقم غير صحيح، عرض تنبيه للمستخدم
                showAlert("خطأ", "رقم الهوية الوطنية يجب أن يحتوي على 9 أرقام فقط.");
            }
        });
        // إنشاء قائمة من الخيارات باستخدام الـ enum
        GenderComboBox.setValue(Gender.MALE);  // تعيين قيمة افتراضية (ذكر)
        ObservableList<Gender> genderOptions = FXCollections.observableArrayList(Gender.values());
        // تعيين هذه القائمة للكومبوبوكس
        GenderComboBox.setItems(genderOptions);

        // تخصيص طريقة عرض القيم في ComboBox باستخدام StringConverter
        GenderComboBox.setConverter(new StringConverter<Gender>() {
            @Override
            public String toString(Gender gender) {
                return gender.name(); // إرجاع اسم الـ enum (مثل "ذكر" أو "أنثى")
            }

            @Override
            public Gender fromString(String string) {
                return Gender.valueOf(string); // تحويل النص إلى قيمة من الـ enum
            }
        });
        // تعيين قيمة افتراضية إذا رغبت
        DateOfBirthPicker.setValue(LocalDate.of(1990, 1, 1));  // مثلاً تعيين 1 يناير 1990

*/
        // ربط الزر بفتح واجهة المواعيد
        AppointmentsButton.setOnAction(e -> showAppointmentPane());

        BackButton.setOnAction(e -> BackAction());
        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
    }


    private void showDashboardPane() {
        DashboardAnchorPane.setVisible(true);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
    }

    private void showPatientsPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        AppointmentsAnchorPane.setVisible(false);
    }

    private void showAppointmentPane(){
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(true);
    }
    /*
    // التحقق من أن رقم الهوية يحتوي على 9 أرقام فقط
    private boolean isValidId(String id) {
        if (id == null) {
            return false;
        }

        // تحقق إذا كان يحتوي على 9 أرقام فقط
        return id.matches("\\d{9}");
    }
*/
//    @FXML
//    private void BackAction(){
////         الحصول على الـ Stage الحالي من الـ Scene
////        Stage currentStage = (Stage) DashboardAnchorPane.getScene().getWindow();  // الحصول على الـ Stage الحالي
////
////        // إغلاق نافذة الاستقبال الحالية
////        currentStage.close();
////        // فتح نافذة تسجيل الدخول باستخدام نفس الـ Stage
////        // منع تغيير الحجم في Stage تسجيل الدخول
////        Stage loginStage = new Stage(); // Stage جديد لواجهة تسجيل الدخول
////        navigation.navigateTo(loginStage, navigation.Login_Fxml);  // استخدام نفس الـ Stage ولكن فتح نافذة تسجيل الدخول
////        // منع تغيير حجم نافذة تسجيل الدخول
////        loginStage.setResizable(false);
////        loginStage.show();  // عرض نافذة تسجيل الدخول
//        Stage stage = (Stage) BackButton.getScene().getWindow(); // لو عندك المتغير BackButton
//        Navigation navigation = new Navigation();
//        navigation.navigateTo(stage, navigation.Login_Fxml);
//        stage.setResizable(false);
//        // لا داعي لـ stage.show() إذا كان ظاهر أصلاً، لكن لا تضر
//        stage.show();
//
//
//    }

    @FXML
    private void BackAction() {
        Stage stage = (Stage) BackButton.getScene().getWindow();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
            loader.setControllerFactory(type ->
                    type == LoginController.class ? new LoginController(new ConnectivityMonitor()) : null
            );
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

    }

}

