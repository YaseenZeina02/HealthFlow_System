package com.example.healthflow.controllers;

import com.example.healthflow.db.Database;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import java.sql.Connection;
import java.sql.SQLException;


public class LoginController {

    private final UserDAO userDao = new com.example.healthflow.dao.UserJdbcDAO();

    // ====== injected from FXML ======
    @FXML private TextField UserNameTextField;
    @FXML private PasswordField PasswordTextField;
    @FXML private CheckBox ShowPasswordCheckBox;
    @FXML private AnchorPane rootPane;
    @FXML private Button LoginButton;
    @FXML private Label AlertLabel;

    private boolean rebindDisableAfterLock;
    // لإظهار/إخفاء كلمة السر
    private final TextField visiblePasswordField = new TextField();

    // ====== connectivity ======
    private final ConnectivityMonitor monitor;

    // ====== Rate Limiting (Brute Force Protection) ======
    private static final java.util.Map<String, java.util.Deque<Long>> loginAttempts = new java.util.concurrent.ConcurrentHashMap<>();    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_TIME_MS = 15 * 60 * 1000; // 15 minutes

    // ====== Auto-retry (اختياري) ======
    private char[] lastTriedPass;  // ✅ Changed to char[] for security
    private boolean pendingLogin;

    // ====== Lock UI helpers ======
    private javafx.animation.Timeline lockCountdown;
    private long lockExpiresAtMs;

    // UI feedback for login-in-progress
    private ProgressIndicator loginSpinner;
    private final javafx.beans.property.BooleanProperty loggingIn =
            new javafx.beans.property.SimpleBooleanProperty(false);

    // ====== أدوات ======
    private final Navigation navigation = new Navigation();



    // overlay chip عند الرجوع أونلاين
    private StackPane overlay;

    // Connectivity banner instance for login header
    private ConnectivityBanner loginBanner;

    public LoginController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    @FXML
    private void initialize() {
        // --- show/hide password (UI فقط) ---
        if (rootPane != null) {
            loginBanner = new ConnectivityBanner(monitor);
            // ثبت البنير أعلى الواجهة وبعرض كامل
            rootPane.getChildren().add(0, loginBanner);
            AnchorPane.setTopAnchor(loginBanner, 0.0);
            AnchorPane.setLeftAnchor(loginBanner, 0.0);
            AnchorPane.setRightAnchor(loginBanner, 0.0);
            // تأكد أنه في المقدمة
            loginBanner.toFront();
        }
        // فحص اتصال ابتدائي بمجرد فتح الشاشة
        initialConnectivityProbe();

        // راقب تغيّر حالة الاتصال لتحديث الرسالة وإظهار إشعار الرجوع أونلاين
        if (monitor != null) {
            // حالة البدء
            if (!monitor.isOnline() && AlertLabel != null) {
                AlertLabel.setText("No internet connection. Please check your network.");
            }
            monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
                if (isOnline) {
                    // أظهر إشعار "رجعنا أونلاين" ثم أخفِ التنبيه
                    showBackOnlineNotice();
                    if (AlertLabel != null) AlertLabel.setText("");
                } else {
                    if (AlertLabel != null) {
                        AlertLabel.setText("No internet connection. Please check your network.");
                    }
                }
            });
        }

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
        if (AlertLabel != null) {
            AlertLabel.getStyleClass().add("hf-alert");
            AlertLabel.setText("");
            AlertLabel.setWrapText(true);
            AlertLabel.setUnderline(false);
            // ✅ show full text without ellipsis & wrap nicely
            AlertLabel.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
            AlertLabel.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            AlertLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            AlertLabel.setMaxWidth(Double.MAX_VALUE);
            if (rootPane != null) {
                AlertLabel.maxWidthProperty().bind(rootPane.widthProperty().subtract(40));
            }
            AlertLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        }

        // --- login spinner inside button ---
        if (LoginButton != null) {
            loginSpinner = new ProgressIndicator();
            loginSpinner.setPrefSize(16, 16);   // صغير ومرتب
            loginSpinner.setMaxSize(16, 16);
            loginSpinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            loginSpinner.setVisible(false);
            loginSpinner.setManaged(false);

            // نحطّه يسار نص الزر
            LoginButton.setContentDisplay(ContentDisplay.LEFT);
            LoginButton.setGraphic(loginSpinner);
            loginSpinner.setStyle("-fx-progress-color: white;");

            // لو حاب تأثير بصري خفيف أثناء التحميل
            LoginButton.getStyleClass().add("hf-btn");
        }
    }


    /** تحقّق باستخدام BCrypt فقط */
    private User authenticate(String emailOrUser, String plainPassword) throws Exception {
        // 1. Input validation and sanitization
        if (emailOrUser == null || plainPassword == null) return null;

        String key = emailOrUser.trim().toLowerCase();

        // 2. Email format validation
        if (!isValidEmail(key)) return null;

        // 3. Max length check
        if (key.length() > 255 || plainPassword.length() > 255) return null;

        User u = userDao.findByEmail(key);
        if (u == null) return null;
        if (!u.isActive()) {
            // Special marker: inactive user (return user object with null password check)
            // Option 1: return null, but we need a way to distinguish in LoginAction.
            // Option 2: throw or wrap? We'll use: return a special user with a "inactive" flag, but for now, just return null and check isActive separately.
            // We'll handle it in LoginAction by checking userDao.findByEmail if authenticate returned null.
            return null;
        }

        String hash = u.getPasswordHash();

        // ✅ BCrypt only - NO plaintext fallback
        if (hash != null && hash.startsWith("$2")) {
            boolean ok = BCrypt.checkpw(plainPassword, hash);
            return ok ? u : null;
        }

        return null; // Reject if password is not BCrypt hashed
    }

    /** التحقق من صحة صيغة البريد الإلكتروني */
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        // Simple but effective email validation
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /** فحص Rate Limiting ومنع Brute Force (خيط-آمن) */
    /** فحص Rate Limiting ومنع Brute Force (خيط-آمن) + إدارة واجهة القفل */
    private boolean isAccountLocked(String email) {
        java.util.Deque<Long> attempts = loginAttempts.get(email);
        if (attempts == null || attempts.isEmpty()) return false;

        long now = System.currentTimeMillis();

        // نظّف المحاولات القديمة بأمان
        for (;;) {
            Long head = attempts.peekFirst();
            if (head == null) break;
            if (now - head > LOCKOUT_TIME_MS) {
                attempts.pollFirst();
            } else {
                break;
            }
        }

        if (attempts.size() >= MAX_ATTEMPTS) {
            long oldestRecentAttempt = attempts.peekFirst();
            long timeRemaining = LOCKOUT_TIME_MS - (now - oldestRecentAttempt);
            if (timeRemaining > 0) {
                startLockCountdown(timeRemaining, email);
                return true;
            }
        }
        return false;
    }

    /** تسجيل محاولة تسجيل دخول فاشلة (خيط-آمن) */
    private void recordFailedAttempt(String email) {
        java.util.Deque<Long> q = loginAttempts.computeIfAbsent(email, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        q.addLast(System.currentTimeMillis());
    }

    /** مسح محاولات تسجيل الدخول الفاشلة بعد النجاح */
    private void clearFailedAttempts(String email) {
        loginAttempts.remove(email);
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
        String username = null;

        // ابدأ إحساس الضغط فورًا (سبنر + منع نقرات متكررة)
        startLoginUi();

        try {
            // (0) أوفلاين
            if (monitor != null && !monitor.isOnline()) {
                String tempPass = ShowPasswordCheckBox.isSelected()
                        ? visiblePasswordField.getText()
                        : PasswordTextField.getText();
                lastTriedPass = tempPass != null ? tempPass.toCharArray() : null;
                pendingLogin = true;
                setAlert("You are offline.", "We will retry automatically when you are back online.");
                stopLoginUi(); // أوقف المؤثر لأننا لن نتابع
                return;
            }

            // (1) تحقق مدخلات
            username = UserNameTextField.getText();
            String password = ShowPasswordCheckBox.isSelected()
                    ? visiblePasswordField.getText()
                    : PasswordTextField.getText();

            if (username == null || username.isBlank()) {
                setAlert("Username (email) is required.", "");
                stopLoginUi();
                return;
            }
            if (password == null || password.isBlank()) {
                setAlert("Password is required.", "");
                stopLoginUi();
                return;
            }

            final String normalizedEmail = username.trim().toLowerCase();

            // (2) حدّ المحاولات قبل الذهاب للداتابيز
            if (isAccountLocked(normalizedEmail)) {
                stopLoginUi();
                return; // شاشة القفل والعدّاد تُدار داخل isAccountLocked()
            }

            // (3) المصادقة — نفّذها على خيط خلفي حتى لا تتجمّد الواجهة ويظهر السبينر
            final String uFinal = username;
            final String pFinal = password;

            javafx.concurrent.Task<User> authTask = new javafx.concurrent.Task<>() {
                @Override
                protected User call() throws Exception {
                    return authenticate(uFinal, pFinal);
                }
            };

            authTask.setOnSucceeded(ev -> {
                User user = authTask.getValue();
                if (user != null) {
                    // نجاح
                    clearFailedAttempts(normalizedEmail);
                    Session.set(user);
                    try {
                        userDao.updateLastLogin(user.getId());
                    } catch (Exception e) {
                        System.err.println("Failed to update last login for user " + user.getId() + ": " + e.getMessage());
                    }

                    // تنظيف الواجهة
                    if (AlertLabel != null) AlertLabel.setText("");
                    enableLoginButtonSafely();
                    if (lockCountdown != null) { lockCountdown.stop(); lockCountdown = null; }

                    // فتح الواجهات حسب الدور
                    try {
                        Stage currentStage = (Stage) rootPane.getScene().getWindow();
                        Role r = user.getRole();
                        if (r == Role.RECEPTIONIST) {
                            Stage stage = new Stage();
                            FXMLLoader loader = new FXMLLoader(getClass().getResource(navigation.Reception_Fxml));
                            Parent root = loader.load();
                            stage.setScene(new Scene(root));
                            stage.setTitle("Reception Dashboard");

                            // ✅ Fill screen by default
                            stage.setResizable(true);
                            stage.setMaximized(true);
                            Rectangle2D boundsR = Screen.getPrimary().getVisualBounds();
                            stage.setX(boundsR.getMinX());
                            stage.setY(boundsR.getMinY());
                            stage.setWidth(boundsR.getWidth());
                            stage.setHeight(boundsR.getHeight());

                            stage.show();
                            ReceptionController rc = loader.getController();
                            stage.setOnCloseRequest(e2 -> rc.shutdown());
                            currentStage.close();

                        } else if (r == Role.DOCTOR) {
                            Stage stage = new Stage();
                            navigation.navigateTo(stage, navigation.Doctor_Fxml);
                            stage.setTitle("Doctor Dashboard");

                            // ✅ Fill screen by default
                            stage.setResizable(true);
                            stage.setMaximized(true);
                            Rectangle2D boundsD = Screen.getPrimary().getVisualBounds();
                            stage.setX(boundsD.getMinX());
                            stage.setY(boundsD.getMinY());
                            stage.setWidth(boundsD.getWidth());
                            stage.setHeight(boundsD.getHeight());

                            stage.show();
                            currentStage.close();

                        } else if (r == Role.PHARMACIST) {
                            Stage stage = new Stage();
                            navigation.navigateTo(stage, navigation.Pharmacy_Fxml);
                            stage.setTitle("Pharmacy Dashboard");

                            // ✅ Fill screen by default
                            stage.setResizable(true);
                            stage.setMaximized(true);
                            Rectangle2D boundsP = Screen.getPrimary().getVisualBounds();
                            stage.setX(boundsP.getMinX());
                            stage.setY(boundsP.getMinY());
                            stage.setWidth(boundsP.getWidth());
                            stage.setHeight(boundsP.getHeight());

                            stage.show();
                            currentStage.close();

                        } else if (r == Role.ADMIN) {
                            Stage stage = new Stage();
                            navigation.navigateTo(stage, navigation.Admin_Fxml);
                            stage.setTitle("Admin Panel");

                            // ✅ Fill screen by default
                            stage.setResizable(true);
                            stage.setMaximized(true);
                            Rectangle2D boundsA = Screen.getPrimary().getVisualBounds();
                            stage.setX(boundsA.getMinX());
                            stage.setY(boundsA.getMinY());
                            stage.setWidth(boundsA.getWidth());
                            stage.setHeight(boundsA.getHeight());

                            stage.show();
                            currentStage.close();

                        } else if (r == Role.PATIENT) {
                            showAlert("Access Restricted", "Patient portal is not available in this version.");
                        }
                    } catch (Exception loadEx) {
                        System.err.println("Navigation error: " + loadEx.getMessage());
                        loadEx.printStackTrace();
                        setAlert("Navigation error.", "Please try again.");
                    }

                    // مسح الحسّاس
                    if (lastTriedPass != null) {
                        java.util.Arrays.fill(lastTriedPass, '\0');
                        lastTriedPass = null;
                    }
                    pendingLogin = false;

                } else {
                    User u = null;
                    try {
                        u = userDao.findByEmail(normalizedEmail);
                    } catch (Exception e) {
                        System.err.println("User lookup failed: " + e.getMessage());
                        setAlert("An error occurred during login.", "Please try again.");
                        stopLoginUi();
                        return;
                    }
                    if (u != null && !u.isActive()) {
                        setAlert("Account inactive.", "Please contact the administrator to activate your account.");
                        stopLoginUi();
                        return;
                    }
                    recordFailedAttempt(normalizedEmail);
                    int attempts = getAttemptsCount(normalizedEmail);
                    int remaining = Math.max(0, MAX_ATTEMPTS - attempts);

                    // قد يتحول لقفل الآن
                    if (isAccountLocked(normalizedEmail)) {
                        // سيبدأ العدّاد ويعطّل الزر ويُحدّث الـLabel
                        stopLoginUi();
                        return;
                    }

                    setAlert(
                            "Username or password is invalid.",
                            String.format("Attempts: %d/%d%s", attempts, MAX_ATTEMPTS, (remaining > 0 ? " · " + remaining + " left" : ""))
                    );
                }

                // إيقاف المؤثر بعد انتهاء المصادقة (نجاح/فشل)
                stopLoginUi();
            });

            authTask.setOnFailed(ev -> {
                Throwable ex = authTask.getException();
                System.err.println("Auth task failed: " + (ex != null ? ex.getMessage() : "unknown"));
                if (ex != null) ex.printStackTrace();
                setAlert("An error occurred during login.", "Please try again.");
                stopLoginUi();
            });

            Thread t = new Thread(authTask, "auth-task");
            t.setDaemon(true);
            t.start();

            // لا منطق بعد إطلاق الـ Task؛ الإكمال يحدث في الـ handlers أعلاه
            return;

        } catch (Exception e) {
            System.err.println("Login error for user: " + username);
            e.printStackTrace();
            setAlert("An error occurred during login.", "Please try again.");
            stopLoginUi(); // احتياط في حال الخطأ وقع قبل إطلاق الـ Task
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

                if (pendingLogin && lastTriedPass != null) {
                    // ✅ Restore password from char array (لا نلمس الزر هنا)
                    String tempPass = new String(lastTriedPass);
                    if (ShowPasswordCheckBox.isSelected()) {
                        visiblePasswordField.setText(tempPass);
                    } else {
                        PasswordTextField.setText(tempPass);
                    }

                    // جرّب الدخول تلقائياً — أي إدارة لقفل/عدّاد ستحدث داخل LoginAction
                    LoginAction();

                    // ✅ امسح الحسّاس من الذاكرة
                    java.util.Arrays.fill(lastTriedPass, '\0');
                    lastTriedPass = null;
                    tempPass = null;
                    pendingLogin = false;

                    // تنظيف واجهة بسيط: فقط لو ما في عدّاد قيد العمل (أي ليس مقفول)
                    if (lockCountdown == null) {
                        if (AlertLabel != null) AlertLabel.setText("");

                        if (LoginButton != null) {
                            // لو كنا فكّينا الربط أثناء القفل، نعيد ربطه الآن
                            if (rebindDisableAfterLock) {
                                OnlineBindings.disableWhenOffline(monitor, LoginButton);
                                rebindDisableAfterLock = false;
                            } else if (!LoginButton.disableProperty().isBound()) {
                                // وإلا فعّل الزر فقط إذا غير مربوط
                                LoginButton.setDisable(false);
                            }
                        }
                    }
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


    /** تحديث تنبيه الواجهة في سطرين */
    private void setAlert(String line1, String line2) {
        if (AlertLabel == null) return;
        String full = (line2 == null || line2.isBlank()) ? line1 : (line1 + "\n" + line2);
        AlertLabel.setText(full);
        // keep a tooltip with the entire message so it’s readable even if layout is tight
        if (AlertLabel.getTooltip() == null) {
            AlertLabel.setTooltip(new Tooltip(full));
        } else {
            AlertLabel.getTooltip().setText(full);
        }
    }

    /** ابدأ مؤثر التحميل على زر الدخول (لا نغيّر disable لتفادي الـ binding) */
    private void startLoginUi() {
        loggingIn.set(true);
        if (LoginButton != null) {
            if (loginSpinner != null) { loginSpinner.setVisible(true); loginSpinner.setManaged(true); }
            LoginButton.setText("Logging in…");
            // امنع النقرات بدل disable (حتى مع binding)
            LoginButton.setMouseTransparent(true);
        }
    }

    /** أوقف مؤثر التحميل مهما كانت النتيجة */
    private void stopLoginUi() {
        loggingIn.set(false);
        if (LoginButton != null) {
            if (loginSpinner != null) { loginSpinner.setVisible(false); loginSpinner.setManaged(false); }
            LoginButton.setText("Login");
            LoginButton.setMouseTransparent(false);
        }
    }

    /** أعِد تمكين زر الدخول بأمان بدون كسر أي binding */
    private void enableLoginButtonSafely() {
        if (LoginButton == null) return;
        if (rebindDisableAfterLock) {
            // كنا فَكّينا الربط أثناء الحظر: نعيد ربطه الآن
            OnlineBindings.disableWhenOffline(monitor, LoginButton);
            rebindDisableAfterLock = false;
        } else if (!LoginButton.disableProperty().isBound()) {
            // لو غير مربوط أصلاً، مسموح نغيّره يدويًا
            LoginButton.setDisable(false);
        }
        // لو مربوط وما في rebind → لا تلمسه (الربط هو اللي يدير حالته)
    }

    /** عدد المحاولات الحالية خلال نافذة الحظر */
    private int getAttemptsCount(String email) {
        java.util.Deque<Long> q = loginAttempts.get(email);
        if (q == null) return 0;
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > LOCKOUT_TIME_MS) q.pollFirst();
        return q.size();
    }

    /** بدء عدّاد الحظر وتحديث الواجهة كل ثانية */
    private void startLockCountdown(long remainingMs, String email) {
        if (lockCountdown != null) lockCountdown.stop();

        lockExpiresAtMs = System.currentTimeMillis() + Math.max(0, remainingMs);

        // 👇 افصل الربط مؤقتًا ثم عطّل الزر يدويًا
        if (LoginButton != null) {
            rebindDisableAfterLock = LoginButton.disableProperty().isBound();
            if (rebindDisableAfterLock) {
                LoginButton.disableProperty().unbind();
            }
            LoginButton.setDisable(true);
        }

        lockCountdown = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> {
                    long left = lockExpiresAtMs - System.currentTimeMillis();
                    if (left <= 0) {
                        // انتهى الحظر → أعد تمكين الزر وأعد الربط إن لزم
                        if (LoginButton != null) {
                            if (rebindDisableAfterLock) {
                                // أعد ربطه بحالة الاتصال
                                OnlineBindings.disableWhenOffline(monitor, LoginButton);
                                rebindDisableAfterLock = false;
                            } else {
                                LoginButton.setDisable(false);
                            }
                        }
                        if (AlertLabel != null) AlertLabel.setText("");
                        java.util.Deque<Long> q = loginAttempts.get(email);
                        if (q != null) q.clear();
                        lockCountdown.stop();
                        return;
                    }
                    int attempts = getAttemptsCount(email);
                    long mins = left / 60000;
                    long secs = (left % 60000) / 1000;
                    String line1 = "Too many failed login attempts. Account is temporarily locked.";
                    String line2 = String.format("Attempts: %d/%d · Retry in %02d:%02d", attempts, MAX_ATTEMPTS, mins, secs);
                    setAlert(line1, line2);
                })
        );
        lockCountdown.setCycleCount(javafx.animation.Animation.INDEFINITE);
        lockCountdown.play();
    }


    /** يقوم بفحص اتصال سريع عند تشغيل البرنامج لعرض حالة الاتصال فوراً على البنير */
    private void initialConnectivityProbe() {
        new Thread(() -> {
            boolean online = false;
            try (Connection c = Database.get()) {
                online = (c != null && !c.isClosed());
            } catch (Exception ex) {
                online = false;
            }
            final boolean finalOnline = online;
            Platform.runLater(() -> {
                // لو في مشكلة اتصال، أعطِ ملاحظة سريعة.
                if (!finalOnline) {
                    if (AlertLabel != null) {
                        AlertLabel.setText("No internet connection. Please check your network.");
                    }
                    // تأكد من ظهور البنير أعلى الشاشة كحل فوري عند الإقلاع الأوفلاين
                    if (loginBanner != null) {
                        loginBanner.setVisible(true);
                        loginBanner.toFront();
                    }
                }
            });
        }, "login-initial-connectivity-probe").start();
    }

    /** إشعار قصير عند الرجوع أونلاين في شاشة الدخول */
    private void showBackOnlineNotice() {
        if (rootPane == null) return;

        // استخدم نفس آلية الـ overlay الخفيفة
        if (overlay != null && rootPane.getChildren().contains(overlay)) {
            // لو فيه Overlay قديم، احذفه أولًا
            rootPane.getChildren().remove(overlay);
            overlay = null;
        }

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(16, 16);

        Label text = new Label("Back online — reconnected");
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
        overlay.toFront();

        overlay.setOpacity(0);
        FadeTransition ftIn = new FadeTransition(Duration.millis(180), overlay);
        ftIn.setToValue(1.0);
        ftIn.play();

        // اتركه زمن مناسب للقراءة ثم أخفهِ تلقائيًا
        javafx.animation.PauseTransition stay = new javafx.animation.PauseTransition(Duration.seconds(2.8));
        stay.setOnFinished(ev -> {
            FadeTransition ftOut = new FadeTransition(Duration.millis(180), overlay);
            ftOut.setToValue(0);
            ftOut.setOnFinished(e2 -> {
                rootPane.getChildren().remove(overlay);
                overlay = null;
            });
            ftOut.play();
        });
        stay.play();
    }
}