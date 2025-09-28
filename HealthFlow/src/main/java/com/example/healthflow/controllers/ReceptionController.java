package com.example.healthflow.controllers;

import com.example.healthflow.db.Database;
import com.example.healthflow.net.ConnectivityMonitor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class ReceptionController {

    /* ============ UI ============ */
    @FXML private AnchorPane DashboardAnchorPane;
    @FXML private AnchorPane PatientAnchorPane;
    @FXML private AnchorPane AppointmentsAnchorPane;

    @FXML private Button DachboardButton;
    @FXML private Button PatientsButton;
    @FXML private Button AppointmentsButton;
    @FXML private Button BackButton;

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
    @FXML private TableColumn<PatientRow, String>    colNationalId;   // يعرض National ID
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

    /* ============ Init ============ */
    @FXML
    private void initialize() {
        // تنقل
        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPane());
        AppointmentsButton.setOnAction(e -> showAppointmentPane());
        BackButton.setOnAction(e -> BackAction());

        // ساعة وتاريخ
        startClock();

        // تحميل اسم المستخدم (جلسة + DB)
        loadHeaderUser();

        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
        GenderComboBox.setValue(Gender.MALE);

        DateOfBirthPicker.setValue(null);

        // جدول + تحميل + بحث
        wirePatientTable();
        loadPatients();
        wireSearch();

        // CRUD
        InsertButton.setOnAction(e -> doInsertPatient());
        UpdateButton.setOnAction(e -> doUpdatePatient());
        deleteButton.setOnAction(e -> doDeletePatient());
        clearBtn.setOnAction(e -> clearForm());

        // ابدأ بالداشبورد
        showDashboardPane();
    }

    /* ============ Clock (12h) ============ */
    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a"); // 12h
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
                new KeyFrame(Duration.seconds(1))
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM:dd:yyyy");
        DateOfDay.setText(LocalDate.now().format(df));
    }

    /* ============ Load header user ============ */
    private void loadHeaderUser() {
        var u = com.example.healthflow.service.AuthService.Session.get();
        if (u == null) return;

        String sql = "SELECT id, full_name FROM users WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String fullName = rs.getString("full_name");
                    UsernameLabel.setText(fullName);
                    UserIdLabel.setText(Long.toString(id));
                    welcomeUser.setText(firstName(fullName));
                    return;
                }
            }
        } catch (SQLException ignore) {
            // fallback للجلسة
        }

        UsernameLabel.setText(u.getFullName());
        UserIdLabel.setText(String.valueOf(u.getId()));
        welcomeUser.setText(firstName(u.getFullName()));
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
                    type == LoginController.class ? new LoginController(new ConnectivityMonitor()) : null
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

    /* ============ Table & Search ============ */
    private void wirePatientTable() {
        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty()); // National ID من users
        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());

        patientTable.setItems(patientData);

        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null) return;
            FullNameTextField.setText(row.getFullName());
            PatientIdTextField.setText(row.getNationalId());  // عرض National ID في الحقل
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
            if (s.isEmpty()) {
                filtered.setPredicate(p -> true);
            } else {
                filtered.setPredicate(p ->
                        contains(p.getFullName(), s) ||
                                contains(p.getGender(), s) ||
                                contains(p.getPhone(), s) ||
                                contains(p.getNationalId(), s) ||
                                contains(p.getMedicalHistory(), s) ||
                                (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s))
                );
            }
        });
        SortedList<PatientRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        patientTable.setItems(sorted);
    }

    private boolean contains(String v, String q) {
        return v != null && v.toLowerCase().contains(q);
    }

    /* ============ Load Patients ============ */
    private void loadPatients() {
        patientData.clear();
        final String sql = """
            SELECT p.id             AS patient_id,
                   u.id             AS user_id,
                   u.full_name,
                   u.national_id,
                   u.phone,
                   p.date_of_birth,
                   p.gender,
                   p.medical_history
              FROM patients p
              JOIN users u ON u.id = p.user_id
             ORDER BY p.id
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                patientData.add(new PatientRow(
                        rs.getLong("patient_id"),
                        rs.getLong("user_id"),
                        rs.getString("full_name"),
                        rs.getString("national_id"),
                        rs.getString("phone"),
                        rs.getObject("date_of_birth", LocalDate.class),
                        rs.getString("gender"),
                        rs.getString("medical_history")
                ));
            }
        } catch (SQLException ex) {
            showError("Load Patients", ex);
        }
    }

    /* ============ CRUD ============ */

    private static final String DEFAULT_PATIENT_PASSWORD = "patient@123";

    private void doInsertPatient() {
        String fullName = trimOrNull(FullNameTextField.getText());
        String nid      = trimOrNull(PatientIdTextField.getText());  // National Id
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

        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                DEFAULT_PATIENT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt()
        );

        String insUser = """
            INSERT INTO users (national_id, full_name, email, password_hash, role, phone)
            VALUES (?,?,?,?, 'PATIENT', ?)
            RETURNING id
        """;
        String insPatient = """
            INSERT INTO patients (user_id, date_of_birth, gender, medical_history)
            VALUES (?, ?, ?::gender_type, ?)
            RETURNING id
        """;

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement psU = c.prepareStatement(insUser);
                 PreparedStatement psP = c.prepareStatement(insPatient)) {

                psU.setString(1, nid);
                psU.setString(2, fullName);
                psU.setString(3, null);  // email اختياري
                psU.setString(4, hash);
                psU.setString(5, phone);
                Long userId;
                try (ResultSet rs = psU.executeQuery()) {
                    rs.next();
                    userId = rs.getLong("id");
                }

                psP.setLong(1, userId);
                psP.setObject(2, dob);
                psP.setString(3, gender.name());
                psP.setString(4, history);
                Long patientId;
                try (ResultSet rs2 = psP.executeQuery()) {
                    rs2.next();
                    patientId = rs2.getLong("id");
                }

                c.commit();
                patientData.add(new PatientRow(
                        patientId, userId, fullName, nid, phone, dob, gender.name(), history
                ));
                clearForm();
                showInfo("Insert", "Patient inserted successfully.");
            } catch (Exception ex) {
                c.rollback();
                showError("Insert Patient", ex);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            showError("Insert Patient", e);
        }
    }

    private void doUpdatePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Update", "Select a patient row first.");
            return;
        }

        String fullName = trimOrNull(FullNameTextField.getText());
        String nid      = trimOrNull(PatientIdTextField.getText());   // National Id
        String phone    = trimOrNull(PhoneTextField.getText());
        String history  = trimOrNull(medicalHistory.getText());
        Gender gender   = GenderComboBox.getValue();
        LocalDate dob   = DateOfBirthPicker.getValue();

        if (fullName == null || dob == null || gender == null) {
            showWarn("Validation", "Full name, gender and date of birth are required.");
            return;
        }

        String updUser = """
            UPDATE users
               SET full_name = ?, phone = ?, national_id = ?, updated_at = NOW()
             WHERE id = ?
        """;

        String updPatient = """
            UPDATE patients
               SET date_of_birth = ?, gender = ?::gender_type, medical_history = ?, updated_at = NOW()
             WHERE id = ?
        """;

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement psU = c.prepareStatement(updUser);
                 PreparedStatement psP = c.prepareStatement(updPatient)) {

                psU.setString(1, fullName);
                psU.setString(2, phone);
                psU.setString(3, nid);
                psU.setLong(4, row.getUserId());
                psU.executeUpdate();

                psP.setObject(1, dob);
                psP.setString(2, gender.name());
                psP.setString(3, history);
                psP.setLong(4, row.getPatientId());
                psP.executeUpdate();

                c.commit();

                // حدّث الصف المعروض
                row.setFullName(fullName);
                row.setNationalId(nid);
                row.setPhone(phone);
                row.setDateOfBirth(dob);
                row.setGender(gender.name());
                row.setMedicalHistory(history);
                patientTable.refresh();

                showInfo("Update", "Patient updated successfully.");
            } catch (Exception ex) {
                c.rollback();
                showError("Update Patient", ex);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            showError("Update Patient", e);
        }
    }

    private void doDeletePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Delete", "Select a patient row first.");
            return;
        }
        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;

        // حذف المستخدم يكفي لأن patients مربوط بـ users
        String del = "DELETE FROM users WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(del)) {
            ps.setLong(1, row.getUserId());
            int n = ps.executeUpdate();
            if (n == 1) {
                patientData.remove(row);
                clearForm();
                showInfo("Delete", "Patient deleted.");
            } else {
                showWarn("Delete", "Nothing deleted.");
            }
        } catch (SQLException e) {
            showError("Delete Patient", e);
        }
    }

    private void clearForm() {
        FullNameTextField.clear();
        PatientIdTextField.clear();   // National ID input
        PhoneTextField.clear();
        medicalHistory.clear();
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);
        patientTable.getSelectionModel().clearSelection();
    }

    /* ============ Helpers ============ */
    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void showError(String title, Exception ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex.getMessage());
        a.showAndWait();
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
        private final LongProperty patientId = new SimpleLongProperty(); // من جدول patients
        private final LongProperty userId    = new SimpleLongProperty(); // من جدول users
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty nationalId = new SimpleStringProperty(); // **National ID**
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
//import javafx.scene.control.cell.PropertyValueFactory;
//import javafx.scene.layout.AnchorPane;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.sql.*;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//
///**
// * Reception Controller
// * - Clock 12h
// * - Header user name/ID
// * - Patients CRUD (users + patients)
// */
//public class ReceptionController {
//
//    /* ============ UI ============ */
//    @FXML private AnchorPane DashboardAnchorPane;
//    @FXML private AnchorPane PatientAnchorPane;
//    @FXML private AnchorPane AppointmentsAnchorPane;
//
//    @FXML private Button DachboardButton;
//    @FXML private Button PatientsButton;
//    @FXML private Button AppointmentsButton;
//    @FXML private Button BackButton;
//
//    // أعلى الشاشة
//    @FXML private Label DateOfDay;
//    @FXML private Label time;
//    @FXML private Label welcomeUser;      // الاسم الأول
//
//    // الشريط الجانبي
//    @FXML private Label UsernameLabel;    // الاسم الكامل
//    @FXML private Label UserIdLabel;      // رقم المستخدم
//
//    // نموذج المريض
//    @FXML private TextField FullNameTextField;
//    @FXML private TextField PatientIdTextField;   // national id (اختياري)
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
//    // البحث
//    @FXML private TextField search;
//
//    // جدول المرضى (بدون Address)
//    @FXML private TableView<PatientRow> patientTable;
//    @FXML private TableColumn<PatientRow, Long>      colPatientId;
//    @FXML private TableColumn<PatientRow, String>    colName;
//    @FXML private TableColumn<PatientRow, String>    colGender;
//    @FXML private TableColumn<PatientRow, LocalDate> colDob;
//    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
//    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;
//
//    // كروت الأرقام (لو لزم لاحقاً)
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
//
//    /* ============ Init ============ */
//    @FXML
//    private void initialize() {
//        // التنقل بين البانلز
//        DachboardButton.setOnAction(e -> showDashboardPane());
//        PatientsButton.setOnAction(e -> showPatientsPane());
//        AppointmentsButton.setOnAction(e -> showAppointmentPane());
//        BackButton.setOnAction(e -> BackAction());
//
//        // ساعة وتاريخ (12 ساعة)
//        startClock();
//
//        // تحميل اسم المستخدم في الهيدر/الجانب
//        loadHeaderUser();
//
//        // كومبوبوكس الجنس
//        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
//        GenderComboBox.setValue(Gender.MALE);
//
//        // تاريخ افتراضي
//        DateOfBirthPicker.setValue(null);
//
//        // جدول المرضى + تحميل البيانات + البحث
//        wirePatientTable();
//        loadPatients();
//        wireSearch();
//
//        // أزرار CRUD
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
//        // hh = 12-hour, a = AM/PM (يعتمد على لغة النظام)
//        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
//        Timeline tl = new Timeline(
//                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
//                new KeyFrame(Duration.seconds(1))
//        );
//        tl.setCycleCount(Timeline.INDEFINITE);
//        tl.play();
//
//        // التاريخ كما هو
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
//        DateOfDay.setText(LocalDate.now().format(df));
//    }
//
//    /* ============ Load header user ============ */
//    private void loadHeaderUser() {
//        // استرجاع المستخدم من الجلسة (عرّف Session كما في الرسالة السابقة)
//        var u = com.example.healthflow.service.AuthService.Session.get();
//        if (u == null) return;
//
//        // نحاول التحديث من DB لتكون أحدث قيمة
//        String sql = "SELECT id, full_name FROM users WHERE id = ?";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//
//            ps.setLong(1, u.getId());
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    long id = rs.getLong("id");
//                    String fullName = rs.getString("full_name");
//
//                    UsernameLabel.setText(fullName);                // الاسم الكامل في الجانب
//                    UserIdLabel.setText(Long.toString(id));         // ID
//                    welcomeUser.setText(firstName(fullName));       // الاسم الأول في الأعلى
//                    return;
//                }
//            }
//        } catch (SQLException ignore) {
//            // fallback للجلسة
//        }
//
//        // Fallback من الجلسة
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
//                    type == LoginController.class ? new LoginController(new ConnectivityMonitor()) : null
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
//    }
//    private void showPatientsPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(true);
//        AppointmentsAnchorPane.setVisible(false);
//    }
//    private void showAppointmentPane(){
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(false);
//        AppointmentsAnchorPane.setVisible(true);
//    }
//
//    /* ============ Table & Search ============ */
//    private void wirePatientTable() {
//        colPatientId.setCellValueFactory(cd -> cd.getValue().patientIdProperty().asObject());
//        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
//        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
//        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
//        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
//        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());
//
//        patientTable.setItems(patientData);
//
//        patientTable.setItems(patientData);
//
//        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
//            if (row == null) return;
//            FullNameTextField.setText(row.getFullName());
//            PatientIdTextField.setText(row.getNationalId()); // لو ما بدك الـNID احذف السطر
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
//            if (s.isEmpty()) {
//                filtered.setPredicate(p -> true);
//            } else {
//                filtered.setPredicate(p ->
//                        contains(p.getFullName(), s) ||
//                                contains(p.getNationalId(), s) ||
//                                contains(p.getGender(), s) ||
//                                contains(p.getPhone(), s) ||
//                                contains(p.getMedicalHistory(), s) ||
//                                (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s)) ||
//                                Long.toString(p.getPatientId()).contains(s)
//                );
//            }
//        });
//        SortedList<PatientRow> sorted = new SortedList<>(filtered);
//        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
//        patientTable.setItems(sorted);
//    }
//
//    private boolean contains(String v, String q) {
//        return v != null && v.toLowerCase().contains(q);
//    }
//
//    /* ============ Load Patients ============ */
//    private void loadPatients() {
//        patientData.clear();
//        final String sql = """
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
//        """;
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//            while (rs.next()) {
//                patientData.add(new PatientRow(
//                        rs.getLong("patient_id"),
//                        rs.getLong("user_id"),
//                        rs.getString("full_name"),
//                        rs.getString("national_id"),
//                        rs.getString("phone"),
//                        rs.getObject("date_of_birth", LocalDate.class),
//                        rs.getString("gender"),
//                        rs.getString("medical_history")
//                ));
//            }
//        } catch (SQLException ex) {
//            showError("Load Patients", ex);
//        }
//    }
//
//    /* ============ CRUD ============ */
//
//    private static final String DEFAULT_PATIENT_PASSWORD = "patient@123";
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
//        // الـ DB يفرض: المريض لازم يكون عنده Email أو Phone. اخترنا Phone هنا.
//        if (phone == null) {
//            showWarn("Validation", "Patient must have a phone number.");
//            return;
//        }
//
//        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
//                DEFAULT_PATIENT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt()
//        );
//
//        String insUser = """
//            INSERT INTO users (national_id, full_name, email, password_hash, role, phone)
//            VALUES (?,?,?,?, 'PATIENT', ?)
//            RETURNING id
//        """;
//        String insPatient = """
//            INSERT INTO patients (user_id, date_of_birth, gender, medical_history)
//            VALUES (?, ?, ?::gender_type, ?)
//            RETURNING id
//        """;
//
//        try (Connection c = Database.get()) {
//            c.setAutoCommit(false);
//            try (PreparedStatement psU = c.prepareStatement(insUser);
//                 PreparedStatement psP = c.prepareStatement(insPatient)) {
//
//                psU.setString(1, nid);
//                psU.setString(2, fullName);
//                psU.setString(3, null);  // email اختياري
//                psU.setString(4, hash);
//                psU.setString(5, phone);
//                Long userId;
//                try (ResultSet rs = psU.executeQuery()) {
//                    rs.next();
//                    userId = rs.getLong("id");
//                }
//
//                psP.setLong(1, userId);
//                psP.setObject(2, dob);
//                psP.setString(3, gender.name());
//                psP.setString(4, history);
//                Long patientId;
//                try (ResultSet rs2 = psP.executeQuery()) {
//                    rs2.next();
//                    patientId = rs2.getLong("id");
//                }
//
//                c.commit();
//                patientData.add(new PatientRow(
//                        patientId, userId, fullName, nid, phone, dob, gender.name(), history
//                ));
//                clearForm();
//                showInfo("Insert", "Patient inserted successfully.");
//            } catch (Exception ex) {
//                c.rollback();
//                showError("Insert Patient", ex);
//            } finally {
//                c.setAutoCommit(true);
//            }
//        } catch (SQLException e) {
//            showError("Insert Patient", e);
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
//        String updUser = """
//            UPDATE users
//               SET full_name = ?, phone = ?, national_id = ?, updated_at = NOW()
//             WHERE id = ?
//        """;
//
//        String updPatient = """
//            UPDATE patients
//               SET date_of_birth = ?, gender = ?::gender_type, medical_history = ?, updated_at = NOW()
//             WHERE id = ?
//        """;
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
//                // حدّث الصف المعروض
//                row.setFullName(fullName);
//                row.setNationalId(nid);
//                row.setPhone(phone);
//                row.setDateOfBirth(dob);
//                row.setGender(gender.name());
//                row.setMedicalHistory(history);
//                patientTable.refresh();
//
//                showInfo("Update", "Patient updated successfully.");
//            } catch (Exception ex) {
//                c.rollback();
//                showError("Update Patient", ex);
//            } finally {
//                c.setAutoCommit(true);
//            }
//        } catch (SQLException e) {
//            showError("Update Patient", e);
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
//        // حذف المستخدم يكفي لأن patients مربوط بـ users
//        String del = "DELETE FROM users WHERE id = ?";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(del)) {
//            ps.setLong(1, row.getUserId());
//            int n = ps.executeUpdate();
//            if (n == 1) {
//                patientData.remove(row);
//                clearForm();
//                showInfo("Delete", "Patient deleted.");
//            } else {
//                showWarn("Delete", "Nothing deleted.");
//            }
//        } catch (SQLException e) {
//            showError("Delete Patient", e);
//        }
//    }
//
//    private void clearForm() {
//        FullNameTextField.clear();
//        PatientIdTextField.clear();   // لو لغيت الـNID من النموذج احذف هذا السطر
//        PhoneTextField.clear();
//        medicalHistory.clear();
//        GenderComboBox.setValue(Gender.MALE);
//        DateOfBirthPicker.setValue(LocalDate.of(1990, 1, 1));
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
//    /* ============ Row model (بدون Address) ============ */
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
//
//
////package com.example.healthflow.controllers;
////
////import com.example.healthflow.db.Database;
////import com.example.healthflow.net.ConnectivityMonitor;
////import javafx.animation.KeyFrame;
////import javafx.animation.Timeline;
////import javafx.beans.property.*;
////import javafx.collections.FXCollections;
////import javafx.collections.ObservableList;
////import javafx.collections.transformation.FilteredList;
////import javafx.collections.transformation.SortedList;
////import javafx.fxml.FXML;
////import javafx.fxml.FXMLLoader;
////import javafx.scene.Parent;
////import javafx.scene.Scene;
////import javafx.scene.control.*;
////import javafx.scene.control.cell.PropertyValueFactory;
////import javafx.scene.layout.AnchorPane;
////import javafx.stage.Stage;
////import javafx.util.Duration;
////
////import java.io.IOException;
////import java.sql.*;
////import java.time.LocalDate;
////import java.time.format.DateTimeFormatter;
////
////public class ReceptionController {
////
////    /* ============ UI ============ */
////    @FXML private AnchorPane DashboardAnchorPane;
////    @FXML private AnchorPane PatientAnchorPane;
////    @FXML private AnchorPane AppointmentsAnchorPane;
////
////    @FXML private Button DachboardButton;
////    @FXML private Button PatientsButton;
////    @FXML private Button AppointmentsButton;
////    @FXML private Button BackButton;
////
////    // أعلى الشاشة
////    @FXML private Label DateOfDay;
////    @FXML private Label time;
////    @FXML private Label UsernameLabel;
////    @FXML private Label UserIdLabel;
////
////    // نموذج المريض
////    @FXML private TextField FullNameTextField;
////    @FXML private TextField PatientIdTextField;   // national id (اختياري)
////    @FXML private ComboBox<Gender> GenderComboBox;
////    @FXML private DatePicker DateOfBirthPicker;
////    @FXML private TextField PhoneTextField;
////    @FXML private TextArea  medicalHistory;
////
////    @FXML private Button InsertButton;
////    @FXML private Button UpdateButton;
////    @FXML private Button deleteButton;
////    @FXML private Button clearBtn;
////
////    // البحث
////    @FXML private TextField search;
////
////    // جدول المرضى (بدون Address)
////    @FXML private TableView<PatientRow> patientTable;
////    @FXML private TableColumn<PatientRow, Long>      colPatientId;
////    @FXML private TableColumn<PatientRow, String>    colName;
////    @FXML private TableColumn<PatientRow, String>    colGender;
////    @FXML private TableColumn<PatientRow, LocalDate> colDob;
////    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
////    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;
////
////
////    @FXML private Label NumberOfTotalAppointments;
////    @FXML private Label NumberOfTotalDoctors;
////    @FXML private Label NumberOfTotalPatients;
////
////    /* ============ Types ============ */
////    public enum Gender { MALE, FEMALE }
////
////    /* ============ State ============ */
////    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
////    private FilteredList<PatientRow> filtered;
////    private final Navigation navigation = new Navigation();
////
////    /* ============ Init ============ */
////    @FXML
////    private void initialize() {
////        // التنقل بين البانلز
////        DachboardButton.setOnAction(e -> showDashboardPane());
////        PatientsButton.setOnAction(e -> showPatientsPane());
////        AppointmentsButton.setOnAction(e -> showAppointmentPane());
////        BackButton.setOnAction(e -> BackAction());
////
////        // ساعة وتاريخ
////        startClock();
////
////        // كومبوبوكس الجنس
////        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
////        GenderComboBox.setValue(Gender.MALE);
////
////        // تاريخ افتراضي
////        DateOfBirthPicker.setValue(LocalDate.of(1990, 1, 1));
////
////        // جدول المرضى + تحميل البيانات + البحث
////        wirePatientTable();
////        loadPatients();
////        wireSearch();
////
////        // أزرار CRUD
////        InsertButton.setOnAction(e -> doInsertPatient());
////        UpdateButton.setOnAction(e -> doUpdatePatient());
////        deleteButton.setOnAction(e -> doDeletePatient());
////        clearBtn.setOnAction(e -> clearForm());
////
////        // ابدأ بالداشبورد
////        showDashboardPane();
////    }
////
////    /* ============ Clock ============ */
////    private void startClock() {
////        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a"); // 12 ساعة + AM/PM
////        Timeline tl = new Timeline(
////                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
////                new KeyFrame(Duration.seconds(1))
////        );
////        tl.setCycleCount(Timeline.INDEFINITE);
////        tl.play();
////
////        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
////        DateOfDay.setText(LocalDate.now().format(df));
////    }
////
////    /* ============ Navigation ============ */
////    @FXML
////    private void BackAction() {
////        Stage stage = (Stage) BackButton.getScene().getWindow();
////        try {
////            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
////            loader.setControllerFactory(type ->
////                    type == LoginController.class ? new LoginController(new ConnectivityMonitor()) : null
////            );
////            Parent root = loader.load();
////            stage.setScene(new Scene(root));
////            stage.setResizable(false);
////            stage.show();
////        } catch (IOException e) {
////            showError("Navigation", e);
////        }
////    }
////
////    /* ============ Panes ============ */
////    private void showDashboardPane() {
////        DashboardAnchorPane.setVisible(true);
////        PatientAnchorPane.setVisible(false);
////        AppointmentsAnchorPane.setVisible(false);
////    }
////    private void showPatientsPane() {
////        DashboardAnchorPane.setVisible(false);
////        PatientAnchorPane.setVisible(true);
////        AppointmentsAnchorPane.setVisible(false);
////    }
////    private void showAppointmentPane(){
////        DashboardAnchorPane.setVisible(false);
////        PatientAnchorPane.setVisible(false);
////        AppointmentsAnchorPane.setVisible(true);
////    }
////
////    /* ============ Table & Search ============ */
////    private void wirePatientTable() {
////        colPatientId.setCellValueFactory(new PropertyValueFactory<>("patientId"));
////        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
////        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
////        colDob.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
////        colPhoneNumber.setCellValueFactory(new PropertyValueFactory<>("phone"));
////        colMedicalHistory.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
////
////        patientTable.setItems(patientData);
////
////        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
////            if (row == null) return;
////            FullNameTextField.setText(row.getFullName());
//////            PatientIdTextField.setText(row.getNationalId());
////            PhoneTextField.setText(row.getPhone());
////            medicalHistory.setText(row.getMedicalHistory());
////            DateOfBirthPicker.setValue(row.getDateOfBirth());
////            GenderComboBox.setValue("MALE".equals(row.getGender()) ? Gender.MALE : Gender.FEMALE);
////        });
////    }
////
////    private void wireSearch() {
////        filtered = new FilteredList<>(patientData, p -> true);
////        search.textProperty().addListener((obs, old, q) -> {
////            String s = (q == null) ? "" : q.trim().toLowerCase();
////            if (s.isEmpty()) {
////                filtered.setPredicate(p -> true);
////            } else {
////                filtered.setPredicate(p ->
////                        contains(p.getFullName(), s) ||
//////                                contains(p.getNationalId(), s) ||
////                                contains(p.getGender(), s) ||
////                                contains(p.getPhone(), s) ||
////                                contains(p.getMedicalHistory(), s) ||
////                                (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s)) ||
////                                Long.toString(p.getPatientId()).contains(s)
////                );
////            }
////        });
////        SortedList<PatientRow> sorted = new SortedList<>(filtered);
////        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
////        patientTable.setItems(sorted);
////    }
////
////    private boolean contains(String v, String q) {
////        return v != null && v.toLowerCase().contains(q);
////    }
////
////    /* ============ Load ============ */
////    private void loadPatients() {
////        patientData.clear();
////        final String sql = """
////            SELECT p.id             AS patient_id,
////                   u.id             AS user_id,
////                   u.full_name,
////                   u.national_id,
////                   u.phone,
////                   p.date_of_birth,
////                   p.gender,
////                   p.medical_history
////              FROM patients p
////              JOIN users u ON u.id = p.user_id
////             ORDER BY p.id
////        """;
////        try (Connection c = Database.get();
////             PreparedStatement ps = c.prepareStatement(sql);
////             ResultSet rs = ps.executeQuery()) {
////            while (rs.next()) {
////                patientData.add(new PatientRow(
////                        rs.getLong("patient_id"),
////                        rs.getLong("user_id"),
////                        rs.getString("full_name"),
////                        rs.getString("national_id"),
////                        rs.getString("phone"),
////                        rs.getObject("date_of_birth", LocalDate.class),
////                        rs.getString("gender"),
////                        rs.getString("medical_history")
////                ));
////            }
////        } catch (SQLException ex) {
////            showError("Load Patients", ex);
////        }
////    }
////
////    /* ============ CRUD ============ */
////
////    private static final String DEFAULT_PATIENT_PASSWORD = "patient@123";
////
////    private void doInsertPatient() {
////        String fullName = trimOrNull(FullNameTextField.getText());
////        String nid      = trimOrNull(PatientIdTextField.getText());
////        Gender gender   = GenderComboBox.getValue();
////        LocalDate dob   = DateOfBirthPicker.getValue();
////        String phone    = trimOrNull(PhoneTextField.getText());
////        String history  = trimOrNull(medicalHistory.getText());
////
////        if (fullName == null || dob == null || gender == null) {
////            showWarn("Validation", "Full name, gender and date of birth are required.");
////            return;
////        }
////        // الـ DB يفرض: المريض لازم يكون عنده Email أو Phone. اخترنا Phone هنا.
////        if (phone == null) {
////            showWarn("Validation", "Patient must have a phone number.");
////            return;
////        }
////
////        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
////                DEFAULT_PATIENT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt()
////        );
////
////        String insUser = """
////            INSERT INTO users (national_id, full_name, email, password_hash, role, phone)
////            VALUES (?,?,?,?, 'PATIENT', ?)
////            RETURNING id
////        """;
////        String insPatient = """
////            INSERT INTO patients (user_id, date_of_birth, gender, medical_history)
////            VALUES (?, ?, ?::gender_type, ?)
////            RETURNING id
////        """;
////
////        try (Connection c = Database.get()) {
////            c.setAutoCommit(false);
////            try (PreparedStatement psU = c.prepareStatement(insUser);
////                 PreparedStatement psP = c.prepareStatement(insPatient)) {
////
////                psU.setString(1, nid);
////                psU.setString(2, fullName);
////                psU.setString(3, null);  // email اختياري
////                psU.setString(4, hash);
////                psU.setString(5, phone);
////                Long userId;
////                try (ResultSet rs = psU.executeQuery()) {
////                    rs.next();
////                    userId = rs.getLong("id");
////                }
////
////                psP.setLong(1, userId);
////                psP.setObject(2, dob);
////                psP.setString(3, gender.name());
////                psP.setString(4, history);
////                Long patientId;
////                try (ResultSet rs2 = psP.executeQuery()) {
////                    rs2.next();
////                    patientId = rs2.getLong("id");
////                }
////
////                c.commit();
////                patientData.add(new PatientRow(
////                        patientId, userId, fullName, nid, phone, dob, gender.name(), history
////                ));
////                clearForm();
////                showInfo("Insert", "Patient inserted successfully.");
////            } catch (Exception ex) {
////                c.rollback();
////                showError("Insert Patient", ex);
////            } finally {
////                c.setAutoCommit(true);
////            }
////        } catch (SQLException e) {
////            showError("Insert Patient", e);
////        }
////    }
////
////    private void doUpdatePatient() {
////        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
////        if (row == null) {
////            showWarn("Update", "Select a patient row first.");
////            return;
////        }
////
////        String fullName = trimOrNull(FullNameTextField.getText());
////        String nid      = trimOrNull(PatientIdTextField.getText());
////        String phone    = trimOrNull(PhoneTextField.getText());
////        String history  = trimOrNull(medicalHistory.getText());
////        Gender gender   = GenderComboBox.getValue();
////        LocalDate dob   = DateOfBirthPicker.getValue();
////
////        if (fullName == null || dob == null || gender == null) {
////            showWarn("Validation", "Full name, gender and date of birth are required.");
////            return;
////        }
////
////        String updUser = """
////            UPDATE users
////               SET full_name = ?, phone = ?, national_id = ?, updated_at = NOW()
////             WHERE id = ?
////        """;
////
////        String updPatient = """
////            UPDATE patients
////               SET date_of_birth = ?, gender = ?::gender_type, medical_history = ?, updated_at = NOW()
////             WHERE id = ?
////        """;
////
////        try (Connection c = Database.get()) {
////            c.setAutoCommit(false);
////            try (PreparedStatement psU = c.prepareStatement(updUser);
////                 PreparedStatement psP = c.prepareStatement(updPatient)) {
////
////                psU.setString(1, fullName);
////                psU.setString(2, phone);
////                psU.setString(3, nid);
////                psU.setLong(4, row.getUserId());
////                psU.executeUpdate();
////
////                psP.setObject(1, dob);
////                psP.setString(2, gender.name());
////                psP.setString(3, history);
////                psP.setLong(4, row.getPatientId());
////                psP.executeUpdate();
////
////                c.commit();
////
////                // حدّث الصف المعروض
////                row.setFullName(fullName);
//////                row.setNationalId(nid);
////                row.setPhone(phone);
////                row.setDateOfBirth(dob);
////                row.setGender(gender.name());
////                row.setMedicalHistory(history);
////                patientTable.refresh();
////
////                showInfo("Update", "Patient updated successfully.");
////            } catch (Exception ex) {
////                c.rollback();
////                showError("Update Patient", ex);
////            } finally {
////                c.setAutoCommit(true);
////            }
////        } catch (SQLException e) {
////            showError("Update Patient", e);
////        }
////    }
////
////    private void doDeletePatient() {
////        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
////        if (row == null) {
////            showWarn("Delete", "Select a patient row first.");
////            return;
////        }
////        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;
////
////        // حذف المستخدم يكفي بسبب ON DELETE CASCADE/RESTRICT حسب سكيمتك
////        String del = "DELETE FROM users WHERE id = ?";
////        try (Connection c = Database.get();
////             PreparedStatement ps = c.prepareStatement(del)) {
////            ps.setLong(1, row.getUserId());
////            int n = ps.executeUpdate();
////            if (n == 1) {
////                patientData.remove(row);
////                clearForm();
////                showInfo("Delete", "Patient deleted.");
////            } else {
////                showWarn("Delete", "Nothing deleted.");
////            }
////        } catch (SQLException e) {
////            showError("Delete Patient", e);
////        }
////    }
////
////    private void clearForm() {
////        FullNameTextField.clear();
////        PatientIdTextField.clear();
////        PhoneTextField.clear();
////        medicalHistory.clear();
////        GenderComboBox.setValue(Gender.MALE);
////        DateOfBirthPicker.setValue(LocalDate.of(1990, 1, 1));
////        patientTable.getSelectionModel().clearSelection();
////    }
////
////    /* ============ Helpers ============ */
////    private String trimOrNull(String s) {
////        if (s == null) return null;
////        String t = s.trim();
////        return t.isEmpty() ? null : t;
////    }
////
////    private void showError(String title, Exception ex) {
////        ex.printStackTrace();
////        Alert a = new Alert(Alert.AlertType.ERROR);
////        a.setTitle(title);
////        a.setHeaderText(null);
////        a.setContentText(ex.getMessage());
////        a.showAndWait();
////    }
////
////    private void showWarn(String title, String msg) {
////        Alert a = new Alert(Alert.AlertType.WARNING);
////        a.setTitle(title);
////        a.setHeaderText(null);
////        a.setContentText(msg);
////        a.showAndWait();
////    }
////
////    private void showInfo(String title, String msg) {
////        Alert a = new Alert(Alert.AlertType.INFORMATION);
////        a.setTitle(title);
////        a.setHeaderText(null);
////        a.setContentText(msg);
////        a.showAndWait();
////    }
////
////    private boolean confirm(String title, String msg) {
////        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
////        a.setTitle(title);
////        a.setHeaderText(null);
////        a.setContentText(msg);
////        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
////    }
////
////    /* ============ Row model (بدون Address) ============ */
////    public static class PatientRow {
////        private final LongProperty patientId = new SimpleLongProperty();
////        private final LongProperty userId    = new SimpleLongProperty();
////        private final StringProperty fullName = new SimpleStringProperty();
////        private final StringProperty nationalId = new SimpleStringProperty();
////        private final StringProperty phone = new SimpleStringProperty();
////        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
////        private final StringProperty gender = new SimpleStringProperty();
////        private final StringProperty medicalHistory = new SimpleStringProperty();
////
////        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
////                          String phone, LocalDate dob, String gender, String medicalHistory) {
////            setPatientId(patientId);
////            setUserId(userId);
////            setFullName(fullName);
//////            setNationalId(nationalId);
////            setPhone(phone);
////            setDateOfBirth(dob);
////            setGender(gender);
////            setMedicalHistory(medicalHistory);
////        }
////
////        public long getPatientId() { return patientId.get(); }
////        public void setPatientId(long v) { patientId.set(v); }
////        public LongProperty patientIdProperty() { return patientId; }
////
////        public long getUserId() { return userId.get(); }
////        public void setUserId(long v) { userId.set(v); }
////        public LongProperty userIdProperty() { return userId; }
////
////        public String getFullName() { return fullName.get(); }
////        public void setFullName(String v) { fullName.set(v); }
////        public StringProperty fullNameProperty() { return fullName; }
////
//////        public String getNationalId() { return nationalId.get(); }
//////        public void setNationalId(String v) { nationalId.set(v); }
//////        public StringProperty nationalIdProperty() { return nationalId; }
////
////        public String getPhone() { return phone.get(); }
////        public void setPhone(String v) { phone.set(v); }
////        public StringProperty phoneProperty() { return phone; }
////
////        public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
////        public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
////        public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }
////
////        public String getGender() { return gender.get(); }
////        public void setGender(String v) { gender.set(v); }
////        public StringProperty genderProperty() { return gender; }
////
////        public String getMedicalHistory() { return medicalHistory.get(); }
////        public void setMedicalHistory(String v) { medicalHistory.set(v); }
////        public StringProperty medicalHistoryProperty() { return medicalHistory; }
////    }
////}
////
////
////
////
//////package com.example.healthflow.controllers;
//////
//////import com.example.healthflow.net.ConnectivityMonitor;
//////import javafx.collections.FXCollections;
//////
//////import javafx.fxml.FXML;
//////import javafx.fxml.FXMLLoader;
//////import javafx.scene.Parent;
//////import javafx.scene.Scene;
//////import javafx.scene.control.*;
//////import javafx.scene.layout.AnchorPane;
//////import javafx.stage.Stage;
//////import javafx.scene.control.DatePicker;
//////
//////import javafx.scene.control.Button;
//////import javafx.scene.control.ComboBox;
//////
//////import javafx.scene.control.Label;
//////import javafx.scene.control.TableColumn;
//////import javafx.scene.control.TextField;
//////import javafx.collections.ObservableList;
//////
//////
//////import java.io.IOException;
//////
//////public class ReceptionController {
//////
//////
//////
//////    @FXML
//////    private TextField AddressTextField; // for patient
//////
//////    @FXML
//////    private AnchorPane AppointmentAnchorPane;
//////
//////    @FXML
//////    private TableColumn<?, ?> AppointmentIdColumn;  // that to show the information of the patients
//////
//////    @FXML
//////    private AnchorPane Appointments;
//////
//////    @FXML
//////    private Button AppointmentsButton;
//////
//////    @FXML
//////    private Button BackButton;
//////
//////    @FXML
//////    private AnchorPane CenterAnchorPane;
//////
//////    @FXML
//////    private Button DachboardButton;
//////
//////    @FXML
//////    private AnchorPane DashboardAnchorPane;
//////
//////    @FXML
//////    private DatePicker DateOfBirthPicker;   // date of appoinment to the patient
//////
//////    @FXML
//////    private AnchorPane DoctorAnchorPane;
//////
//////    @FXML
//////    private AnchorPane Doctors;
//////
//////    @FXML
//////    private Button DoctorsButton;
//////
//////    @FXML
//////    private TextField FullNameTextField;  // for the patient
//////
//////    @FXML
//////    private ComboBox<Gender> GenderComboBox;
//////
//////
//////    @FXML
//////    private Label NumberOfTotalAppointments;
//////
//////    @FXML
//////    private Label NumberOfTotalDoctors;
//////
//////    @FXML
//////    private Label NumberOfTotalPatients;
//////
//////    @FXML
//////    private AnchorPane PatientAnchorPane;
//////
//////    @FXML
//////    private TextField PatientIdTextField;
//////
//////    @FXML
//////    private AnchorPane Patients;
//////
//////    @FXML
//////    private Button PatientsButton;
//////
//////    @FXML
//////    private TextField PhoneTextField;
//////
//////    @FXML
//////    private Label TotalAppointments;
//////
//////    @FXML
//////    private Label TotalDoctors;
//////
//////    @FXML
//////    private Label TotalPatients;
//////
//////    @FXML
//////    private Label UserIdLabel;    // user id
//////
//////    @FXML
//////    private Label UsernameLabel;  //get the name of the user (full name)
//////
//////    @FXML
//////    private Label welcomeUser;  // welcome to the user just the first name
//////
//////    @FXML
//////    private Label DateOfDay;   // date of this day
//////
//////    @FXML
//////    private Label time;      // changing on time (current time)
//////
//////    @FXML
//////    private Button UpdateButton;
//////
//////    @FXML
//////    private Button clearBtn;
//////
//////    @FXML
//////    private Button deleteButton;
//////
//////    @FXML
//////    private Button InsertButton;
//////
//////    @FXML
//////    private TextArea medicalHistory;
//////
//////    ObservableList<Gender> genders = FXCollections.observableArrayList(Gender.values());
//////
//////
//////    @FXML private AnchorPane AppointmentsAnchorPane;
//////
//////    public enum Gender {
//////        MALE,
//////        FEMALE
//////    }
//////
//////    @FXML
//////    private TextField search;
//////
//////
//////
//////    private final Navigation navigation = new Navigation();
//////
//////    @FXML
//////    private void initialize() {
//////
//////
//////        // ربط الزر بفتح الواجهة الرئيسية
//////        DachboardButton.setOnAction(e -> showDashboardPane());
//////
//////        // ربط الزر بفتح واجهة المرضى
//////        PatientsButton.setOnAction(e -> showPatientsPane());
///////*
//////        // كل ما يخص واجهة المرضى:
//////        // إضافة مستمع لحقل رقم الهوية
//////        PatientIdTextField.textProperty().addListener((observable, oldValue, newValue) -> {
//////            if (!isValidId(newValue)) {
//////                // إذا كان الرقم غير صحيح، عرض تنبيه للمستخدم
//////                showAlert("خطأ", "رقم الهوية الوطنية يجب أن يحتوي على 9 أرقام فقط.");
//////            }
//////        });
//////        // إنشاء قائمة من الخيارات باستخدام الـ enum
//////        GenderComboBox.setValue(Gender.MALE);  // تعيين قيمة افتراضية (ذكر)
//////        ObservableList<Gender> genderOptions = FXCollections.observableArrayList(Gender.values());
//////        // تعيين هذه القائمة للكومبوبوكس
//////        GenderComboBox.setItems(genderOptions);
//////
//////        // تخصيص طريقة عرض القيم في ComboBox باستخدام StringConverter
//////        GenderComboBox.setConverter(new StringConverter<Gender>() {
//////            @Override
//////            public String toString(Gender gender) {
//////                return gender.name(); // إرجاع اسم الـ enum (مثل "ذكر" أو "أنثى")
//////            }
//////
//////            @Override
//////            public Gender fromString(String string) {
//////                return Gender.valueOf(string); // تحويل النص إلى قيمة من الـ enum
//////            }
//////        });
//////        // تعيين قيمة افتراضية إذا رغبت
//////        DateOfBirthPicker.setValue(LocalDate.of(1990, 1, 1));  // مثلاً تعيين 1 يناير 1990
//////
//////*/
//////        // ربط الزر بفتح واجهة المواعيد
//////        AppointmentsButton.setOnAction(e -> showAppointmentPane());
//////
//////        BackButton.setOnAction(e -> BackAction());
//////
//////        GenderComboBox.setItems(genders);
//////    }
//////
//////
//////    private void showDashboardPane() {
//////        DashboardAnchorPane.setVisible(true);
//////        PatientAnchorPane.setVisible(false);
//////        AppointmentsAnchorPane.setVisible(false);
//////    }
//////
//////    private void showPatientsPane() {
//////        DashboardAnchorPane.setVisible(false);
//////        PatientAnchorPane.setVisible(true);
//////        AppointmentsAnchorPane.setVisible(false);
//////    }
//////
//////    private void showAppointmentPane(){
//////        DashboardAnchorPane.setVisible(false);
//////        PatientAnchorPane.setVisible(false);
//////        AppointmentsAnchorPane.setVisible(true);
//////    }
//////    /*
//////    // التحقق من أن رقم الهوية يحتوي على 9 أرقام فقط
//////    private boolean isValidId(String id) {
//////        if (id == null) {
//////            return false;
//////        }
//////
//////        // تحقق إذا كان يحتوي على 9 أرقام فقط
//////        return id.matches("\\d{9}");
//////    }
//////*/
////////    @FXML
////////    private void BackAction(){
//////////         الحصول على الـ Stage الحالي من الـ Scene
//////////        Stage currentStage = (Stage) DashboardAnchorPane.getScene().getWindow();  // الحصول على الـ Stage الحالي
//////////
//////////        // إغلاق نافذة الاستقبال الحالية
//////////        currentStage.close();
//////////        // فتح نافذة تسجيل الدخول باستخدام نفس الـ Stage
//////////        // منع تغيير الحجم في Stage تسجيل الدخول
//////////        Stage loginStage = new Stage(); // Stage جديد لواجهة تسجيل الدخول
//////////        navigation.navigateTo(loginStage, navigation.Login_Fxml);  // استخدام نفس الـ Stage ولكن فتح نافذة تسجيل الدخول
//////////        // منع تغيير حجم نافذة تسجيل الدخول
//////////        loginStage.setResizable(false);
//////////        loginStage.show();  // عرض نافذة تسجيل الدخول
////////        Stage stage = (Stage) BackButton.getScene().getWindow(); // لو عندك المتغير BackButton
////////        Navigation navigation = new Navigation();
////////        navigation.navigateTo(stage, navigation.Login_Fxml);
////////        stage.setResizable(false);
////////        // لا داعي لـ stage.show() إذا كان ظاهر أصلاً، لكن لا تضر
////////        stage.show();
////////
////////
////////    }
//////
//////    @FXML
//////    private void BackAction() {
//////        Stage stage = (Stage) BackButton.getScene().getWindow();
//////
//////        try {
//////            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
//////            loader.setControllerFactory(type ->
//////                    type == LoginController.class ? new LoginController(new ConnectivityMonitor()) : null
//////            );
//////            Parent root = loader.load();
//////
//////            stage.setScene(new Scene(root));
//////            stage.setResizable(false);
//////            stage.show();
//////        } catch (IOException e) {
//////            e.printStackTrace();
//////        }
//////    }
//////
//////    private void showAlert(String title, String message) {
//////        Alert alert = new Alert(Alert.AlertType.ERROR);
//////        alert.setTitle(title);
//////        alert.setHeaderText(null);
//////        alert.setContentText(message);
//////        alert.showAndWait();
//////
//////    }
//////
//////}
//////
