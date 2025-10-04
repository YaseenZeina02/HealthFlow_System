package com.example.healthflow.controllers;

import com.example.healthflow.db.Database;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.model.dto.PatientView;
import com.example.healthflow.service.PatientService;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReceptionController {

    /* ============ UI ============ */
    @FXML private AnchorPane DashboardAnchorPane;
    @FXML private AnchorPane PatientAnchorPane;
    @FXML private AnchorPane AppointmentsAnchorPane;
    @FXML private AnchorPane DoctorAnchorPane;
    @FXML private VBox rootPane;

    @FXML private Button DachboardButton;
    @FXML private Button PatientsButton;
    @FXML private Button AppointmentsButton;
    @FXML private Button BackButton;
    @FXML private Button DoctorsButton;

    @FXML private Label DateOfDay;
    @FXML private Label time;
    @FXML private Label welcomeUser;

    @FXML private Label UsernameLabel;
    @FXML private Label UserIdLabel;

    @FXML private TextField FullNameTextField;
    @FXML private TextField PatientIdTextField;   // **National Id**
    @FXML private ComboBox<Gender> GenderComboBox;
    @FXML private DatePicker DateOfBirthPicker;
    @FXML private TextField PhoneTextField;
    @FXML private TextArea  medicalHistory;

    @FXML private Button InsertButton;
    @FXML private Button UpdateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearBtn;

    @FXML private TextField search;

    @FXML private TableView<PatientRow> patientTable;
    @FXML private TableColumn<PatientRow, String>    colNationalId;
    @FXML private TableColumn<PatientRow, String>    colName;
    @FXML private TableColumn<PatientRow, String>    colGender;
    @FXML private TableColumn<PatientRow, LocalDate> colDob;
    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;

    @FXML private Label NumberOfTotalAppointments;
    @FXML private Label NumberOfTotalDoctors;
    @FXML private Label NumberOfTotalPatients;

    /* ============ Types ============ */
    public enum Gender { MALE, FEMALE }

    /* ============ State ============ */
    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
    private FilteredList<PatientRow> filtered;
    private final Navigation navigation = new Navigation();
    private final PatientService patientService = new PatientService();

    /* ============ Connectivity ============ */
    private final ConnectivityMonitor monitor;

    // ---- حواجز لمنع تنبيهات مكررة أو تنبيه أولي مزعج ----
    private static volatile boolean listenerRegistered = false; // عبر كل النسخ
    private static volatile Boolean lastNotifiedOnline = null;  // آخر حالة أرسلنا عنها Alert

    public ReceptionController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader (لو ما تمّ تمرير monitor من برّا)
    public ReceptionController() {
        this(new ConnectivityMonitor());
    }

    /* ============ Init ============ */
    @FXML
    private void initialize() {
        // Start connectivity monitor (مرة لكل Monitor)
        monitor.start();

        // Banner لطيف أعلى الواجهة
        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
        }

        // عطّل الأزرار عند الأوفلاين (شامل DoctorsButton)
        OnlineBindings.disableWhenOffline(
                monitor,
                InsertButton, UpdateButton, deleteButton, clearBtn,
                DachboardButton, PatientsButton, AppointmentsButton,
                DoctorsButton
        );

        // Listener التنبيهات — ضيفه مرّة واحدة فقط عبر كل النسخ
        if (!listenerRegistered) {
            listenerRegistered = true;

            // تجاهل الإشارة الأولى (initial emission) علشان ما يطلع "Back online" عند فتح الشاشة
            final boolean[] firstEmissionHandled = { false };

            monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
                if (!firstEmissionHandled[0]) {
                    firstEmissionHandled[0] = true;
                    lastNotifiedOnline = isOnline; // خزّن الحالة الحالية بدون تنبيه
                    return;
                }
                // امنع تنبيهات مكررة لنفس الحالة
                if (lastNotifiedOnline != null && lastNotifiedOnline == isOnline) return;
                lastNotifiedOnline = isOnline;

                if (!isOnline) {
                    showWarn("Offline", "No internet connection. Some actions are disabled.");
                } else {
                    showInfo("Back online", "Connection restored.");
                }
            });
        }

        // تنقل
        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPane());
        AppointmentsButton.setOnAction(e -> showAppointmentPane());
        DoctorsButton.setOnAction(e -> showDoctorPane());
        BackButton.setOnAction(e -> BackAction());

        // ساعة وتاريخ
        startClock();

        // إعدادات UI خفيفة
        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);
        wirePatientTable();
        wireSearch();

        InsertButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doInsertPatient(); });
        UpdateButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doUpdatePatient(); });
        deleteButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doDeletePatient(); });
        clearBtn.setOnAction(e -> clearForm());


        // ✅ لا نعمل استعلامات DB ثقيلة هنا!
        // حمّل الاسم والمرضى بعد عرض المشهد وفي خيوط خلفية.
        Platform.runLater(() -> {
            new Thread(() -> {
                try { loadHeaderUser(); } catch (Exception ignored) {}
            }, "hdr-user-load").start();

            new Thread(() -> {
                try {
                    var list = patientService.listPatients();
                    Platform.runLater(() -> {
                        patientData.clear();
                        for (PatientView pv : list) {
                            patientData.add(new PatientRow(
                                    pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
                                    pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
                            ));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("Load Patients", ex));
                }
            }, "patients-load").start();
        });

        // ابدأ بالداشبورد
        showDashboardPane();
    }

    /* ============ Clock (12h) ============ */
    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, e -> time.setText(LocalTime.now().format(tf))),
                new KeyFrame(Duration.seconds(1))
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateOfDay.setText(LocalDate.now().format(df));
    }

    /* ============ Load header user ============ */
    private void loadHeaderUser() {
        var u = Session.get();   // Get current user from session
        if (u == null) return;

        // (اختياري) جلب الاسم من DB لعرض أحدث اسم
        String sql = "SELECT id, full_name FROM users WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String fullName = rs.getString("full_name");
                    Platform.runLater(() -> {
                        UsernameLabel.setText(fullName);
                        UserIdLabel.setText(Long.toString(id));
                        welcomeUser.setText(firstName(fullName));
                    });
                    return;
                }
            }
        } catch (SQLException ignored) {
            // fallback
        }

        Platform.runLater(() -> {
            UsernameLabel.setText(u.getFullName());
            UserIdLabel.setText(String.valueOf(u.getId()));
            welcomeUser.setText(firstName(u.getFullName()));
        });
    }

    private String firstName(String full) {
        if (full == null || full.isBlank()) return "";
        return full.trim().split("\\s+")[0];
    }

    /* ============ Navigation ============ */
    @FXML
    private void BackAction() {
        Stage stage = (Stage) BackButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
            loader.setControllerFactory(type ->
                    type == LoginController.class ? new LoginController(monitor) : null
            );
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            showError("Navigation", e);
        }
    }

    /* ============ Panes ============ */
    private void showDashboardPane() {
        DashboardAnchorPane.setVisible(true);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
    }
    private void showPatientsPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
    }
    private void showAppointmentPane(){
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(true);
        DoctorAnchorPane.setVisible(false);
    }
    private void showDoctorPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(true);
    }

    /* ============ Table & Search ============ */
    private void wirePatientTable() {
        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());

        patientTable.setItems(patientData);

        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null) return;
            FullNameTextField.setText(row.getFullName());
            PatientIdTextField.setText(row.getNationalId());
            PhoneTextField.setText(row.getPhone());
            medicalHistory.setText(row.getMedicalHistory());
            DateOfBirthPicker.setValue(row.getDateOfBirth());
            GenderComboBox.setValue("MALE".equals(row.getGender()) ? Gender.MALE : Gender.FEMALE);
        });
    }

    private void wireSearch() {
        filtered = new FilteredList<>(patientData, p -> true);
        search.textProperty().addListener((obs, old, q) -> {
            String s = (q == null) ? "" : q.trim().toLowerCase();
            if (s.isEmpty()) filtered.setPredicate(p -> true);
            else filtered.setPredicate(p ->
                    contains(p.getFullName(), s) ||
                            contains(p.getGender(), s) ||
                            contains(p.getPhone(), s) ||
                            contains(p.getNationalId(), s) ||
                            contains(p.getMedicalHistory(), s) ||
                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s))
            );
        });
        SortedList<PatientRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        patientTable.setItems(sorted);
    }

    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }

    /* ============ Load Patients via Service (unused directly) ============ */
    private void loadPatientsSyncIntoTable() throws Exception {
        var list = patientService.listPatients();
        Platform.runLater(() -> {
            patientData.clear();
            for (PatientView pv : list) {
                patientData.add(new PatientRow(
                        pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
                        pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
                ));
            }
        });
    }

    /* ============ CRUD via Service ============ */
    private void doInsertPatient() {
        String fullName = trimOrNull(FullNameTextField.getText());
        String nid      = trimOrNull(PatientIdTextField.getText());
        Gender gender   = GenderComboBox.getValue();
        LocalDate dob   = DateOfBirthPicker.getValue();
        String phone    = trimOrNull(PhoneTextField.getText());
        String history  = trimOrNull(medicalHistory.getText());

        if (fullName == null || dob == null || gender == null) {
            showWarn("Validation", "Full name, gender and date of birth are required.");
            return;
        }
        if (phone == null) {
            showWarn("Validation", "Patient must have a phone number.");
            return;
        }

        try {
            var pv = patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
            patientData.add(new PatientRow(
                    pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
                    pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
            ));
            clearForm();
            showInfo("Insert", "Patient inserted successfully.");
        } catch (Exception ex) {
            showError("Insert Patient", ex);
        }
    }

    private void doUpdatePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Update", "Select a patient row first.");
            return;
        }

        String fullName = trimOrNull(FullNameTextField.getText());
        String nid      = trimOrNull(PatientIdTextField.getText());
        String phone    = trimOrNull(PhoneTextField.getText());
        String history  = trimOrNull(medicalHistory.getText());
        Gender gender   = GenderComboBox.getValue();
        LocalDate dob   = DateOfBirthPicker.getValue();

        if (fullName == null || dob == null || gender == null) {
            showWarn("Validation", "Full name, gender and date of birth are required.");
            return;
        }

        try {
            patientService.updatePatient(row.getUserId(), row.getPatientId(),
                    fullName, nid, phone, dob, gender.name(), history);

            row.setFullName(fullName);
            row.setNationalId(nid);
            row.setPhone(phone);
            row.setDateOfBirth(dob);
            row.setGender(gender.name());
            row.setMedicalHistory(history);
            patientTable.refresh();

            showInfo("Update", "Patient updated successfully.");
        } catch (Exception ex) {
            showError("Update Patient", ex);
        }
    }

    private void doDeletePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Delete", "Select a patient row first.");
            return;
        }
        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;

        try {
            patientService.deletePatientByUserId(row.getUserId());
            patientData.remove(row);
            clearForm();
            showInfo("Delete", "Patient deleted.");
        } catch (Exception e) {
            showError("Delete Patient", e);
        }
    }

    private void clearForm() {
        FullNameTextField.clear();
        PatientIdTextField.clear();
        PhoneTextField.clear();
        medicalHistory.clear();
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);
        patientTable.getSelectionModel().clearSelection();
    }

    /* ============ Helpers ============ */
    private boolean ensureOnlineOrAlert() {
        if (monitor != null && !monitor.isOnline()) {
            showWarn("Offline", "You're offline. Please reconnect and try again.");
            return false;
        }
        return true;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String showError(String title, Exception ex) {
        String ms = null;
        if (ex instanceof SQLException sqlEx && "23514".equals(sqlEx.getSQLState())) {
            // 23514 = check_violation
            ms ="Invalid data: please check that the date of birth is not in the future.";
        }else{
            ex.printStackTrace();
        }
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex.getMessage());
        a.showAndWait();
        return ms;
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /* ============ Row model ============ */
    public static class PatientRow {
        private final LongProperty patientId = new SimpleLongProperty();
        private final LongProperty userId    = new SimpleLongProperty();
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty nationalId = new SimpleStringProperty();
        private final StringProperty phone = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
        private final StringProperty gender = new SimpleStringProperty();
        private final StringProperty medicalHistory = new SimpleStringProperty();

        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
                          String phone, LocalDate dob, String gender, String medicalHistory) {
            setPatientId(patientId);
            setUserId(userId);
            setFullName(fullName);
            setNationalId(nationalId);
            setPhone(phone);
            setDateOfBirth(dob);
            setGender(gender);
            setMedicalHistory(medicalHistory);
        }

        public long getPatientId() { return patientId.get(); }
        public void setPatientId(long v) { patientId.set(v); }
        public LongProperty patientIdProperty() { return patientId; }

        public long getUserId() { return userId.get(); }
        public void setUserId(long v) { userId.set(v); }
        public LongProperty userIdProperty() { return userId; }

        public String getFullName() { return fullName.get(); }
        public void setFullName(String v) { fullName.set(v); }
        public StringProperty fullNameProperty() { return fullName; }

        public String getNationalId() { return nationalId.get(); }
        public void setNationalId(String v) { nationalId.set(v); }
        public StringProperty nationalIdProperty() { return nationalId; }

        public String getPhone() { return phone.get(); }
        public void setPhone(String v) { phone.set(v); }
        public StringProperty phoneProperty() { return phone; }

        public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
        public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
        public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }

        public String getGender() { return gender.get(); }
        public void setGender(String v) { gender.set(v); }
        public StringProperty genderProperty() { return gender; }

        public String getMedicalHistory() { return medicalHistory.get(); }
        public void setMedicalHistory(String v) { medicalHistory.set(v); }
        public StringProperty medicalHistoryProperty() { return medicalHistory; }
    }
}


//package com.example.healthflow.controllers;
//
//import com.example.healthflow.db.Database;
//import com.example.healthflow.net.ConnectivityMonitor;
//import com.example.healthflow.service.AuthService.Session;
//import com.example.healthflow.model.dto.PatientView;
//import com.example.healthflow.service.PatientService;
//import com.example.healthflow.ui.ConnectivityBanner;
//import com.example.healthflow.ui.OnlineBindings;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.beans.property.*;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.collections.transformation.FilteredList;
//import javafx.collections.transformation.SortedList;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.sql.*;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//
//public class ReceptionController {
//
//    /* ============ UI ============ */
//    @FXML private AnchorPane DashboardAnchorPane;
//    @FXML private AnchorPane PatientAnchorPane;
//    @FXML private AnchorPane AppointmentsAnchorPane;
//    @FXML private AnchorPane DoctorAnchorPane;
//    @FXML private VBox rootPane;
//
//    @FXML private Button DachboardButton;
//    @FXML private Button PatientsButton;
//    @FXML private Button AppointmentsButton;
//    @FXML private Button BackButton;
//    @FXML private Button DoctorsButton;
//
//    @FXML private Label DateOfDay;
//    @FXML private Label time;
//    @FXML private Label welcomeUser;
//
//    @FXML private Label UsernameLabel;
//    @FXML private Label UserIdLabel;
//
//    @FXML private TextField FullNameTextField;
//    @FXML private TextField PatientIdTextField;   // **National Id**
//    @FXML private ComboBox<Gender> GenderComboBox;
//    @FXML private DatePicker DateOfBirthPicker;
//    @FXML private TextField PhoneTextField;
//    @FXML private TextArea  medicalHistory;
//
//    @FXML private Button InsertButton;
//    @FXML private Button UpdateButton;
//    @FXML private Button deleteButton;
//    @FXML private Button clearBtn;
//
//    @FXML private TextField search;
//
//    @FXML private TableView<PatientRow> patientTable;
//    @FXML private TableColumn<PatientRow, String>    colNationalId;
//    @FXML private TableColumn<PatientRow, String>    colName;
//    @FXML private TableColumn<PatientRow, String>    colGender;
//    @FXML private TableColumn<PatientRow, LocalDate> colDob;
//    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
//    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;
//
//    @FXML private Label NumberOfTotalAppointments;
//    @FXML private Label NumberOfTotalDoctors;
//    @FXML private Label NumberOfTotalPatients;
//
//    /* ============ Types ============ */
//    public enum Gender { MALE, FEMALE }
//
//    /* ============ State ============ */
//    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
//    private FilteredList<PatientRow> filtered;
//    private final Navigation navigation = new Navigation();
//    private final PatientService patientService = new PatientService();
//
//    /* ============ Connectivity ============ */
//    private final ConnectivityMonitor monitor;
//
//    // ---- حواجز لمنع تنبيهات مكررة أو تنبيه أولي مزعج ----
//    private static volatile boolean listenerRegistered = false; // عبر كل النسخ
//    private static volatile Boolean lastNotifiedOnline = null;  // آخر حالة أرسلنا عنها Alert
//
//    public ReceptionController(ConnectivityMonitor monitor) {
//        this.monitor = monitor;
//    }
//
//    // Default constructor for FXML loader (لو ما تمّ تمرير monitor من برّا)
//    public ReceptionController() {
//        this(new ConnectivityMonitor());
//    }
//
//    /* ============ Init ============ */
//    @FXML
//    private void initialize() {
//        // Start connectivity monitor (مرة لكل Monitor)
//        monitor.start();
//
//        // Banner لطيف أعلى الواجهة
//        if (rootPane != null) {
//            ConnectivityBanner banner = new ConnectivityBanner(monitor);
//            rootPane.getChildren().add(0, banner);
//        }
//
//        // عطّل الأزرار عند الأوفلاين (شامل DoctorsButton)
//        OnlineBindings.disableWhenOffline(
//                monitor,
//                InsertButton, UpdateButton, deleteButton, clearBtn,
//                DachboardButton, PatientsButton, AppointmentsButton,
//                DoctorsButton
//        );
//
//        // Listener التنبيهات — ضيفه مرّة واحدة فقط عبر كل النسخ
//        if (!listenerRegistered) {
//            listenerRegistered = true;
//
//            // تجاهل الإشارة الأولى (initial emission) علشان ما يطلع "Back online" عند فتح الشاشة
//            final boolean[] firstEmissionHandled = { false };
//
//            monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
//                if (!firstEmissionHandled[0]) {
//                    firstEmissionHandled[0] = true;
//                    lastNotifiedOnline = isOnline; // خزّن الحالة الحالية بدون تنبيه
//                    return;
//                }
//                // امنع تنبيهات مكررة لنفس الحالة
//                if (lastNotifiedOnline != null && lastNotifiedOnline == isOnline) return;
//                lastNotifiedOnline = isOnline;
//
//                if (!isOnline) {
//                    showWarn("Offline", "No internet connection. Some actions are disabled.");
//                } else {
//                    showInfo("Back online", "Connection restored.");
//                }
//            });
//        }
//
//        // تنقل
//        DachboardButton.setOnAction(e -> showDashboardPane());
//        PatientsButton.setOnAction(e -> showPatientsPane());
//        AppointmentsButton.setOnAction(e -> showAppointmentPane());
//        DoctorsButton.setOnAction(e -> showDoctorPane());
//        BackButton.setOnAction(e -> BackAction());
//
//        // ساعة وتاريخ
//        startClock();
//
//        // تحميل اسم المستخدم (الجلسة)
//        loadHeaderUser();
//
//        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
//        GenderComboBox.setValue(Gender.MALE);
//
//        DateOfBirthPicker.setValue(null);
//
//        // جدول + تحميل + بحث
//        wirePatientTable();
//        loadPatients();
//        wireSearch();
//
//        // CRUD via service + تحقق من الاتصال قبل التنفيذ
//        InsertButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doInsertPatient(); });
//        UpdateButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doUpdatePatient(); });
//        deleteButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doDeletePatient(); });
//        clearBtn.setOnAction(e -> clearForm());
//
//        // ابدأ بالداشبورد
//        showDashboardPane();
//    }
//
//    /* ============ Clock (12h) ============ */
//    private void startClock() {
//        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
//        Timeline tl = new Timeline(
//                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
//                new KeyFrame(Duration.seconds(1))
//        );
//        tl.setCycleCount(Timeline.INDEFINITE);
//        tl.play();
//
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM:dd:yyyy");
//        DateOfDay.setText(LocalDate.now().format(df));
//    }
//
//    /* ============ Load header user ============ */
//    private void loadHeaderUser() {
//        var u = Session.get();   // Get current user from session
//        if (u == null) return;
//
//        // (اختياري) جلب الاسم من DB
//        String sql = "SELECT id, full_name FROM users WHERE id = ?";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, u.getId());
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    long id = rs.getLong("id");
//                    String fullName = rs.getString("full_name");
//                    UsernameLabel.setText(fullName);
//                    UserIdLabel.setText(Long.toString(id));
//                    welcomeUser.setText(firstName(fullName));
//                    return;
//                }
//            }
//        } catch (SQLException ignore) {
//            // fallback
//        }
//
//        UsernameLabel.setText(u.getFullName());
//        UserIdLabel.setText(String.valueOf(u.getId()));
//        welcomeUser.setText(firstName(u.getFullName()));
//    }
//
//    private String firstName(String full) {
//        if (full == null || full.isBlank()) return "";
//        return full.trim().split("\\s+")[0];
//    }
//
//    /* ============ Navigation ============ */
//    @FXML
//    private void BackAction() {
//        Stage stage = (Stage) BackButton.getScene().getWindow();
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
//            loader.setControllerFactory(type ->
//                    type == LoginController.class ? new LoginController(monitor) : null
//            );
//            Parent root = loader.load();
//            stage.setScene(new Scene(root));
//            stage.setResizable(false);
//            stage.show();
//        } catch (IOException e) {
//            showError("Navigation", e);
//        }
//    }
//
//    /* ============ Panes ============ */
//    private void showDashboardPane() {
//        DashboardAnchorPane.setVisible(true);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showPatientsPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(true);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showAppointmentPane(){
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(true);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showDoctorPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(true);
//    }
//
//    /* ============ Table & Search ============ */
//    private void wirePatientTable() {
//        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
//        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
//        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
//        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
//        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
//        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());
//
//        patientTable.setItems(patientData);
//
//        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
//            if (row == null) return;
//            FullNameTextField.setText(row.getFullName());
//            PatientIdTextField.setText(row.getNationalId());
//            PhoneTextField.setText(row.getPhone());
//            medicalHistory.setText(row.getMedicalHistory());
//            DateOfBirthPicker.setValue(row.getDateOfBirth());
//            GenderComboBox.setValue("MALE".equals(row.getGender()) ? Gender.MALE : Gender.FEMALE);
//        });
//    }
//
//    private void wireSearch() {
//        filtered = new FilteredList<>(patientData, p -> true);
//        search.textProperty().addListener((obs, old, q) -> {
//            String s = (q == null) ? "" : q.trim().toLowerCase();
//            if (s.isEmpty()) filtered.setPredicate(p -> true);
//            else filtered.setPredicate(p ->
//                    contains(p.getFullName(), s) ||
//                            contains(p.getGender(), s) ||
//                            contains(p.getPhone(), s) ||
//                            contains(p.getNationalId(), s) ||
//                            contains(p.getMedicalHistory(), s) ||
//                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s))
//            );
//        });
//        SortedList<PatientRow> sorted = new SortedList<>(filtered);
//        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
//        patientTable.setItems(sorted);
//    }
//
//    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }
//
//    /* ============ Load Patients via Service ============ */
//    private void loadPatients() {
//        patientData.clear();
//        try {
//            for (PatientView pv : patientService.listPatients()) {
//                patientData.add(new PatientRow(
//                        pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
//                        pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
//                ));
//            }
//        } catch (Exception ex) {
//            showError("Load Patients", ex);
//        }
//    }
//
//    /* ============ CRUD via Service ============ */
//    private void doInsertPatient() {
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//
//        if (fullName == null || dob == null || gender == null) {
//            showWarn("Validation", "Full name, gender and date of birth are required.");
//            return;
//        }
//        if (phone == null) {
//            showWarn("Validation", "Patient must have a phone number.");
//            return;
//        }
//
//        try {
//            var pv = patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
//            patientData.add(new PatientRow(
//                    pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
//                    pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
//            ));
//            clearForm();
//            showInfo("Insert", "Patient inserted successfully.");
//        } catch (Exception ex) {
//            showError("Insert Patient", ex);
//        }
//    }
//
//    private void doUpdatePatient() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) {
//            showWarn("Update", "Select a patient row first.");
//            return;
//        }
//
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//
//        if (fullName == null || dob == null || gender == null) {
//            showWarn("Validation", "Full name, gender and date of birth are required.");
//            return;
//        }
//
//        try {
//            patientService.updatePatient(row.getUserId(), row.getPatientId(),
//                    fullName, nid, phone, dob, gender.name(), history);
//
//            row.setFullName(fullName);
//            row.setNationalId(nid);
//            row.setPhone(phone);
//            row.setDateOfBirth(dob);
//            row.setGender(gender.name());
//            row.setMedicalHistory(history);
//            patientTable.refresh();
//
//            showInfo("Update", "Patient updated successfully.");
//        } catch (Exception ex) {
//            showError("Update Patient", ex);
//        }
//    }
//
//    private void doDeletePatient() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) {
//            showWarn("Delete", "Select a patient row first.");
//            return;
//        }
//        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;
//
//        try {
//            patientService.deletePatientByUserId(row.getUserId());
//            patientData.remove(row);
//            clearForm();
//            showInfo("Delete", "Patient deleted.");
//        } catch (Exception e) {
//            showError("Delete Patient", e);
//        }
//    }
//
//    private void clearForm() {
//        FullNameTextField.clear();
//        PatientIdTextField.clear();
//        PhoneTextField.clear();
//        medicalHistory.clear();
//        GenderComboBox.setValue(Gender.MALE);
//        DateOfBirthPicker.setValue(null);
//        patientTable.getSelectionModel().clearSelection();
//    }
//
//    /* ============ Helpers ============ */
//    private boolean ensureOnlineOrAlert() {
//        if (monitor != null && !monitor.isOnline()) {
//            showWarn("Offline", "You're offline. Please reconnect and try again.");
//            return false;
//        }
//        return true;
//    }
//
//    private String trimOrNull(String s) {
//        if (s == null) return null;
//        String t = s.trim();
//        return t.isEmpty() ? null : t;
//    }
//
//    private void showError(String title, Exception ex) {
//        ex.printStackTrace();
//        Alert a = new Alert(Alert.AlertType.ERROR);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(ex.getMessage());
//        a.showAndWait();
//    }
//
//    private void showWarn(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.WARNING);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    private void showInfo(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.INFORMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    private boolean confirm(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
//    }
//
//    /* ============ Row model ============ */
//    public static class PatientRow {
//        private final LongProperty patientId = new SimpleLongProperty();
//        private final LongProperty userId    = new SimpleLongProperty();
//        private final StringProperty fullName = new SimpleStringProperty();
//        private final StringProperty nationalId = new SimpleStringProperty();
//        private final StringProperty phone = new SimpleStringProperty();
//        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
//        private final StringProperty gender = new SimpleStringProperty();
//        private final StringProperty medicalHistory = new SimpleStringProperty();
//
//        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
//                          String phone, LocalDate dob, String gender, String medicalHistory) {
//            setPatientId(patientId);
//            setUserId(userId);
//            setFullName(fullName);
//            setNationalId(nationalId);
//            setPhone(phone);
//            setDateOfBirth(dob);
//            setGender(gender);
//            setMedicalHistory(medicalHistory);
//        }
//
//        public long getPatientId() { return patientId.get(); }
//        public void setPatientId(long v) { patientId.set(v); }
//        public LongProperty patientIdProperty() { return patientId; }
//
//        public long getUserId() { return userId.get(); }
//        public void setUserId(long v) { userId.set(v); }
//        public LongProperty userIdProperty() { return userId; }
//
//        public String getFullName() { return fullName.get(); }
//        public void setFullName(String v) { fullName.set(v); }
//        public StringProperty fullNameProperty() { return fullName; }
//
//        public String getNationalId() { return nationalId.get(); }
//        public void setNationalId(String v) { nationalId.set(v); }
//        public StringProperty nationalIdProperty() { return nationalId; }
//
//        public String getPhone() { return phone.get(); }
//        public void setPhone(String v) { phone.set(v); }
//        public StringProperty phoneProperty() { return phone; }
//
//        public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
//        public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
//        public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }
//
//        public String getGender() { return gender.get(); }
//        public void setGender(String v) { gender.set(v); }
//        public StringProperty genderProperty() { return gender; }
//
//        public String getMedicalHistory() { return medicalHistory.get(); }
//        public void setMedicalHistory(String v) { medicalHistory.set(v); }
//        public StringProperty medicalHistoryProperty() { return medicalHistory; }
//    }
//}

//package com.example.healthflow.controllers;
//
//import com.example.healthflow.db.Database;
//import com.example.healthflow.net.ConnectivityMonitor;
//import com.example.healthflow.service.AuthService.Session;
//import com.example.healthflow.model.dto.PatientView;
//import com.example.healthflow.service.PatientService;
//import com.example.healthflow.ui.ConnectivityBanner;
//import com.example.healthflow.ui.OnlineBindings;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.beans.property.*;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.collections.transformation.FilteredList;
//import javafx.collections.transformation.SortedList;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.sql.*;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//
//public class ReceptionController {
//
//    /* ============ UI ============ */
//    @FXML private AnchorPane DashboardAnchorPane;
//    @FXML private AnchorPane PatientAnchorPane;
//    @FXML private AnchorPane AppointmentsAnchorPane;
//    @FXML private AnchorPane DoctorAnchorPane;
//    @FXML private VBox rootPane;
//
//    @FXML private Button DachboardButton;
//    @FXML private Button PatientsButton;
//    @FXML private Button AppointmentsButton;
//    @FXML private Button BackButton;
//    @FXML private Button DoctorsButton;
//
//    @FXML private Label DateOfDay;
//    @FXML private Label time;
//    @FXML private Label welcomeUser;
//
//    @FXML private Label UsernameLabel;
//    @FXML private Label UserIdLabel;
//
//    @FXML private TextField FullNameTextField;
//    @FXML private TextField PatientIdTextField;   // **National Id**
//    @FXML private ComboBox<Gender> GenderComboBox;
//    @FXML private DatePicker DateOfBirthPicker;
//    @FXML private TextField PhoneTextField;
//    @FXML private TextArea  medicalHistory;
//
//    @FXML private Button InsertButton;
//    @FXML private Button UpdateButton;
//    @FXML private Button deleteButton;
//    @FXML private Button clearBtn;
//
//    @FXML private TextField search;
//
//    @FXML private TableView<PatientRow> patientTable;
//    @FXML private TableColumn<PatientRow, String>    colNationalId;
//    @FXML private TableColumn<PatientRow, String>    colName;
//    @FXML private TableColumn<PatientRow, String>    colGender;
//    @FXML private TableColumn<PatientRow, LocalDate> colDob;
//    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
//    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;
//
//    @FXML private Label NumberOfTotalAppointments;
//    @FXML private Label NumberOfTotalDoctors;
//    @FXML private Label NumberOfTotalPatients;
//
//    /* ============ Types ============ */
//    public enum Gender { MALE, FEMALE }
//
//    /* ============ State ============ */
//    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
//    private FilteredList<PatientRow> filtered;
//    private final Navigation navigation = new Navigation();
//    private final PatientService patientService = new PatientService();
//
//    /* ============ Connectivity ============ */
//    private final ConnectivityMonitor monitor;
//
//    public ReceptionController(ConnectivityMonitor monitor) {
//        this.monitor = monitor;
//    }
//
//    // Default constructor for FXML loader
//    public ReceptionController() {
//        this(new ConnectivityMonitor());
//    }
//
//    /* ============ Init ============ */
//    @FXML
//    private void initialize() {
//        // Start connectivity monitor
//        monitor.start();
//
//        // Add connectivity banner at the top of the UI
//        if (rootPane != null) {
//            ConnectivityBanner banner = new ConnectivityBanner(monitor);
//            rootPane.getChildren().add(0, banner);
//        }
//
//        // Disable buttons when offline
//        OnlineBindings.disableWhenOffline(monitor,
//            InsertButton, UpdateButton, deleteButton, clearBtn,
//            DachboardButton, PatientsButton, AppointmentsButton
//        );
//
//        // تنقل
//        DachboardButton.setOnAction(e -> showDashboardPane());
//        PatientsButton.setOnAction(e -> showPatientsPane());
//        AppointmentsButton.setOnAction(e -> showAppointmentPane());
//        DoctorsButton.setOnAction(e -> showDoctorPane());
//        BackButton.setOnAction(e -> BackAction());
//
//        // ساعة وتاريخ
//        startClock();
//
//        // تحميل اسم المستخدم (الجلسة)
//        loadHeaderUser();
//
//        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
//        GenderComboBox.setValue(Gender.MALE);
//
//        DateOfBirthPicker.setValue(null);
//
//        // جدول + تحميل + بحث
//        wirePatientTable();
//        loadPatients();
//        wireSearch();
//
//        // CRUD (توجّه للخدمة)
//        InsertButton.setOnAction(e -> doInsertPatient());
//        UpdateButton.setOnAction(e -> doUpdatePatient());
//        deleteButton.setOnAction(e -> doDeletePatient());
//        clearBtn.setOnAction(e -> clearForm());
//
//        // ابدأ بالداشبورد
//        showDashboardPane();
//    }
//
//    /* ============ Clock (12h) ============ */
//    private void startClock() {
//        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
//        Timeline tl = new Timeline(
//                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
//                new KeyFrame(Duration.seconds(1))
//        );
//        tl.setCycleCount(Timeline.INDEFINITE);
//        tl.play();
//
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM:dd:yyyy");
//        DateOfDay.setText(LocalDate.now().format(df));
//    }
//
//    /* ============ Load header user ============ */
//    private void loadHeaderUser() {
//        var u = Session.get();   // Get current user from session
//        if (u == null) return;
//
//        // لو بدك اسم من DB (اختياري)—ابقيناه كما هو:
//        String sql = "SELECT id, full_name FROM users WHERE id = ?";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, u.getId());
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    long id = rs.getLong("id");
//                    String fullName = rs.getString("full_name");
//                    UsernameLabel.setText(fullName);
//                    UserIdLabel.setText(Long.toString(id));
//                    welcomeUser.setText(firstName(fullName));
//                    return;
//                }
//            }
//        } catch (SQLException ignore) {
//            // fallback
//        }
//
//        UsernameLabel.setText(u.getFullName());
//        UserIdLabel.setText(String.valueOf(u.getId()));
//        welcomeUser.setText(firstName(u.getFullName()));
//    }
//
//    private String firstName(String full) {
//        if (full == null || full.isBlank()) return "";
//        return full.trim().split("\\s+")[0];
//    }
//
//    /* ============ Navigation ============ */
//    @FXML
//    private void BackAction() {
//        Stage stage = (Stage) BackButton.getScene().getWindow();
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
//            // Use the existing monitor instead of creating a new one
//            loader.setControllerFactory(type ->
//                    type == LoginController.class ? new LoginController(monitor) : null
//            );
//            Parent root = loader.load();
//            stage.setScene(new Scene(root));
//            stage.setResizable(false);
//            stage.show();
//        } catch (IOException e) {
//            showError("Navigation", e);
//        }
//    }
//
//    /* ============ Panes ============ */
//    private void showDashboardPane() {
//        DashboardAnchorPane.setVisible(true);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showPatientsPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(true);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showAppointmentPane(){
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(true);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showDoctorPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(true);
//    }
//
//    /* ============ Table & Search ============ */
//    private void wirePatientTable() {
//        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
//        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
//        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
//        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
//        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
//        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());
//
//        patientTable.setItems(patientData);
//
//        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
//            if (row == null) return;
//            FullNameTextField.setText(row.getFullName());
//            PatientIdTextField.setText(row.getNationalId());
//            PhoneTextField.setText(row.getPhone());
//            medicalHistory.setText(row.getMedicalHistory());
//            DateOfBirthPicker.setValue(row.getDateOfBirth());
//            GenderComboBox.setValue("MALE".equals(row.getGender()) ? Gender.MALE : Gender.FEMALE);
//        });
//    }
//
//    private void wireSearch() {
//        filtered = new FilteredList<>(patientData, p -> true);
//        search.textProperty().addListener((obs, old, q) -> {
//            String s = (q == null) ? "" : q.trim().toLowerCase();
//            if (s.isEmpty()) filtered.setPredicate(p -> true);
//            else filtered.setPredicate(p ->
//                    contains(p.getFullName(), s) ||
//                            contains(p.getGender(), s) ||
//                            contains(p.getPhone(), s) ||
//                            contains(p.getNationalId(), s) ||
//                            contains(p.getMedicalHistory(), s) ||
//                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s))
//            );
//        });
//        SortedList<PatientRow> sorted = new SortedList<>(filtered);
//        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
//        patientTable.setItems(sorted);
//    }
//
//    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }
//
//    /* ============ Load Patients via Service ============ */
//    private void loadPatients() {
//        patientData.clear();
//        try {
//            for (PatientView pv : patientService.listPatients()) {
//                patientData.add(new PatientRow(
//                        pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
//                        pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
//                ));
//            }
//        } catch (Exception ex) {
//            showError("Load Patients", ex);
//        }
//    }
//
//    /* ============ CRUD via Service ============ */
//
//    private void doInsertPatient() {
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//
//        if (fullName == null || dob == null || gender == null) {
//            showWarn("Validation", "Full name, gender and date of birth are required.");
//            return;
//        }
//        if (phone == null) {
//            showWarn("Validation", "Patient must have a phone number.");
//            return;
//        }
//
//        try {
//            var pv = patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
//            patientData.add(new PatientRow(
//                    pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
//                    pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
//            ));
//            clearForm();
//            showInfo("Insert", "Patient inserted successfully.");
//        } catch (Exception ex) {
//            showError("Insert Patient", ex);
//        }
//    }
//
//    private void doUpdatePatient() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) {
//            showWarn("Update", "Select a patient row first.");
//            return;
//        }
//
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//
//        if (fullName == null || dob == null || gender == null) {
//            showWarn("Validation", "Full name, gender and date of birth are required.");
//            return;
//        }
//
//        try {
//            patientService.updatePatient(row.getUserId(), row.getPatientId(),
//                    fullName, nid, phone, dob, gender.name(), history);
//
//            row.setFullName(fullName);
//            row.setNationalId(nid);
//            row.setPhone(phone);
//            row.setDateOfBirth(dob);
//            row.setGender(gender.name());
//            row.setMedicalHistory(history);
//            patientTable.refresh();
//
//            showInfo("Update", "Patient updated successfully.");
//        } catch (Exception ex) {
//            showError("Update Patient", ex);
//        }
//    }
//
//    private void doDeletePatient() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) {
//            showWarn("Delete", "Select a patient row first.");
//            return;
//        }
//        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;
//
//        try {
//            patientService.deletePatientByUserId(row.getUserId());
//            patientData.remove(row);
//            clearForm();
//            showInfo("Delete", "Patient deleted.");
//        } catch (Exception e) {
//            showError("Delete Patient", e);
//        }
//    }
//
//    private void clearForm() {
//        FullNameTextField.clear();
//        PatientIdTextField.clear();
//        PhoneTextField.clear();
//        medicalHistory.clear();
//        GenderComboBox.setValue(Gender.MALE);
//        DateOfBirthPicker.setValue(null);
//        patientTable.getSelectionModel().clearSelection();
//    }
//
//    /* ============ Helpers ============ */
//    private String trimOrNull(String s) {
//        if (s == null) return null;
//        String t = s.trim();
//        return t.isEmpty() ? null : t;
//    }
//
//    private void showError(String title, Exception ex) {
//        ex.printStackTrace();
//        Alert a = new Alert(Alert.AlertType.ERROR);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(ex.getMessage());
//        a.showAndWait();
//    }
//
//    private void showWarn(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.WARNING);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    private void showInfo(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.INFORMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    private boolean confirm(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
//    }
//
//    /* ============ Row model ============ */
//    public static class PatientRow {
//        private final LongProperty patientId = new SimpleLongProperty();
//        private final LongProperty userId    = new SimpleLongProperty();
//        private final StringProperty fullName = new SimpleStringProperty();
//        private final StringProperty nationalId = new SimpleStringProperty();
//        private final StringProperty phone = new SimpleStringProperty();
//        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
//        private final StringProperty gender = new SimpleStringProperty();
//        private final StringProperty medicalHistory = new SimpleStringProperty();
//
//        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
//                          String phone, LocalDate dob, String gender, String medicalHistory) {
//            setPatientId(patientId);
//            setUserId(userId);
//            setFullName(fullName);
//            setNationalId(nationalId);
//            setPhone(phone);
//            setDateOfBirth(dob);
//            setGender(gender);
//            setMedicalHistory(medicalHistory);
//        }
//
//        public long getPatientId() { return patientId.get(); }
//        public void setPatientId(long v) { patientId.set(v); }
//        public LongProperty patientIdProperty() { return patientId; }
//
//        public long getUserId() { return userId.get(); }
//        public void setUserId(long v) { userId.set(v); }
//        public LongProperty userIdProperty() { return userId; }
//
//        public String getFullName() { return fullName.get(); }
//        public void setFullName(String v) { fullName.set(v); }
//        public StringProperty fullNameProperty() { return fullName; }
//
//        public String getNationalId() { return nationalId.get(); }
//        public void setNationalId(String v) { nationalId.set(v); }
//        public StringProperty nationalIdProperty() { return nationalId; }
//
//        public String getPhone() { return phone.get(); }
//        public void setPhone(String v) { phone.set(v); }
//        public StringProperty phoneProperty() { return phone; }
//
//        public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
//        public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
//        public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }
//
//        public String getGender() { return gender.get(); }
//        public void setGender(String v) { gender.set(v); }
//        public StringProperty genderProperty() { return gender; }
//
//        public String getMedicalHistory() { return medicalHistory.get(); }
//        public void setMedicalHistory(String v) { medicalHistory.set(v); }
//        public StringProperty medicalHistoryProperty() { return medicalHistory; }
//    }
//
//    /* ===========================================================
//       LEGACY JDBC CRUD (معلّق بناءً على طلبك — لا يُنفَّذ)
//       — هذا هو الكود القديم كما كان تقريبًا، تركناه كمراجع.
//       =========================================================== */
//
//    /*
//    private static final String DEFAULT_PATIENT_PASSWORD = "patient@123";
//
//    // OLD: loadPatients عبر SQL مباشر
//    private void loadPatients_OLD_JDBC() {
//        patientData.clear();
//        final String sql = \"\"\"
//            SELECT p.id             AS patient_id,
//                   u.id             AS user_id,
//                   u.full_name,
//                   u.national_id,
//                   u.phone,
//                   p.date_of_birth,
//                   p.gender,
//                   p.medical_history
//              FROM patients p
//              JOIN users u ON u.id = p.user_id
//             ORDER BY p.id
//        \"\"\";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//            while (rs.next()) {
//                patientData.add(new PatientRow(
//                        rs.getLong(\"patient_id\"),
//                        rs.getLong(\"user_id\"),
//                        rs.getString(\"full_name\"),
//                        rs.getString(\"national_id\"),
//                        rs.getString(\"phone\"),
//                        rs.getObject(\"date_of_birth\", LocalDate.class),
//                        rs.getString(\"gender\"),
//                        rs.getString(\"medical_history\")
//                ));
//            }
//        } catch (SQLException ex) { showError(\"Load Patients\", ex); }
//    }
//
//    // OLD: Insert
//    private void doInsertPatient_OLD_JDBC() {
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//
//        if (fullName == null || dob == null || gender == null) { showWarn(\"Validation\", \"Full name, gender and date of birth are required.\"); return; }
//        if (phone == null) { showWarn(\"Validation\", \"Patient must have a phone number.\"); return; }
//
//        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(DEFAULT_PATIENT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt());
//
//        String insUser = \"\"\"
//            INSERT INTO users (national_id, full_name, email, password_hash, role, phone)
//            VALUES (?,?,?,?, 'PATIENT', ?)
//            RETURNING id
//        \"\"\";
//
//        String insPatient = \"\"\"
//            INSERT INTO patients (user_id, date_of_birth, gender, medical_history)
//            VALUES (?, ?, ?::gender_type, ?)
//            RETURNING id
//        \"\"\";
//
//        try (Connection c = Database.get()) {
//            c.setAutoCommit(false);
//            try (PreparedStatement psU = c.prepareStatement(insUser);
//                 PreparedStatement psP = c.prepareStatement(insPatient)) {
//
//                psU.setString(1, nid);
//                psU.setString(2, fullName);
//                psU.setString(3, null);
//                psU.setString(4, hash);
//                psU.setString(5, phone);
//                Long userId;
//                try (ResultSet rs = psU.executeQuery()) { rs.next(); userId = rs.getLong(\"id\"); }
//
//                psP.setLong(1, userId);
//                psP.setObject(2, dob);
//                psP.setString(3, gender.name());
//                psP.setString(4, history);
//                Long patientId;
//                try (ResultSet rs2 = psP.executeQuery()) { rs2.next(); patientId = rs2.getLong(\"id\"); }
//
//                c.commit();
//                patientData.add(new PatientRow(patientId, userId, fullName, nid, phone, dob, gender.name(), history));
//                clearForm();
//                showInfo(\"Insert\", \"Patient inserted successfully.\");
//            } catch (Exception ex) { c.rollback(); showError(\"Insert Patient\", ex); }
//            finally { c.setAutoCommit(true); }
//        } catch (SQLException e) { showError(\"Insert Patient\", e); }
//    }
//
//    // OLD: Update
//    private void doUpdatePatient_OLD_JDBC() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) { showWarn(\"Update\", \"Select a patient row first.\"); return; }
//
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//
//        if (fullName == null || dob == null || gender == null) { showWarn(\"Validation\", \"Full name, gender and date of birth are required.\"); return; }
//
//        String updUser = \"\"\"
//            UPDATE users SET full_name=?, phone=?, national_id=?, updated_at=NOW() WHERE id=?
//        \"\"\";
//        String updPatient = \"\"\"
//            UPDATE patients SET date_of_birth=?, gender=?::gender_type, medical_history=?, updated_at=NOW() WHERE id=?
//        \"\"\";
//
//        try (Connection c = Database.get()) {
//            c.setAutoCommit(false);
//            try (PreparedStatement psU = c.prepareStatement(updUser);
//                 PreparedStatement psP = c.prepareStatement(updPatient)) {
//
//                psU.setString(1, fullName);
//                psU.setString(2, phone);
//                psU.setString(3, nid);
//                psU.setLong(4, row.getUserId());
//                psU.executeUpdate();
//
//                psP.setObject(1, dob);
//                psP.setString(2, gender.name());
//                psP.setString(3, history);
//                psP.setLong(4, row.getPatientId());
//                psP.executeUpdate();
//
//                c.commit();
//
//                row.setFullName(fullName);
//                row.setNationalId(nid);
//                row.setPhone(phone);
//                row.setDateOfBirth(dob);
//                row.setGender(gender.name());
//                row.setMedicalHistory(history);
//                patientTable.refresh();
//                showInfo(\"Update\", \"Patient updated successfully.\");
//            } catch (Exception ex) { c.rollback(); showError(\"Update Patient\", ex); }
//            finally { c.setAutoCommit(true); }
//        } catch (SQLException e) { showError(\"Update Patient\", e); }
//    }
//
//    // OLD: Delete
//    private void doDeletePatient_OLD_JDBC() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) { showWarn(\"Delete\", \"Select a patient row first.\"); return; }
//        if (!confirm(\"Delete\", \"Are you sure you want to delete this patient?\")) return;
//
//        String del = \"DELETE FROM users WHERE id = ?\";
//        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(del)) {
//            ps.setLong(1, row.getUserId());
//            int n = ps.executeUpdate();
//            if (n == 1) {
//                patientData.remove(row);
//                clearForm();
//                showInfo(\"Delete\", \"Patient deleted.\");
//            } else {
//                showWarn(\"Delete\", \"Nothing deleted.\");
//            }
//        } catch (SQLException e) { showError(\"Delete Patient\", e); }
//    }
//    */
//
//}


//
//package com.example.healthflow.controllers;
//
//import com.example.healthflow.db.Database;
//import com.example.healthflow.net.ConnectivityMonitor;
//import com.example.healthflow.service.AuthService.Session;
//import com.example.healthflow.model.dto.PatientView;
//import com.example.healthflow.service.PatientService;
//import com.example.healthflow.ui.ConnectivityBanner;
//import com.example.healthflow.ui.OnlineBindings;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.beans.property.*;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.collections.transformation.FilteredList;
//import javafx.collections.transformation.SortedList;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.sql.*;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//
//public class ReceptionController {
//
//    /* ============ UI ============ */
//    @FXML private AnchorPane DashboardAnchorPane;
//    @FXML private AnchorPane PatientAnchorPane;
//    @FXML private AnchorPane AppointmentsAnchorPane;
//    @FXML private AnchorPane DoctorAnchorPane;
//    @FXML private VBox rootPane;
//
//    @FXML private Button DachboardButton;
//    @FXML private Button PatientsButton;
//    @FXML private Button AppointmentsButton;
//    @FXML private Button BackButton;
//    @FXML private Button DoctorsButton;
//
//    @FXML private Label DateOfDay;
//    @FXML private Label time;
//    @FXML private Label welcomeUser;
//
//    @FXML private Label UsernameLabel;
//    @FXML private Label UserIdLabel;
//
//    @FXML private TextField FullNameTextField;
//    @FXML private TextField PatientIdTextField;   // **National Id**
//    @FXML private ComboBox<Gender> GenderComboBox;
//    @FXML private DatePicker DateOfBirthPicker;
//    @FXML private TextField PhoneTextField;
//    @FXML private TextArea  medicalHistory;
//
//    @FXML private Button InsertButton;
//    @FXML private Button UpdateButton;
//    @FXML private Button deleteButton;
//    @FXML private Button clearBtn;
//
//    @FXML private TextField search;
//
//    @FXML private TableView<PatientRow> patientTable;
//    @FXML private TableColumn<PatientRow, String>    colNationalId;
//    @FXML private TableColumn<PatientRow, String>    colName;
//    @FXML private TableColumn<PatientRow, String>    colGender;
//    @FXML private TableColumn<PatientRow, LocalDate> colDob;
//    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
//    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;
//
//    @FXML private Label NumberOfTotalAppointments;
//    @FXML private Label NumberOfTotalDoctors;
//    @FXML private Label NumberOfTotalPatients;
//
//    /* ============ Types ============ */
//    public enum Gender { MALE, FEMALE }
//
//    /* ============ State ============ */
//    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
//    private FilteredList<PatientRow> filtered;
//    private final Navigation navigation = new Navigation();
//    private final PatientService patientService = new PatientService();
//
//    /* ============ Connectivity ============ */
//    private final ConnectivityMonitor monitor;
//    private boolean offlineAlertShown = false; // لإظهار تنبيه واحد فقط وقت الانقطاع
//
//    public ReceptionController(ConnectivityMonitor monitor) {
//        this.monitor = monitor;
//    }
//
//    // Default constructor for FXML loader
//    public ReceptionController() {
//        this(new ConnectivityMonitor());
//    }
//
//    /* ============ Init ============ */
//    @FXML
//    private void initialize() {
//        // Start connectivity monitor
//        monitor.start();
//
//        // Add connectivity banner at the top of the UI
//        if (rootPane != null) {
//            ConnectivityBanner banner = new ConnectivityBanner(monitor);
//            rootPane.getChildren().add(0, banner);
//        }
//
//        // Disable buttons when offline (أضفنا DoctorsButton)
//        OnlineBindings.disableWhenOffline(
//                monitor,
//                InsertButton, UpdateButton, deleteButton, clearBtn,
//                DachboardButton, PatientsButton, AppointmentsButton,
//                DoctorsButton
//        );
//
//        // Alerts on connectivity changes
//        monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
//            if (!isOnline && !offlineAlertShown) {
//                offlineAlertShown = true;
//                showWarn("Offline", "No internet connection. Some actions are disabled.");
//            } else if (isOnline) {
//                offlineAlertShown = false;
//                showInfo("Back online", "Connection restored.");
//            }
//        });
//
//        // تنقل
//        DachboardButton.setOnAction(e -> showDashboardPane());
//        PatientsButton.setOnAction(e -> showPatientsPane());
//        AppointmentsButton.setOnAction(e -> showAppointmentPane());
//        DoctorsButton.setOnAction(e -> showDoctorPane());
//        BackButton.setOnAction(e -> BackAction());
//
//        // ساعة وتاريخ
//        startClock();
//
//        // تحميل اسم المستخدم (الجلسة)
//        loadHeaderUser();
//
//        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
//        GenderComboBox.setValue(Gender.MALE);
//
//        DateOfBirthPicker.setValue(null);
//
//        // جدول + تحميل + بحث
//        wirePatientTable();
//        loadPatients();
//        wireSearch();
//
//        // CRUD via service + تحقق من الاتصال قبل التنفيذ
//        InsertButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doInsertPatient(); });
//        UpdateButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doUpdatePatient(); });
//        deleteButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doDeletePatient(); });
//        clearBtn.setOnAction(e -> clearForm());
//
//        // ابدأ بالداشبورد
//        showDashboardPane();
//    }
//
//    /* ============ Clock (12h) ============ */
//    private void startClock() {
//        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
//        Timeline tl = new Timeline(
//                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
//                new KeyFrame(Duration.seconds(1))
//        );
//        tl.setCycleCount(Timeline.INDEFINITE);
//        tl.play();
//
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM:dd:yyyy");
//        DateOfDay.setText(LocalDate.now().format(df));
//    }
//
//    /* ============ Load header user ============ */
//    private void loadHeaderUser() {
//        var u = Session.get();   // Get current user from session
//        if (u == null) return;
//
//        // (اختياري) جلب الاسم من DB
//        String sql = "SELECT id, full_name FROM users WHERE id = ?";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, u.getId());
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    long id = rs.getLong("id");
//                    String fullName = rs.getString("full_name");
//                    UsernameLabel.setText(fullName);
//                    UserIdLabel.setText(Long.toString(id));
//                    welcomeUser.setText(firstName(fullName));
//                    return;
//                }
//            }
//        } catch (SQLException ignore) {
//            // fallback
//        }
//
//        UsernameLabel.setText(u.getFullName());
//        UserIdLabel.setText(String.valueOf(u.getId()));
//        welcomeUser.setText(firstName(u.getFullName()));
//    }
//
//    private String firstName(String full) {
//        if (full == null || full.isBlank()) return "";
//        return full.trim().split("\\s+")[0];
//    }
//
//    /* ============ Navigation ============ */
//    @FXML
//    private void BackAction() {
//        Stage stage = (Stage) BackButton.getScene().getWindow();
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
//            loader.setControllerFactory(type ->
//                    type == LoginController.class ? new LoginController(monitor) : null
//            );
//            Parent root = loader.load();
//            stage.setScene(new Scene(root));
//            stage.setResizable(false);
//            stage.show();
//        } catch (IOException e) {
//            showError("Navigation", e);
//        }
//    }
//
//    /* ============ Panes ============ */
//    private void showDashboardPane() {
//        DashboardAnchorPane.setVisible(true);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showPatientsPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(true);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showAppointmentPane(){
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(true);
//        DoctorAnchorPane.setVisible(false);
//    }
//    private void showDoctorPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(false);
//        DoctorAnchorPane.setVisible(true);
//    }
//
//    /* ============ Table & Search ============ */
//    private void wirePatientTable() {
//        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
//        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
//        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
//        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
//        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
//        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());
//
//        patientTable.setItems(patientData);
//
//        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
//            if (row == null) return;
//            FullNameTextField.setText(row.getFullName());
//            PatientIdTextField.setText(row.getNationalId());
//            PhoneTextField.setText(row.getPhone());
//            medicalHistory.setText(row.getMedicalHistory());
//            DateOfBirthPicker.setValue(row.getDateOfBirth());
//            GenderComboBox.setValue("MALE".equals(row.getGender()) ? Gender.MALE : Gender.FEMALE);
//        });
//    }
//
//    private void wireSearch() {
//        filtered = new FilteredList<>(patientData, p -> true);
//        search.textProperty().addListener((obs, old, q) -> {
//            String s = (q == null) ? "" : q.trim().toLowerCase();
//            if (s.isEmpty()) filtered.setPredicate(p -> true);
//            else filtered.setPredicate(p ->
//                    contains(p.getFullName(), s) ||
//                            contains(p.getGender(), s) ||
//                            contains(p.getPhone(), s) ||
//                            contains(p.getNationalId(), s) ||
//                            contains(p.getMedicalHistory(), s) ||
//                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s))
//            );
//        });
//        SortedList<PatientRow> sorted = new SortedList<>(filtered);
//        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
//        patientTable.setItems(sorted);
//    }
//
//    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }
//
//    /* ============ Load Patients via Service ============ */
//    private void loadPatients() {
//        patientData.clear();
//        try {
//            for (PatientView pv : patientService.listPatients()) {
//                patientData.add(new PatientRow(
//                        pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
//                        pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
//                ));
//            }
//        } catch (Exception ex) {
//            showError("Load Patients", ex);
//        }
//    }
//
//    /* ============ CRUD via Service ============ */
//    private void doInsertPatient() {
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//
//        if (fullName == null || dob == null || gender == null) {
//            showWarn("Validation", "Full name, gender and date of birth are required.");
//            return;
//        }
//        if (phone == null) {
//            showWarn("Validation", "Patient must have a phone number.");
//            return;
//        }
//
//        try {
//            var pv = patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
//            patientData.add(new PatientRow(
//                    pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
//                    pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
//            ));
//            clearForm();
//            showInfo("Insert", "Patient inserted successfully.");
//        } catch (Exception ex) {
//            showError("Insert Patient", ex);
//        }
//    }
//
//    private void doUpdatePatient() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) {
//            showWarn("Update", "Select a patient row first.");
//            return;
//        }
//
//        String fullName = trimOrNull(FullNameTextField.getText());
//        String nid      = trimOrNull(PatientIdTextField.getText());
//        String phone    = trimOrNull(PhoneTextField.getText());
//        String history  = trimOrNull(medicalHistory.getText());
//        Gender gender   = GenderComboBox.getValue();
//        LocalDate dob   = DateOfBirthPicker.getValue();
//
//        if (fullName == null || dob == null || gender == null) {
//            showWarn("Validation", "Full name, gender and date of birth are required.");
//            return;
//        }
//
//        try {
//            patientService.updatePatient(row.getUserId(), row.getPatientId(),
//                    fullName, nid, phone, dob, gender.name(), history);
//
//            row.setFullName(fullName);
//            row.setNationalId(nid);
//            row.setPhone(phone);
//            row.setDateOfBirth(dob);
//            row.setGender(gender.name());
//            row.setMedicalHistory(history);
//            patientTable.refresh();
//
//            showInfo("Update", "Patient updated successfully.");
//        } catch (Exception ex) {
//            showError("Update Patient", ex);
//        }
//    }
//
//    private void doDeletePatient() {
//        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
//        if (row == null) {
//            showWarn("Delete", "Select a patient row first.");
//            return;
//        }
//        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;
//
//        try {
//            patientService.deletePatientByUserId(row.getUserId());
//            patientData.remove(row);
//            clearForm();
//            showInfo("Delete", "Patient deleted.");
//        } catch (Exception e) {
//            showError("Delete Patient", e);
//        }
//    }
//
//    private void clearForm() {
//        FullNameTextField.clear();
//        PatientIdTextField.clear();
//        PhoneTextField.clear();
//        medicalHistory.clear();
//        GenderComboBox.setValue(Gender.MALE);
//        DateOfBirthPicker.setValue(null);
//        patientTable.getSelectionModel().clearSelection();
//    }
//
//    /* ============ Helpers ============ */
//    private boolean ensureOnlineOrAlert() {
//        if (monitor != null && !monitor.isOnline()) {
//            showWarn("Offline", "You're offline. Please reconnect and try again.");
//            return false;
//        }
//        return true;
//    }
//
//    private String trimOrNull(String s) {
//        if (s == null) return null;
//        String t = s.trim();
//        return t.isEmpty() ? null : t;
//    }
//
//    private void showError(String title, Exception ex) {
//        ex.printStackTrace();
//        Alert a = new Alert(Alert.AlertType.ERROR);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(ex.getMessage());
//        a.showAndWait();
//    }
//
//    private void showWarn(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.WARNING);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    private void showInfo(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.INFORMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    private boolean confirm(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
//    }
//
//    /* ============ Row model ============ */
//    public static class PatientRow {
//        private final LongProperty patientId = new SimpleLongProperty();
//        private final LongProperty userId    = new SimpleLongProperty();
//        private final StringProperty fullName = new SimpleStringProperty();
//        private final StringProperty nationalId = new SimpleStringProperty();
//        private final StringProperty phone = new SimpleStringProperty();
//        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
//        private final StringProperty gender = new SimpleStringProperty();
//        private final StringProperty medicalHistory = new SimpleStringProperty();
//
//        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
//                          String phone, LocalDate dob, String gender, String medicalHistory) {
//            setPatientId(patientId);
//            setUserId(userId);
//            setFullName(fullName);
//            setNationalId(nationalId);
//            setPhone(phone);
//            setDateOfBirth(dob);
//            setGender(gender);
//            setMedicalHistory(medicalHistory);
//        }
//
//        public long getPatientId() { return patientId.get(); }
//        public void setPatientId(long v) { patientId.set(v); }
//        public LongProperty patientIdProperty() { return patientId; }
//
//        public long getUserId() { return userId.get(); }
//        public void setUserId(long v) { userId.set(v); }
//        public LongProperty userIdProperty() { return userId; }
//
//        public String getFullName() { return fullName.get(); }
//        public void setFullName(String v) { fullName.set(v); }
//        public StringProperty fullNameProperty() { return fullName; }
//
//        public String getNationalId() { return nationalId.get(); }
//        public void setNationalId(String v) { nationalId.set(v); }
//        public StringProperty nationalIdProperty() { return nationalId; }
//
//        public String getPhone() { return phone.get(); }
//        public void setPhone(String v) { phone.set(v); }
//        public StringProperty phoneProperty() { return phone; }
//
//        public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
//        public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
//        public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }
//
//        public String getGender() { return gender.get(); }
//        public void setGender(String v) { gender.set(v); }
//        public StringProperty genderProperty() { return gender; }
//
//        public String getMedicalHistory() { return medicalHistory.get(); }
//        public void setMedicalHistory(String v) { medicalHistory.set(v); }
//        public StringProperty medicalHistoryProperty() { return medicalHistory; }
//    }
//}
