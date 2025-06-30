package com.example.healthflow.controllers;
import com.example.healthflow.controllers.Navigation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Map;

public class LoginController {
    // عناصر FXML المربوطة من الواجهة:
    @FXML private TextField UserNameTextField;
    // حقل إدخال كلمة المرور
    @FXML private PasswordField PasswordTextField;

    @FXML private CheckBox ShowPasswordCheckBox;
    // لوحة أساسية لواجهة تسجيل الدخول
    @FXML private AnchorPane rootPane;
    // كائن للتنقل بين الواجهات
    private final Navigation navigation = new Navigation();
    // حقل نصي بديل لإظهار كلمة المرور (عند تحديد "Show Password")
    private TextField visiblePasswordField = new TextField();

    /**
     * تتم تهيئة عناصر الشاشة عند تحميلها
     * - يتم إعداد TextField مرئي بديل لـ PasswordField لعرض كلمة المرور عند الطلب
     * - يتم ربط CheckBox لعرض/إخفاء كلمة المرور
     */
    @FXML
    private void initialize() {
        // ضبط خصائص TextField البديل المطابق لـ PasswordField
        visiblePasswordField.setLayoutX(PasswordTextField.getLayoutX());
        visiblePasswordField.setLayoutY(PasswordTextField.getLayoutY());
        visiblePasswordField.setPrefWidth(PasswordTextField.getPrefWidth());
        visiblePasswordField.setPrefHeight(PasswordTextField.getPrefHeight());
        visiblePasswordField.setFont(PasswordTextField.getFont());
        visiblePasswordField.setStyle(PasswordTextField.getStyle());
        visiblePasswordField.setPromptText(PasswordTextField.getPromptText());

        // إضافته إلى نفس الحاوية
        Pane parent = (Pane) PasswordTextField.getParent();
        parent.getChildren().add(visiblePasswordField);

        // التأكد من أن الحقل النصي المرئي مخفي بشكل افتراضي
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        // ربط حدث عرض/إخفاء كلمة المرور
        // ربط CheckBox لتبديل الرؤية بين الحقلين
        ShowPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                // عندما يتم تحديد خيار إظهار كلمة المرور
                visiblePasswordField.setText(PasswordTextField.getText());  // نسخ النص من PasswordTextField إلى visiblePasswordField
                visiblePasswordField.setVisible(true);   // إظهار النص في visiblePasswordField
                visiblePasswordField.setManaged(true);   // جعل الحقل مرئي في الواجهة
                PasswordTextField.setVisible(false);     // إخفاء PasswordTextField
                PasswordTextField.setManaged(false);    // جعله غير مرئي
            } else {
                // عندما يتم إلغاء تحديد خيار إظهار كلمة المرور
                PasswordTextField.setText(visiblePasswordField.getText()); // نسخ النص من visiblePasswordField إلى PasswordTextField
                PasswordTextField.setVisible(true);  // إظهار الحقل الأصلي
                PasswordTextField.setManaged(true); // جعله مرئيًا في الواجهة
                visiblePasswordField.setVisible(false); // إخفاء visiblePasswordField
                visiblePasswordField.setManaged(false); // جعله غير مرئي
            }
        });
    }

    /**
     التحقق من صحة بيانات المستخدم (حل مؤقت قبل ربط قاعدة البيانات)
     *
     * @param username اسم المستخدم المدخل
     * @param password كلمة المرور المدخلة
     * @return true إذا كانت البيانات صحيحة، false إذا كانت غير صحيحة
     */
    private boolean isValidUser(String username, String password) {
        Map<String, String> users = Map.of(
                "Admin", "123",
                "Recep", "123",
                "Doctor", "123",
                "Pharmacist", "123"
        );
        return users.containsKey(username) && users.get(username).equals(password);
    }
    /**
     * عرض رسالة تحذير للمستخدم
     *
     * @param title عنوان الرسالة
     * @param message محتوى الرسالة
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    /**
     * معالجة حدث النقر على زر تسجيل الدخول
     * - التحقق من إدخال البيانات المطلوبة
     * - التحقق من صحة بيانات الدخول
     * - توجيه المستخدم حسب دوره عند نجاح التسجيل
     */
    @FXML
    public void LoginAction() {
        try {
            // الحصول على البيانات المدخلة
            String username = UserNameTextField.getText();
            String password = ShowPasswordCheckBox.isSelected() ? visiblePasswordField.getText() : PasswordTextField.getText();

            // التحقق من إدخال البيانات المطلوبة
            if (username.isEmpty()) {
                showAlert("Error", "Username is required.");
                return;
            } else if (password.isEmpty()) {
                showAlert("Error", "Password is required.");
                return;
            }

            // التحقق من صحة بيانات الدخول
            if (isValidUser(username, password)) {
                Stage currentStage = (Stage) rootPane.getScene().getWindow();  // الحصول على الـ Stage الحالي
                currentStage.close();  // إغلاق نافذة تسجيل الدخول الحالية
                switch (username) {
                    case "Recep":
                      //  navigation.navigateTo(new Stage(), navigation.Reception_Fxml);
                        break;
                    case "Doctor":
                       // navigation.navigateTo(new Stage(), navigation.Doctor_Fxml);
                        break;
                    case "Pharmacist":
                        // navigation.navigateTo(new Stage(), navigation.Pharmacy_Fxml);
                        break;
                    case "Admin":
                        // navigation.navigateTo(new Stage(), navigation.Admin_Fxml);
                        break;
                }
            } else {
                showAlert("Login Failed", "Invalid credentials");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }



}
