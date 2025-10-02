package com.example.healthflow.controllers;

import com.example.healthflow.db.Database;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.OnlineBindings;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public class LoginController {

    private final UserDAO userDao = new com.example.healthflow.dao.UserJdbcDAO();

    // ====== injected from FXML ======
    @FXML private TextField UserNameTextField;
    @FXML private PasswordField PasswordTextField;
    @FXML private CheckBox ShowPasswordCheckBox;
    @FXML private AnchorPane rootPane;
    @FXML private Button LoginButton;

    // لإظهار/إخفاء كلمة السر
    private final TextField visiblePasswordField = new TextField();

    // ====== connectivity ======
    private final ConnectivityMonitor monitor;

    // ====== Auto-retry (اختياري) ======
    private String lastTriedUser;
    private String lastTriedPass;
    private boolean pendingLogin;

    // overlay chip عند الرجوع أونلاين
    private StackPane overlay;

    public LoginController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    @FXML
    private void initialize() {
        // --- show/hide password (UI فقط) ---
        visiblePasswordField.setLayoutX(PasswordTextField.getLayoutX());
        visiblePasswordField.setLayoutY(PasswordTextField.getLayoutY());
        visiblePasswordField.setPrefWidth(PasswordTextField.getPrefWidth());
        visiblePasswordField.setPrefHeight(PasswordTextField.getPrefHeight());
        visiblePasswordField.setFont(PasswordTextField.getFont());
        visiblePasswordField.setStyle(PasswordTextField.getStyle());
        visiblePasswordField.setPromptText(PasswordTextField.getPromptText());

        Pane parent = (Pane) PasswordTextField.getParent();
        parent.getChildren().add(visiblePasswordField);

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        ShowPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                visiblePasswordField.setText(PasswordTextField.getText());
                visiblePasswordField.setVisible(true);
                visiblePasswordField.setManaged(true);
                PasswordTextField.setVisible(false);
                PasswordTextField.setManaged(false);
            } else {
                PasswordTextField.setText(visiblePasswordField.getText());
                PasswordTextField.setVisible(true);
                PasswordTextField.setManaged(true);
                visiblePasswordField.setVisible(false);
                visiblePasswordField.setManaged(false);
            }
        });

        if (monitor != null && LoginButton != null) {
            OnlineBindings.disableWhenOffline(monitor, LoginButton);
        }
    }

    /** تحقّق مرن: يجرّب BCrypt ثم مساواة نصّية (لبيانات قديمة غير مشفرة) */
    private User authenticate(String emailOrUser, String plainPassword) throws Exception {
        String key = (emailOrUser == null) ? null : emailOrUser.trim().toLowerCase();
        User u = userDao.findByEmail(key);       // البريد هو اسم المستخدم
        if (u == null || !u.isActive()) return null;

        String hash = u.getPasswordHash();
        boolean ok = false;

        if (hash != null && hash.startsWith("$2")) { // BCrypt
            ok = BCrypt.checkpw(plainPassword, hash);
        }
        if (!ok) {
            // fallback انتقالياً لو كانت مخزنة كنص عادي
            ok = Objects.equals(plainPassword, hash);
        }
        return ok ? u : null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void LoginAction() {
        try {
            // 0) تحقق من الإنترنت (واجهة فقط)
            if (monitor != null && !monitor.isOnline()) {
                lastTriedUser = UserNameTextField.getText();
                lastTriedPass = ShowPasswordCheckBox.isSelected()
                        ? visiblePasswordField.getText()
                        : PasswordTextField.getText();
                pendingLogin = true;
                showAlert("Offline", "No internet connection. We'll retry automatically when you’re back online.");
                return;
            }

            // 1) التحقق من مدخلات المستخدم
            String username = UserNameTextField.getText();
            String password = ShowPasswordCheckBox.isSelected()
                    ? visiblePasswordField.getText()
                    : PasswordTextField.getText();

            if (username == null || username.isBlank()) {
                showAlert("Error", "Username (email) is required.");
                return;
            }
            if (password == null || password.isBlank()) {
                showAlert("Error", "Password is required.");
                return;
            }

//            // 2) فحص قاعدة البيانات الآن فقط (ليس عند الإقلاع)
//            Database.Status dbStatus = Database.ping();
//            if (dbStatus != Database.Status.ONLINE) {
//                showAlert("Database Offline",
//                        switch (dbStatus) {
//                            case OFFLINE_NETWORK -> "Cannot reach the database server. Check your internet/VPN or DB host.";
//                            case BAD_CREDENTIALS -> "Database credentials are invalid. Please verify db.user/db.password.";
//                            case CONFIG_MISSING -> "Database config missing in application.properties.";
//                            default -> "Database is currently unavailable. Please try again.";
//                        });
//                return;
//            }

            // 3) تحقّق من القاعدة فعليًا
            User user = authenticate(username, password);
            if (user != null) {
                // خزّن في الجلسة
                Session.set(user);

                // حدّث آخر تسجيل دخول (غير حرج لو فشل)
                try { userDao.updateLastLogin(user.getId()); } catch (Exception ignored) {}

                // أغلق نافذة الدخول وافتح الوجهة حسب الدور
                Stage currentStage = (Stage) rootPane.getScene().getWindow();
                currentStage.close();

                Role r = user.getRole();
                if (r == Role.RECEPTIONIST) {
                    Stage stage = new Stage();
                    navigation.navigateTo(stage, navigation.Reception_Fxml);
                    stage.setTitle("Reception Dashboard");
                    stage.show();
                } else if (r == Role.DOCTOR) {
                    Stage stage = new Stage();
                    navigation.navigateTo(stage, navigation.Doctor_Fxml);
                    stage.setTitle("Doctor Dashboard");
                    stage.show();
                } else if (r == Role.PHARMACIST) {
                    Stage stage = new Stage();
                    navigation.navigateTo(stage, navigation.Pharmacy_Fxml);
                    stage.setTitle("Pharmacy Dashboard");
                    stage.show();
                } else if (r == Role.ADMIN) {
                    Stage stage = new Stage();
                    navigation.navigateTo(stage, navigation.Admin_Fxml);
                    stage.setTitle("Admin Panel");
                    stage.show();
                } else if (r == Role.PATIENT) {
                    showAlert("Access Restricted", "Patient portal is not available in this version.");
                }

                // صفّر حالة pending
                pendingLogin = false;
                lastTriedUser = null;
                lastTriedPass = null;
            } else {
                showAlert("Login Failed", "Invalid credentials");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            showAlert("Error", "An unexpected error occurred: " + cause.getMessage());
        }
    }

    // ================= Reload on Reconnect =================
    /** ينادى من App عندما يعود الانترنت */
    public void onBecameOnline() {
        showReloadOverlay();
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                hideReloadOverlay();
                if (pendingLogin && lastTriedUser != null) {
                    UserNameTextField.setText(lastTriedUser);
                    if (ShowPasswordCheckBox.isSelected()) {
                        visiblePasswordField.setText(lastTriedPass);
                    } else {
                        PasswordTextField.setText(lastTriedPass);
                    }
                    LoginAction();
                }
            });
        }, "reconnect-refresh").start();
    }

    private void showReloadOverlay() {
        if (overlay != null && rootPane.getChildren().contains(overlay)) return;

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(18, 18);

        Label text = new Label("Back online — refreshing…");
        text.setStyle("-fx-font-weight: 600; -fx-text-fill: #155724;");

        HBox chip = new HBox(10, pi, text);
        chip.setPadding(new Insets(8, 12, 8, 12));
        chip.setStyle("-fx-background-color: #d4edda; -fx-border-color:#c3e6cb; -fx-border-radius:10; -fx-background-radius:10;");

        overlay = new StackPane(chip);
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(true);
        StackPane.setMargin(chip, new Insets(16, 0, 0, 0));
        overlay.setTranslateY(-220);

        rootPane.getChildren().add(overlay);

        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
        ft.setToValue(1.0);
        ft.play();
    }

    private void hideReloadOverlay() {
        if (overlay == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            rootPane.getChildren().remove(overlay);
            overlay = null;
        });
        ft.play();
    }

    /** دعم الضغط على Enter */
    public void handleEnterKey(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            LoginAction();
        }
    }

    // ====== أدوات ======
    private final Navigation navigation = new Navigation();
}