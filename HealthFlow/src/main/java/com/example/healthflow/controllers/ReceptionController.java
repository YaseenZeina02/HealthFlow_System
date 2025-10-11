package com.example.healthflow.controllers;

import com.example.healthflow.dao.AppointmentJdbcDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.dao.DoctorDAO;
import com.example.healthflow.model.Appointment;
import com.example.healthflow.model.DoctorRow;
import com.example.healthflow.model.PatientRow;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.model.Appointment.ApptRow;
import com.example.healthflow.service.PatientService;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;

import com.example.healthflow.db.notify.DbNotifications;
import com.example.healthflow.ui.fx.RefreshScheduler;
import com.example.healthflow.ui.fx.TableUtils;
import static com.example.healthflow.ui.base.Dialogs.error;
//import static jdk.internal.org.commonmark.text.Characters.isBlank;

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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;


public class ReceptionController {

    /* ============ UI ============ */
    @FXML
    private AnchorPane DashboardAnchorPane;
    @FXML
    private AnchorPane PatientAnchorPane;
    @FXML
    private AnchorPane AppointmentsAnchorPane;
    @FXML
    private AnchorPane DoctorAnchorPane;
    @FXML
    private StackPane rootPane;

    @FXML
    private Button DachboardButton;
    @FXML
    private Button PatientsButton;
    @FXML
    private Button AppointmentsButton;
    @FXML
    private Button BackButton;
    @FXML
    private Button DoctorsButton;

    @FXML
    private Label DateOfDay;
    @FXML
    private Label time;
    @FXML
    private Label welcomeUser;

    @FXML
    private Label UsernameLabel;
    @FXML
    private Label UserIdLabel;

    // ===== Patients form =====
    @FXML
    private TextField FullNameTextField;
    @FXML
    private TextField PatientIdTextField;   // National Id
    @FXML
    private ComboBox<Gender> GenderComboBox;
    @FXML
    private DatePicker DateOfBirthPicker;
    @FXML
    private TextField PhoneTextField;
    @FXML
    private TextArea medicalHistory;

    @FXML
    private Button deleteRowApptTable;

    @FXML
    private Button addNewRow;

    @FXML
    private Button InsertButton;
    @FXML
    private Button UpdateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearBtn;

    @FXML
    private TextField search;

    @FXML
    private TableView<PatientRow> patientTable;
    @FXML
    private TableColumn<PatientRow, String> colNationalId;
    @FXML
    private TableColumn<PatientRow, String> colName;
    @FXML
    private TableColumn<PatientRow, String> colGender;
    @FXML
    private TableColumn<PatientRow, LocalDate> colDob;
    @FXML
    private TableColumn<PatientRow, String> colPhoneNumber;
    @FXML
    private TableColumn<PatientRow, String> colMedicalHistory;

    @FXML
    private Label NumberOfTotalAppointments;
    @FXML
    private Label NumberOfTotalDoctors;
    @FXML
    private Label NumberOfTotalPatients;
    @FXML
    private Label patientCompleteNum;
    @FXML
    private Label RemainingNum;

    private final ObservableList<DoctorDAO.AppointmentRow> apptData = FXCollections.observableArrayList();

    // Editable list for the appointments table in the Appointment pane
    private final ObservableList<ApptRow> apptEditable = FXCollections.observableArrayList();

    // Caches/edit choices for in-row editors
    private final ObservableList<String> specialtyChoices = FXCollections.observableArrayList();
    private final Map<String, ObservableList<DoctorDAO.DoctorOption>> doctorsBySpec = new ConcurrentHashMap<>();

    @FXML
    private Circle ActiveStatus;

    @FXML
    private TableColumn<?, ?> AppointmentIdColumn;
    @FXML
    private AnchorPane Appointments;
    @FXML
    private AnchorPane CenterAnchorPane;
    @FXML
    private AnchorPane Doctors;
    @FXML
    private AnchorPane Patients;

    @FXML
    private Label TotalAppointments;
    @FXML
    private Label TotalDoctors;

    @FXML
    private TableView<DoctorDAO.AppointmentRow> TableAppInDashboard;

    @FXML
    private TableColumn<DoctorDAO.AppointmentRow, Number> colAppointmentID;
    @FXML
    private TableColumn<DoctorDAO.AppointmentRow, Void> colActionDash;
    @FXML
    private TableColumn<DoctorDAO.AppointmentRow, LocalDate> colAppintementDateDash;
    @FXML
    private TableColumn<DoctorDAO.AppointmentRow, String> colAppintementTimeDash;
    @FXML
    private TableColumn<DoctorDAO.AppointmentRow, String> colDoctorNameDash;
    @FXML
    private TableColumn<DoctorDAO.AppointmentRow, String> colPatientNameDash;

    @FXML
    private BarChart<String, Number> appointmentStatusChart;
    @FXML
    private Button clearSelectionDach;

    @FXML
    private TextField searchAppointmentDach;
    @FXML
    private TextField searchDoctor;

    @FXML
    private Button insertAppointments;

    @FXML
    private Label TotalPatients;
    @FXML
    private Button BookAppointmentFromPateint;
    @FXML
    private Button updateAppointments;

    @FXML
    private ComboBox<String> DoctorspecialtyApp;             // list of specialties
    @FXML
    private ComboBox<DoctorDAO.DoctorOption> avilabelDoctorApp; // available doctors for selected specialty
    @FXML
    private Button clear_Appointments;
    @FXML
    private Button deleteAppointments;

    @FXML
    private TableView<ApptRow> TableINAppointment;
    @FXML
    private TableColumn<ApptRow, Number> colAppointmentIDAppointment;
    @FXML
    private TableColumn<ApptRow, LocalDate> colDateAppointment;
    @FXML
    private TableColumn<ApptRow, String> colDoctorNameAppointment;
    @FXML
    private TableColumn<ApptRow, String> colPatientNameAppointment;
    @FXML
    private TableColumn<ApptRow, String> colSpecialty;
    @FXML
    private TableColumn<ApptRow, String> colStatusAppointment;
    @FXML
    private TableColumn<ApptRow, String> colStartTime;
    @FXML
    private TableColumn<ApptRow, Number> colSessionTime;
    @FXML
    private TableColumn<ApptRow, String> colRoomNumber;

    @FXML
    private Label LabelToAlert;


    @FXML
    private Button deleteButtonAppointment;
    @FXML
    private Label getPatientName;
    @FXML
    private Label getPatientID;

    // ===== Doctors table =====
    @FXML
    private TableView<DoctorRow> DocTable_Recption;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_name;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Gender;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Phone;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Specialty;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_bio;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Status;
    @FXML
    private TableColumn<DoctorRow, String> colDocRoomNumber;
    @FXML
    private TableColumn<DoctorRow, Boolean> colDoctor_available;

    @FXML
    private DatePicker AppointmentDate;
    @FXML
    private TextField appointmentSetTime;
    @FXML
    private TextField PatientNameForAppointment;
    @FXML
    private TextField PatientIDForAppointment;
    @FXML
    private ComboBox<DoctorDAO.Slot> cmbSlots;

    @FXML
    private Label DoctorAvilable;


    private static final java.time.ZoneId APP_ZONE = java.time.ZoneId.of("Asia/Gaza");

    // helpers:
    private static java.time.OffsetDateTime toAppOffset(java.time.LocalDate d, java.time.LocalTime t) {
        return java.time.ZonedDateTime.of(d, t, APP_ZONE).toOffsetDateTime();
    }
    private static java.time.LocalDateTime toLocal(java.time.OffsetDateTime odt) {
        return odt == null ? null : odt.atZoneSameInstant(APP_ZONE).toLocalDateTime();
    }

    private void updateDirtyAlert() {
        int n = (int) apptEditable.stream().filter(Appointment.ApptRow::isDirty).count();
        if (LabelToAlert != null) {
            if (n > 0) {
                LabelToAlert.setText(n + (n == 1 ? " unsaved change" : " unsaved changes"));
                LabelToAlert.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                LabelToAlert.setVisible(true);
            } else {
                LabelToAlert.setText("");
                LabelToAlert.setVisible(false);
            }
        }
    }


    // --- Auto refresh infra ---
    private final ScheduledExecutorService autoRefreshExec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ui-auto-refresh");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean refreshBusy = new AtomicBoolean(false);

    public static final int DEFAULT_SESSION_MIN = 20;
    // --- Coalesced UI refresh + DB NOTIFY ---
    private final RefreshScheduler uiRefresh = new RefreshScheduler(600);
    private DbNotifications apptDbListener;

    // To color current nav button
    private static final String ACTIVE_CLASS = "current";
    private static final DateTimeFormatter SLOT_FMT_12H = DateTimeFormatter.ofPattern("hh:mm a");

    private void markNavActive(Button active) {
        Button[] all = {DachboardButton, DoctorsButton, PatientsButton, AppointmentsButton};
        for (Button b : all) {
            b.getStyleClass().remove(ACTIVE_CLASS);
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
            active.getStyleClass().add(ACTIVE_CLASS);
        }
    }





    private static final String[] ROOMS = {"Room 1", "Room 2", "Room 3", "Room 4", "Room 5", "Room 6", "Room 7", "Room 8", "Room 9"};

    // Cache for free slots per doctor/day to make row selection instant
    private final Map<Long, Map<LocalDate, ObservableList<DoctorDAO.Slot>>> slotCache = new ConcurrentHashMap<>();


    // Best-effort: cancel pending debounced refresh if the implementation exposes such method
    private void cancelPendingUiRefresh() {
        if (uiRefresh == null) return;
        try {
            var m = uiRefresh.getClass().getMethod("cancelPending");
            m.invoke(uiRefresh);
        } catch (Exception ignore) {
            // no-op if not supported
        }
    }

    /* ============ slot load ============ */

    // إرجاع الغرف المتاحة (Room 1..Room 9) مع استبعاد المحجوزة
    private List<String> listAvailableRooms(long doctorId, LocalDate date, LocalTime time) throws SQLException {
        final int TOTAL_ROOMS = 9;
        // كل الغرف الافتراضية
        List<String> all = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ROOMS; i++) all.add("Room " + i);

        if (doctorId <= 0 || date == null || time == null) return all;

//        long doctorId = doctorDAO.findIdByName(doctorName);

        // الغرف المحجوزة لهذه اللحظة لهذا الطبيب
        final String sql = """
            SELECT COALESCE(location,'') AS loc
            FROM appointments
            WHERE doctor_id = ?
              AND appointment_date::date = ?
              AND appointment_date::time = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, doctorId);   // لا تستخدم الاسم هنا
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));

            try (ResultSet rs = ps.executeQuery()) {
                Set<String> taken = new HashSet<>();
                while (rs.next()) {
                    String loc = rs.getString("loc");
                    if (loc != null && !loc.isBlank()) taken.add(loc);
                }
                all.removeAll(taken);
                return all;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return all; // fallback لو صار خطأ
        }
    }


    /* ============ Types ============ */
    public enum Gender {MALE, FEMALE}

    /* ============ State ============ */
    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
    private FilteredList<PatientRow> filtered;

    private final ObservableList<DoctorRow> doctorData = FXCollections.observableArrayList();
    private FilteredList<DoctorRow> doctorFiltered;

    private final Navigation navigation = new Navigation();
    private final PatientService patientService = new PatientService();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    /* ============ Connectivity ============ */
    private final ConnectivityMonitor monitor;
    private static volatile boolean listenerRegistered = false;
    private static volatile Boolean lastNotifiedOnline = null;

    public ReceptionController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    public ReceptionController() {
        this(new ConnectivityMonitor());
    }

    /* ============ Init ============ */
    @FXML
    private void initialize() {
        // CSS attach (safe if scene null at init)
        if (rootPane != null) {
            var cssUrl = getClass().getResource("/com/example/healthflow/Design/ReceptionDesign.css");
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                if (rootPane.getScene() != null) {
                    if (!rootPane.getScene().getStylesheets().contains(css))
                        rootPane.getScene().getStylesheets().add(css);
                } else {
                    rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null && !newScene.getStylesheets().contains(css)) {
                            newScene.getStylesheets().add(css);
                        }
                    });
                }
            } else {
                System.err.println("Reception CSS not found at /com/example/healthflow/Design/ReceptionDesign.css");
            }
        }

        monitor.start();
        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
            banner.prefWidthProperty().bind(rootPane.widthProperty());
        }
        OnlineBindings.disableWhenOffline(monitor,
                InsertButton, UpdateButton, deleteButton, clearBtn,
                DachboardButton, PatientsButton, AppointmentsButton, DoctorsButton);

        if (!listenerRegistered) {
            listenerRegistered = true;
            final boolean[] firstEmissionHandled = {false};
            monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
                if (!firstEmissionHandled[0]) {
                    firstEmissionHandled[0] = true;
                    lastNotifiedOnline = isOnline;
                    return;
                }
                if (lastNotifiedOnline != null && lastNotifiedOnline == isOnline) return;
                lastNotifiedOnline = isOnline;
            });
        }

        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPane());
        AppointmentsButton.setOnAction(e -> showAppointmentPane());
        DoctorsButton.setOnAction(e -> showDoctorPane());
        BackButton.setOnAction(e -> BackAction());

        startClock();

        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);

        if (AppointmentDate != null && AppointmentDate.getValue() == null) {
            AppointmentDate.setValue(LocalDate.now());
        }

        wirePatientTable();
        wireDoctorTable();
        wireSearchPatients();
        wireSearchDoctors();
        setupDoctorFilters();

        InsertButton.setOnAction(e -> {
            if (ensureOnlineOrAlert()) doInsertPatient();
        });
        UpdateButton.setOnAction(e -> {
            if (ensureOnlineOrAlert()) doUpdatePatient();
        });
        deleteButton.setOnAction(e -> {
            if (ensureOnlineOrAlert()) doDeletePatient();
        });
        clearBtn.setOnAction(e -> clearForm());
        BookAppointmentFromPateint.setOnAction(e -> {
            PatientRow row = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
            if (row == null) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("Select a patient");
                a.setHeaderText(null);
                a.setContentText("Please select a patient from the table first.");
                a.showAndWait();
                return;
            }
            if (getPatientName != null) getPatientName.setText(row.getFullName());
            if (getPatientID != null) getPatientID.setText(row.getNationalId());
            showAppointmentPane();
            if (DoctorspecialtyApp != null && DoctorspecialtyApp.getItems().isEmpty()) loadSpecialtiesAsync();
            addOrFocusDraftForPatient(row);
        });

        Platform.runLater(() -> {
            new Thread(() -> {
                try {
                    loadHeaderUser();
                } catch (Exception ignored) {
                }
            }, "hdr-user-load").start();
            new Thread(this::loadPatientsBG, "patients-load").start();
            new Thread(() -> {
                var list = DoctorDAO.loadDoctorsBG();
                Platform.runLater(() -> doctorData.setAll(list));
            }, "doctors-load").start();
        });

        // Slots combobox rendering
        // داخل initialize أو أينما تهيّئ cmbSlots
        if (cmbSlots != null) {
            cmbSlots.setCellFactory(cb -> new ListCell<DoctorDAO.Slot>() {
                @Override
                protected void updateItem(DoctorDAO.Slot s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(empty || s == null ? null :
                            s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
                                    s.to().toLocalTime().format(SLOT_FMT_12H));
                }
            });
            cmbSlots.setButtonCell(new ListCell<DoctorDAO.Slot>() {
                @Override
                protected void updateItem(DoctorDAO.Slot s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(empty || s == null ? "Select time"
                            : s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
                            s.to().toLocalTime().format(SLOT_FMT_12H));
                }
            });
            cmbSlots.setOnShown(e -> refreshSlots());
        } else {
            System.out.println("cmbSlots is null: Reception.fxml likely doesn't have fx:id=\"cmbSlots\" on a ComboBox");
        }

        if (AppointmentDate != null) AppointmentDate.valueProperty().addListener((o, a, b) -> refreshSlots());
        if (avilabelDoctorApp != null) avilabelDoctorApp.valueProperty().addListener((o, a, b) -> refreshSlots());

        showDashboardPane();

        // appointments wiring + load
        wireAppointmentsTables();
        setupAppointmentSlotsListener();
        wireDashboardAppointmentsSearch();
        if (clearSelectionDach != null) clearSelectionDach.setOnAction(e -> {
            if (TableAppInDashboard != null) TableAppInDashboard.getSelectionModel().clearSelection();
            if (appointmentStatusChart != null) appointmentStatusChart.getData().clear();
            if (searchAppointmentDach != null) searchAppointmentDach.clear();
        });

        // CRUD buttons
        if (insertAppointments != null) insertAppointments.setOnAction(e -> doInsertAppointment());
        if (updateAppointments != null) updateAppointments.setOnAction(e -> doUpdateAppointment());
        if (deleteAppointments != null) deleteAppointments.setOnAction(e -> doDeleteAppointment());
        if (clear_Appointments != null) clear_Appointments.setOnAction(e -> doClearAppointmentForm());
        if (addNewRow != null) addNewRow.setOnAction(e -> addBlankDraftRow());
        // زر حذف صف من جدول المواعيد (لا يحذف من قاعدة البيانات)
        if (deleteRowApptTable != null) {
            deleteRowApptTable.setOnAction(e -> {
                if (TableINAppointment == null) return;
                ApptRow selected = TableINAppointment.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showWarn("Delete Row", "Please select a row to delete.");
                    return;
                }
                TableINAppointment.getItems().remove(selected);
            });
        }

        // initial data loads
        new Thread(this::loadAppointmentsTable, "appt-load").start();
        new Thread(this::updateAppointmentCounters, "appt-counts").start();

        // === التحديث اللحظي + تهيئة أولية ===
        startDbNotifications();      // يبدأ LISTEN
        scheduleCoalescedRefresh();  // تعبئة أولية

    }
    private void setupAppointmentSlotsListener() {
        // listeners already wired in initialize():
        // AppointmentDate.valueProperty() -> refreshSlots()
        // avilabelDoctorApp.valueProperty() -> refreshSlots()
        // cmbSlots.setOnShown(...) -> refreshSlots()
    }

    /**
     * TableCell تعرض DatePicker لتعديل تاريخ الموعد داخل الجدول
     */

    // داخل ReceptionController (أو المكان اللي مخصص لتهيئة الأعمدة)
    private TableCell<Appointment.ApptRow, LocalDate> datePickerCell() {
        return new TableCell<Appointment.ApptRow, LocalDate>() {
            private final DatePicker picker = new DatePicker();

            {
                // شكليّات + إصلاحات
                picker.setEditable(true);
                picker.setPromptText("yyyy-MM-dd");

                // فور اختيار تاريخ جديد
                picker.setOnAction(e -> {
                    var rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    LocalDate d = picker.getValue();
                    if (rowItem == null || d == null) return;

                    // حدّث الموديل
                    rowItem.setDate(d);

                    // لو فيه وقت محدد، حدّث الـ start في الداتابيز
                    if (rowItem.getTime() != null && rowItem.getId() > 0) {
                        try {
                            updateAppointmentStartAt(rowItem.getId(), d, rowItem.getTime());
                        } catch (Exception ex) {
                            showError("Update date/time", ex);
                        }
                    }

                    // ريفرش جدول/أوقات
                    commitEdit(d);
                    if (TableINAppointment != null) TableINAppointment.refresh();
                    updateDirtyAlert();
                });

                // افتح الـ DatePicker عند بداية التحرير
                this.setOnMouseClicked(me -> {
                    if (!isEmpty() && me.getClickCount() == 1) {
                        startEdit();
                        picker.show();
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                picker.setValue(getItem());
                setGraphic(picker);
                setText(null);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(format(getItem()));
            }

            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else if (isEditing()) {
                    picker.setValue(item);
                    setGraphic(picker);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(format(item));
                }
            }

            private String format(LocalDate d) {
                return (d == null) ? "" : d.toString(); // بدك فورمات معيّن؟ استعمل DateTimeFormatter
            }
        };
    }
//    private TableCell<ApptRow, LocalDate> DatePickerCell() {
//        return new TableCell<ApptRow, LocalDate>() {
//            private final DatePicker picker = new DatePicker();
//
//            {
//                // لمسات شكل اختيارية
//                picker.setEditable(ture);
//                picker.setPromptText("yyyy-MM-dd");
//
//                // لما يختار تاريخ جديد
//                picker.valueProperty().addListener((obs, old, d) -> {
//                    if (!isEditing()) return;
//                    ApptRow row = getTableView().getItems().get(getIndex());
//                    if (row == null) return;
//
//                    row.setDate(d);
//                    // لو عنده وقت مختار، حدّث start_at في الداتابيز
//                    if (row.getTime() != null) {
//                        updateAppointmentStartAt(row.getId(), d, row.getTime());
//                    }
//                    // التاريخ تغيّر → لازم ننعش قائمة الأوقات في عمود الـ Time
//                    if (TableINAppointment != null) TableINAppointment.refresh();
//                    commitEdit(d);
//                });
//
//                // منع فتح الـDatePicker لو ما في صف صالح
//                setOnMouseClicked(e -> {
//                    if (isEmpty()) e.consume();
//                });
//            }
//
//            @Override public void startEdit() {
//                super.startEdit();
//                picker.setValue(getItem());
//                setGraphic(picker);
//                setText(null);
//            }
//
//            @Override public void cancelEdit() {
//                super.cancelEdit();
//                setGraphic(null);
//                setText(getItem() == null ? "" : getItem().toString());
//            }
//
//            @Override protected void updateItem(LocalDate item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty) {
//                    setText(null);
//                    setGraphic(null);
//                } else if (isEditing()) {
//                    picker.setValue(item);
//                    setGraphic(picker);
//                    setText(null);
//                } else {
//                    setText(item == null ? "" : item.toString());
//                    setGraphic(null);
//                }
//            }
//        };
//    }

    /**
     * Doctor column: show ComboBox only when row is selected/editing
     */
    private TableCell<Appointment.ApptRow, String> doctorComboCell() {
        return new TableCell<Appointment.ApptRow, String>() {
            private final ComboBox<DoctorDAO.DoctorOption> combo = new ComboBox<>();

            {
                combo.setVisibleRowCount(8);
                combo.setPromptText("Select doctor");
                combo.setCellFactory(list -> new ListCell<>() {
                    @Override
                    protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.fullName + "  (Room: " + item.roomNumber + ")");
                    }
                });
                combo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.fullName + "  (Room: " + item.roomNumber + ")");
                    }
                });

                // افتح المحرر على نقرة واحدة حين يكون الصف محددًا
                setOnMouseClicked(e -> {
                    if (!isEmpty() && getTableRow() != null && getTableRow().isSelected()) {
                        startEdit();
                        combo.show();
                    }
                });

                // حمّل الأطباء المتاحين للتخصص الحالي عند فتح القائمة
                combo.setOnShown(e -> {
                    var rowItem = (getTableRow() == null) ? null : getTableRow().getItem();
                    if (rowItem == null) return;
                    String spec = rowItem.getSpecialty();
                    java.util.List<DoctorDAO.DoctorOption> opts;
                    try {
                        opts = doctorDAO.listAvailableBySpecialty(spec);
                    } catch (Exception ex) {
                        opts = java.util.Collections.emptyList();
                    }
                    combo.setItems(FXCollections.observableArrayList(opts));
                    // اختَر الحالي إن كان مضبوطًا
                    if (rowItem.getDoctorId() > 0) {
                        for (var o : opts) if (o.doctorId == rowItem.getDoctorId()) { combo.getSelectionModel().select(o); break; }
                    }
                });

                // تحديث الموديل عند الاختيار
                combo.setOnAction(e -> {
                    var rowItem = (getTableRow() == null) ? null : getTableRow().getItem();
                    var opt = combo.getValue();
                    if (rowItem == null || opt == null) return;
                    rowItem.setDoctorId(opt.doctorId);
                    rowItem.setDoctorName(opt.fullName);
                    // ✅ أهم سطر: ثبّت الغرفة في الصف عند اختيار الدكتور
                    rowItem.setRoomNumber(opt.roomNumber);
                    rowItem.setDirty(true);
                    commitEdit(opt.fullName);
                    if (TableINAppointment != null) TableINAppointment.refresh();
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                setGraphic(combo);
                setText(null);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean showEditor = isEditing() && getTableRow() != null && getTableRow().isSelected();
                if (showEditor) {
                    setGraphic(combo);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        };
    }


     private void refreshSlots() {

        if (cmbSlots == null) return;
        var doc = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
        var day = (AppointmentDate == null) ? null : AppointmentDate.getValue();
        if (doc == null || day == null) {
            cmbSlots.setItems(FXCollections.observableArrayList());
            return;
        }

        final LocalTime open = LocalTime.of(9, 0);
        final LocalTime close = LocalTime.of(15, 0);
        final int slotMinutes = DEFAULT_SESSION_MIN; //20 in this time

        new Thread(() -> {
            try {
                var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMinutes);
                if (day.equals(LocalDate.now())) {
                    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                    int mod = now.getMinute() % slotMinutes;
                    LocalDateTime cutoff = (mod == 0) ? now : now.plusMinutes(slotMinutes - mod);
                    slots.removeIf(s -> s.from().isBefore(cutoff));
                    if (now.toLocalTime().isAfter(close)) {
                        Platform.runLater(() -> {
                            cmbSlots.getItems().clear();
                            showInfo("Working Hours", "Clinic working hours are over for today.");
                        });
                        return;
                    }
                }
                var data = FXCollections.observableArrayList(slots);
                Platform.runLater(() -> cmbSlots.setItems(data));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showWarn("Slots", "Failed to load free slots: " + e.getMessage()));
            }
        }, "load-slots").start();
    }

    private static String fmt12(LocalTime t) {
        return t == null ? "" : t.format(SLOT_FMT_12H);
    }

    /* ============ Clock (12h) ============ */
    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline tl = new Timeline(new KeyFrame(Duration.ZERO, e -> time.setText(LocalTime.now().format(tf))),
                new KeyFrame(Duration.seconds(1)));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateOfDay.setText(LocalDate.now().format(df));
    }

    /* ============ Load header user ============ */
    private void loadHeaderUser() {
        var u = Session.get();
        if (u == null) return;
        String sql = "SELECT id, full_name FROM users WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
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
            loader.setControllerFactory(type -> type == LoginController.class ? new LoginController(monitor) : null);
            Parent loginRoot = loader.load();

            var banner = new ConnectivityBanner(monitor);
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
            root.setTop(banner);
            root.setCenter(loginRoot);

            stage.setScene(new Scene(root));
            stage.setTitle("HealthFlow");
            stage.setResizable(false);
            stage.show();

            // أوقف المستمعين والـ executors عند الخروج
            shutdown();
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
        markNavActive(DachboardButton);
    }

    private void showDoctorPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(true);
        markNavActive(DoctorsButton);
    }

    private void showPatientsPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
        markNavActive(PatientsButton);
    }

    private void showAppointmentPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(true);
        DoctorAnchorPane.setVisible(false);
        markNavActive(AppointmentsButton);
    }

    /* ============ Patients: table & search ============ */
    private void wirePatientTable() {
        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());
        patientTable.setItems(patientData);
        patientTable.setEditable(true);
        setupPatientInlineEditing();

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

    private void wireSearchPatients() {
        filtered = new FilteredList<>(patientData, p -> true);
        search.textProperty().addListener((obs, old, q) -> {
            String s = (q == null) ? "" : q.trim().toLowerCase();
            if (s.isEmpty()) filtered.setPredicate(p -> true);
            else filtered.setPredicate(p ->
                    contains(p.getFullName(), s) || contains(p.getGender(), s) ||
                            contains(p.getPhone(), s) || contains(p.getNationalId(), s) ||
                            contains(p.getMedicalHistory(), s) ||
                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s)));
        });
        SortedList<PatientRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        patientTable.setItems(sorted);
    }

    /**
     * تفعيل التحرير داخل patientTable والكتابة مباشرة إلى قاعدة البيانات
     */
    private void setupPatientInlineEditing() {
        // Full Name -> users.full_name
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setFullName(v);
            updateUserText(row.getUserId(), "full_name", v);
            notifyPatientsChanged();
        });

        // Phone -> users.phone
        colPhoneNumber.setCellFactory(TextFieldTableCell.forTableColumn());
        colPhoneNumber.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setPhone(v);
            updateUserText(row.getUserId(), "phone", v);
            notifyPatientsChanged();
        });

        // Gender -> users.gender (ComboBox MALE/FEMALE)
        colGender.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList("MALE", "FEMALE")
        ));
        colGender.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setGender(v);
            updateUserText(row.getUserId(), "gender", v);
            notifyPatientsChanged();
        });

        // National ID -> patients.national_id
        colNationalId.setCellFactory(TextFieldTableCell.forTableColumn());
        colNationalId.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setNationalId(v);
            updatePatientText(row.getPatientId(), "national_id", v);
            notifyPatientsChanged();
        });

        // Date of Birth -> patients.date_of_birth (نستخدم TextFieldTableCell مع محوّل بسيط yyyy-MM-dd)
        colDob.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : d.toString();
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return LocalDate.parse(s.trim()); // صيغة: 2025-10-10
            }
        }));
        colDob.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            LocalDate d = ev.getNewValue();
            if (d == null) return;
            row.setDateOfBirth(d);
            updatePatientDate(row.getPatientId(), "date_of_birth", d);
            notifyPatientsChanged();
        });

        // Medical History -> patients.medical_history
        colMedicalHistory.setCellFactory(TextFieldTableCell.forTableColumn());
        colMedicalHistory.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) v = ""; // نسمح بقيمة فارغة
            row.setMedicalHistory(v);
            updatePatientText(row.getPatientId(), "medical_history", v);
            notifyPatientsChanged();
        });
    }

    private void updateUserText(long userId, String column, String value) {
        final String sql = "UPDATE users SET " + column + " = ? WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("Update user." + column, e);
        }
    }

    private void updatePatientText(long patientId, String column, String value) {
        final String sql = "UPDATE patients SET " + column + " = ? WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setLong(2, patientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("Update patient." + column, e);
        }
    }

    private void updatePatientDate(long patientId, String column, LocalDate d) {
        final String sql = "UPDATE patients SET " + column + " = ? WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(d));
            ps.setLong(2, patientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("Update patient." + column, e);
        }
    }

    private void notifyPatientsChanged() {
        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','update')")) {
            nps.execute();
        } catch (SQLException e) {
            // مش حرجة لو فشلت النوتيفاي، بس نطبع للتشخيص
            e.printStackTrace();
        }
    }

    private String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean contains(String v, String q) {
        return v != null && v.toLowerCase().contains(q);
    }

    //    private void loadPatientsBG() {
    //        try {
    //            var list = patientService.listPatients(); // List<PatientRow>
    //            Platform.runLater(() -> {
    //                patientData.clear();
    //                // بدل الحلقة التي كانت تستخدم pv.patientId()… إلخ
    //                var pv = patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
    //
    //                // استبدل الاستدعاءات style record بالـ getters
    //                patientData.add(new PatientRow(
    //                        pv.getPatientId(), pv.getUserId(), pv.getFullName(), pv.getNationalId(),
    //                        pv.getPhone(), pv.getDateOfBirth(), pv.getGender(), pv.getMedicalHistory()
    //                ));
    //                patientData.addAll(list);
    //            });
    //        } catch (Exception ex) {
    //            Platform.runLater(() -> showError("Load Patients", ex));
    //        }
    //    }

    private void loadPatientsBG() {
        try {
            var list = patientService.listPatients(); // List<PatientRow>
            Platform.runLater(() -> {
                patientData.clear();
                patientData.addAll(list);
            });
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Load Patients", ex));
        }
    }

    /* ============ Doctors: table, search, load ============ */
    private void wireDoctorTable() {
        if (colDoctor_name != null) colDoctor_name.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        if (colDoctor_Gender != null) colDoctor_Gender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        if (colDoctor_Phone != null) colDoctor_Phone.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        if (colDoctor_Specialty != null)
            colDoctor_Specialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
        if (colDoctor_bio != null) colDoctor_bio.setCellValueFactory(cd -> cd.getValue().bioProperty());
        if (colDoctor_Status != null) colDoctor_Status.setCellValueFactory(cd -> cd.getValue().statusTextProperty());
        if (colDoctor_available != null)
            colDoctor_available.setCellValueFactory(cd -> cd.getValue().availableProperty());
        if (colDocRoomNumber != null) colDocRoomNumber.setCellValueFactory(cd -> cd.getValue().roomNumberProperty());
        if (DocTable_Recption != null) DocTable_Recption.setItems(doctorData);
    }

    private void wireSearchDoctors() {
        doctorFiltered = new FilteredList<>(doctorData, d -> true);
        if (searchDoctor != null) {
            searchDoctor.textProperty().addListener((obs, old, q) -> {
                String s = (q == null) ? "" : q.trim().toLowerCase();
                if (s.isEmpty()) doctorFiltered.setPredicate(d -> true);
                else doctorFiltered.setPredicate(d ->
                        contains(d.getFullName(), s) || contains(d.getGender(), s) ||
                                contains(d.getPhone(), s) || contains(d.getSpecialty(), s) ||
                                contains(d.getBio(), s) || contains(d.getStatusText(), s));
            });
        }
        if (DocTable_Recption != null) {
            SortedList<DoctorRow> sorted = new SortedList<>(doctorFiltered);
            sorted.comparatorProperty().bind(DocTable_Recption.comparatorProperty());
            DocTable_Recption.setItems(sorted);
        }
    }

    private void setupDoctorFilters() {
        if (DoctorspecialtyApp != null) {
            DoctorspecialtyApp.setPromptText("Select specialty");
            loadSpecialtiesAsync();
            DoctorspecialtyApp.valueProperty().addListener((obs, old, sp) -> loadAvailableDoctorsForSpecialty(sp));
        }
        if (avilabelDoctorApp != null) {
            avilabelDoctorApp.setPromptText("Available doctor");
            avilabelDoctorApp.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.fullName + "  (id: " + item.doctorId + ")");
                }
            });
            avilabelDoctorApp.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.fullName);
                }
            });
        }
    }

    private void loadSpecialtiesAsync() {
        if (DoctorspecialtyApp == null) return;
        new Thread(() -> {
            try {
                var available = doctorDAO.listAvailableBySpecialty((String) null);
                Set<String> specs = new TreeSet<>();
                for (var opt : available) if (opt != null && opt.specialty != null) specs.add(opt.specialty);
                Platform.runLater(() -> DoctorspecialtyApp.setItems(FXCollections.observableArrayList(specs)));
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Doctors", "Failed to load specialties (available only)."));
            }
        }, "recp-specialties").start();
    }

    private void loadAvailableDoctorsForSpecialty(String specialty) {
        if (avilabelDoctorApp == null) return;
        if (specialty == null || specialty.isBlank()) {
            avilabelDoctorApp.getItems().clear();
            avilabelDoctorApp.setValue(null);
            return;
        }
        new Thread(() -> {
            try {
                var list = doctorDAO.listAvailableBySpecialty(specialty);
                Platform.runLater(() -> {
                    avilabelDoctorApp.setItems(FXCollections.observableArrayList(list));
                    if (!list.isEmpty()) avilabelDoctorApp.getSelectionModel().select(0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Doctors", "Failed to load available doctors."));
            }
        }, "recp-avail-docs").start();
    }

    private void ensureSpecialtiesLoadedAsync() {
        if (!specialtyChoices.isEmpty()) return;
        new Thread(() -> {
            try {
                var all = doctorDAO.listAvailableBySpecialty((String) null);
                Set<String> sp = new TreeSet<>();
                for (var d : all) if (d != null && d.specialty != null) sp.add(d.specialty);
                Platform.runLater(() -> specialtyChoices.setAll(sp));
            } catch (Exception ignored) {
            }
        }, "load-spec-cache").start();
    }

    private void ensureDoctorsForSpecAsync(String spec) {
        if (spec == null || spec.isBlank()) return;
        if (doctorsBySpec.containsKey(spec)) return;
        doctorsBySpec.put(spec, FXCollections.observableArrayList());
        new Thread(() -> {
            try {
                var list = doctorDAO.listAvailableBySpecialty(spec);
                Platform.runLater(() -> doctorsBySpec.get(spec).setAll(list));
            } catch (Exception ignored) {
            }
        }, "load-docs-" + spec).start();
    }

    /**
     * تحديث لحظي مجمّع (coalesced)
     */
    private void scheduleCoalescedRefresh() {
        uiRefresh.request(() -> {
            new Thread(() -> {
                try {
                    var apptRows = AppointmentJdbcDAO.listScheduledAppointments();
                    var mapped = FXCollections.<ApptRow>observableArrayList();
                    for (var r : apptRows) {
                        ApptRow ar = new ApptRow();
                        ar.setId(r.id);
                        ar.setDoctorId(r.doctorId);
                        ar.setDoctorName(r.doctorName);
                        ar.setPatientName(r.patientName);
                        ar.setSpecialty(r.specialty);
                        ar.setStatus(r.status);
                        java.time.LocalDateTime ldt1 = toLocal(r.startAt);
                        if (ldt1 != null) {
                            ar.setDate(ldt1.toLocalDate());
                            ar.setTime(ldt1.toLocalTime());
                        }

                        // ✅ أضف هذا السطر:
                        ar.setRoomNumber(r.location);

                        ar.setNew(false);
                        ar.setDirty(false);
                        mapped.add(ar);
                    }

                    var dashRows = apptRows;
                    if (TableAppInDashboard != null && searchAppointmentDach != null) {
                        String q = searchAppointmentDach.getText();
                        if (q != null && !q.isBlank()) dashRows = AppointmentJdbcDAO.searchScheduledAppointments(q);
                    }
                    final var dashRowsFinal = dashRows;

                    int doctors = AppointmentJdbcDAO.countAvailableDoctors();
                    int appts = AppointmentJdbcDAO.countAppointments();
                    int patients = AppointmentJdbcDAO.countPatients();
                    int completed = AppointmentJdbcDAO.countCompletedAppointments();
                    int scheduled = AppointmentJdbcDAO.countScheduledAppointments();

                    Platform.runLater(() -> {
                        apptEditable.setAll(mapped); // استبدال ذري
                        if (TableAppInDashboard != null) apptData.setAll(dashRowsFinal);
                        if (NumberOfTotalDoctors != null) NumberOfTotalDoctors.setText(String.valueOf(doctors));
                        if (NumberOfTotalAppointments != null) NumberOfTotalAppointments.setText(String.valueOf(appts));
                        if (NumberOfTotalPatients != null) NumberOfTotalPatients.setText(String.valueOf(patients));
                        if (patientCompleteNum != null) patientCompleteNum.setText(String.valueOf(completed));
                        if (RemainingNum != null) RemainingNum.setText(String.valueOf(scheduled));
                        updatePatientDetailsChart();
                    });
                    TableUtils.applyDelta(apptEditable, mapped, ApptRow::getId);

                    if (cmbSlots != null && avilabelDoctorApp != null && AppointmentDate != null
                            && avilabelDoctorApp.getValue() != null && AppointmentDate.getValue() != null) {
                        final LocalTime open = LocalTime.of(9, 0);
                        final LocalTime close = LocalTime.of(15, 0);
                        final int slotMin = 20;
                        var doc = avilabelDoctorApp.getValue();
                        var day = AppointmentDate.getValue();
                        var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMin);
                        if (day.equals(LocalDate.now())) {
                            var now = LocalDateTime.now().withSecond(0).withNano(0);
                            int mod = now.getMinute() % slotMin;
                            var cutoff = (mod == 0) ? now : now.plusMinutes(slotMin - mod);
                            slots.removeIf(s -> s.from().isBefore(cutoff));
                            if (now.toLocalTime().isAfter(close)) slots.clear();
                        }
                        Platform.runLater(() -> {
                            var selected = cmbSlots.getValue();
                            cmbSlots.setItems(FXCollections.observableArrayList(slots));
                            if (selected != null && slots.stream().anyMatch(s ->
                                    s.from().equals(selected.from()) && s.to().equals(selected.to()))) {
                                cmbSlots.getSelectionModel().select(selected);
                            }
                        });
                        TableUtils.applyDelta(apptEditable, mapped, ApptRow::getId);
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> error("Auto refresh", e));
                }
            }, "ui-coalesced-refresh").start();
        });
    }

    /**
     * استماع لقناة DB NOTIFY
     */

    private void startDbNotifications() {
        apptDbListener = new DbNotifications();

        // appointments_changed -> فلش كاش الساعات وجدّد الشاشة
        apptDbListener.listen("appointments_changed", payload -> {
            slotCache.clear();
            scheduleCoalescedRefresh();
        });

        // patients_changed -> مستمع واحد مع debounce
        apptDbListener.listen("patients_changed", payload -> {
            System.out.println("NOTIFY patients_changed: " + payload);
            uiRefresh.request(this::loadPatientsBG);
        });

        System.out.println("DbNotifications: starting listeners...");
    }


    /**
     * Poll احتياطي خفيف فقط (تعطيل التحديث كل 1 دقيقة)
     */
    private void startAutoRefresh() {
        autoRefreshExec.scheduleAtFixedRate(this::scheduleCoalescedRefresh, 10, 10, TimeUnit.SECONDS);
        autoRefreshExec.scheduleAtFixedRate(() -> {
            try {
                loadPatientsBG();
                DoctorDAO.loadDoctorsBG();
            } catch (Exception ignore) {
            }
        }, 0, 60, TimeUnit.SECONDS);
        // داخل startAutoRefresh()
    }

    // ==== بقية الدوال كما كانت (loadDoctorsBG, CRUD, إلخ) ====

    /* ===== Helpers (alerts & online guard wrapper) ===== */
    private boolean ensureOnlineOrAlert() {
        if (monitor != null && !monitor.isOnline()) {
            showWarn("Offline", "You're offline. Please reconnect and try again.");
            return false;
        }
        return true;
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

    private String showError(String title, Exception ex) {
        if (ex != null) ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex == null ? title : ex.getMessage());
        a.showAndWait();
        return ex == null ? title : ex.getMessage();
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /* ===== Patients CRUD (restored) ===== */
    private void doInsertPatient() {
        String fullName = trimOrNull(FullNameTextField.getText());
        String nid = trimOrNull(PatientIdTextField.getText());
        Gender gender = GenderComboBox.getValue();
        LocalDate dob = DateOfBirthPicker.getValue();
        String phone = trimOrNull(PhoneTextField.getText());
        String history = trimOrNull(medicalHistory.getText());

        if (fullName == null || dob == null || gender == null) {
            showWarn("Validation", "Full name, gender and date of birth are required.");
            return;
        }
        if (phone == null) {
            showWarn("Validation", "Patient must have a phone number.");
            return;
        }

        try {
            // ننشئ المريض – ما بنعتمد على PatientView
            patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);

            // نحدّث الجدول من المصدر الرسمي (PatientRow)
            loadPatientsBG();

            clearForm();
            showInfo("Insert", "Patient inserted successfully.");
        } catch (Exception ex) {
            showError("Insert Patient", ex);
            return;
        }

        // إشعار قنوات الـ DB NOTIFY (غير حرِج لو فشل)
        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','insert')")) {
            nps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void doUpdatePatient() {
        PatientRow row = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Update", "Select a patient row first.");
            return;
        }

        String fullName = trimOrNull(FullNameTextField.getText());
        String nid = trimOrNull(PatientIdTextField.getText());
        String phone = trimOrNull(PhoneTextField.getText());
        String history = trimOrNull(medicalHistory.getText());
        Gender gender = GenderComboBox.getValue();
        LocalDate dob = DateOfBirthPicker.getValue();


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
            if (patientTable != null) patientTable.refresh();
            showInfo("Update", "Patient updated successfully.");
        } catch (Exception ex) {
            showError("Update Patient", ex);
        }

        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','update')")) {
            nps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doDeletePatient() {
        PatientRow row = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
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

        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','delete')")) {
            nps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearForm() {
        if (FullNameTextField != null) FullNameTextField.clear();
        if (PatientIdTextField != null) PatientIdTextField.clear();
        if (PhoneTextField != null) PhoneTextField.clear();
        if (medicalHistory != null) medicalHistory.clear();
        if (GenderComboBox != null) GenderComboBox.setValue(Gender.MALE);
        if (DateOfBirthPicker != null) DateOfBirthPicker.setValue(null);
        if (patientTable != null) patientTable.getSelectionModel().clearSelection();
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ===== Appointments table wiring & search (minimal) ===== */
    private void wireAppointmentsTables() {
        if (TableINAppointment == null) return;
        TableINAppointment.setItems(apptEditable);
        // === تفعيل التحرير داخل جدول المواعيد ===
        TableINAppointment.setEditable(true);
        colDateAppointment.setEditable(true);

        setupInlineEditing();
        if (colAppointmentIDAppointment != null)
            colAppointmentIDAppointment.setCellValueFactory(cd -> cd.getValue().idProperty());
        if (colDateAppointment != null) colDateAppointment.setCellValueFactory(cd -> cd.getValue().dateProperty());
        if (colDoctorNameAppointment != null)
            colDoctorNameAppointment.setCellValueFactory(cd -> cd.getValue().doctorNameProperty());

        //        --------------------
        //        --------------------
        if (colPatientNameAppointment != null)
            colPatientNameAppointment.setCellValueFactory(cd -> cd.getValue().patientNameProperty());
        if (colSpecialty != null) colSpecialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
        if (colStatusAppointment != null)
            colStatusAppointment.setCellValueFactory(cd -> cd.getValue().statusProperty());

        // --- Start Time column (يعرض الوقت بصيغة 12h)
        if (colStartTime != null) {
            colStartTime.setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(fmt12(cd.getValue().getTime()))
            );

            // تحرير الوقت كنص: يقبل HH:mm أو hh:mm AM/PM
            colStartTime.setCellFactory(
                    javafx.scene.control.cell.TextFieldTableCell.forTableColumn(
                            new javafx.util.StringConverter<String>() {
                                @Override public String toString(String s) { return s == null ? "" : s; }
                                @Override public String fromString(String s) { return (s == null) ? null : s.trim(); }
                            }
                    )
            );

            colStartTime.setOnEditCommit(ev -> {
                var row = ev.getRowValue();
                String txt = ev.getNewValue();
                if (row == null || txt == null || txt.isBlank()) return;
                try {
                    java.time.LocalTime nt;
                    try {
                        nt = java.time.LocalTime.parse(txt.trim()); // HH:mm
                    } catch (Exception e1) {
                        nt = java.time.LocalTime.parse(
                                txt.trim().toUpperCase(),
                                java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                        ); // hh:mm AM/PM
                    }
                    row.setTime(nt);
                    if (row.getId() > 0 && row.getDate() != null) {
                        updateAppointmentStartAt(row.getId(), row.getDate(), nt);
                    }
                    if (TableINAppointment != null) TableINAppointment.refresh();
                    if (row.getId() <= 0) { row.setDirty(true); }
                    updateDirtyAlert();
                } catch (Exception ex) {
                    showError("Invalid time", new RuntimeException("Use HH:mm or hh:mm AM/PM"));
                }
            });
        }
        if (colRoomNumber != null) {
            colRoomNumber.setEditable(false);
            colRoomNumber.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    cd.getValue().getRoomNumber() == null ? "" : cd.getValue().getRoomNumber()
            ));
        }

        // Date as DatePicker
        if (colDateAppointment != null) {
            colDateAppointment.setCellFactory(col -> datePickerCell());
        }
        // Session time column: ثابت 20 دقيقة، غير قابل للتحرير
        if (colSessionTime != null) {
            colSessionTime.setEditable(true);
            colSessionTime.setCellValueFactory(cd -> new SimpleIntegerProperty(
                    cd.getValue().getSessionTime() > 0 ? cd.getValue().getSessionTime() : DEFAULT_SESSION_MIN
            ));

            // محرر نصي يسمح بإدخال رقم فقط
            colSessionTime.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.StringConverter<Number>() {
                @Override
                public String toString(Number n) {
                    return (n == null) ? "" : n.toString();
                }

                @Override
                public Number fromString(String s) {
                    try {
                        int v = Integer.parseInt(s.trim());
                        return v > 0 ? v : DEFAULT_SESSION_MIN;
                    } catch (Exception e) {
                        return DEFAULT_SESSION_MIN; // fallback
                    }
                }
            }));

            // عند تعديل القيمة
            colSessionTime.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                int newVal = ev.getNewValue().intValue();
                if (newVal <= 0) newVal = DEFAULT_SESSION_MIN; // دائماً على الأقل 20
                row.setSessionTime(newVal);
                row.setDirty(true);
                updateDirtyAlert();

                if (TableINAppointment != null)
                    TableINAppointment.refresh();
            });
        }
        if (TableINAppointment != null) TableINAppointment.refresh();
    }

    /**
     * تهيئة التحرير المباشر على جدول المواعيد
     */
    private void setupInlineEditing() {
        if (TableINAppointment == null) return;

        // Patient name inline (kept local only)
        if (colPatientNameAppointment != null) {
            colPatientNameAppointment.setEditable(true);
            colPatientNameAppointment.setCellFactory(TextFieldTableCell.forTableColumn());
            colPatientNameAppointment.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                String v = safe(ev.getNewValue());
                if (row == null || v == null) return;
                row.setPatientName(v);
                row.setDirty(true);
                updateDirtyAlert();
            });
        }

        // Specialty as ComboBox
        if (colSpecialty != null) {
            ensureSpecialtiesLoadedAsync();
            colSpecialty.setEditable(true);
            colSpecialty.setCellFactory(ComboBoxTableCell.forTableColumn(specialtyChoices));
            colSpecialty.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                String sp = ev.getNewValue();
                if (row == null || sp == null) return;
                // حدّث التخصص
                row.setSpecialty(sp);
                // Specialty تغيّر ⇒ لازم نلغي أي اختيار دكتور وغرفة
                row.setDoctorId(0);
                row.setDoctorName(null);
                row.setRoomNumber(null);
                row.setDirty(true);
                updateDirtyAlert();
                ensureDoctorsForSpecAsync(sp);  // حمّل قائمة أطباء التخصص الجديد
                if (TableINAppointment != null) TableINAppointment.refresh();
            });
        }

        // Doctor column uses our custom cell that only shows the ComboBox while editing/selected
        if (colDoctorNameAppointment != null) {
            colDoctorNameAppointment.setEditable(true);
            colDoctorNameAppointment.setCellFactory(col -> doctorComboCell());
        }


        // Status column as ComboBox
        if (colStatusAppointment != null) {
            ObservableList<String> statuses = FXCollections.observableArrayList("SCHEDULED", "COMPLETED", "CANCELLED");
            colStatusAppointment.setEditable(true);
            colStatusAppointment.setCellFactory(ComboBoxTableCell.forTableColumn(statuses));
            colStatusAppointment.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                String st = ev.getNewValue();
                if (row == null || st == null) return;
                row.setStatus(st);
                row.setDirty(true);
                updateDirtyAlert();
            });
        }
    }

    // Add or focus a draft appointment row for a selected patient
    private void addOrFocusDraftForPatient(PatientRow p) {
        if (p == null || TableINAppointment == null) return;
        for (ApptRow r : apptEditable) {
            if (p.getFullName().equals(r.getPatientName()) && r.isNew()) {
                TableINAppointment.getSelectionModel().select(r);
                TableINAppointment.scrollTo(r);
                return;
            }
        }
        ApptRow r = new ApptRow();
        r.setId(0);
        r.setNew(true);
        r.setDirty(true);
        r.setPatientName(p.getFullName());
        r.setStatus("SCHEDULED");
        r.setDate(AppointmentDate != null && AppointmentDate.getValue() != null ? AppointmentDate.getValue() : LocalDate.now());
        apptEditable.add(0, r);
        TableINAppointment.getSelectionModel().select(r);
        TableINAppointment.scrollTo(r);
    }

    // Add a blank draft row from the + button
    private void addBlankDraftRow() {
        if (TableINAppointment == null) return;
        ApptRow r = new ApptRow();
//        r.setId(0);
        r.setNew(true);
        r.setDirty(true);
        r.setStatus("PENDING");
        r.setDate(LocalDate.now());
        apptEditable.add(0, r);
        TableINAppointment.getSelectionModel().select(r);
        TableINAppointment.scrollTo(r);
    }

    // Clear mini booking form on the left (specialty/doctor/slot)
    private void doClearAppointmentForm() {
        if (DoctorspecialtyApp != null) DoctorspecialtyApp.getSelectionModel().clearSelection();
        if (avilabelDoctorApp != null) avilabelDoctorApp.getSelectionModel().clearSelection();
        if (AppointmentDate != null) AppointmentDate.setValue(LocalDate.now());
        if (cmbSlots != null) cmbSlots.getItems().clear();
    }

    // Load appointments table once (used at init)
    private void loadAppointmentsTable() {
        try {
            var apptRows = AppointmentJdbcDAO.listScheduledAppointments();
            var mapped = FXCollections.<ApptRow>observableArrayList();
            for (var r : apptRows) {
                ApptRow ar = new ApptRow();
                ar.setId(r.id);
                ar.setDoctorId(r.doctorId);
                ar.setDoctorName(r.doctorName);
                ar.setPatientName(r.patientName);
                ar.setSpecialty(r.specialty);
                ar.setStatus(r.status);
                java.time.LocalDateTime ldt2 = toLocal(r.startAt);
                if (ldt2 != null) {
                    ar.setDate(ldt2.toLocalDate());
                    ar.setTime(ldt2.toLocalTime());
                }
                ar.setRoomNumber(r.location);
                ar.setNew(false);
                ar.setDirty(false);
                mapped.add(ar);
            }
            Platform.runLater(() -> TableUtils.applyDelta(apptEditable, mapped, ApptRow::getId));
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Load Appointments", ex));
        }
    }

    private void updateAppointmentCounters() {
        try {
            int doctors = AppointmentJdbcDAO.countAvailableDoctors();
            int appts = AppointmentJdbcDAO.countAppointments();
            int patients = AppointmentJdbcDAO.countPatients();
            int completed = AppointmentJdbcDAO.countCompletedAppointments();
            int scheduled = AppointmentJdbcDAO.countScheduledAppointments();
            Platform.runLater(() -> {
                if (NumberOfTotalDoctors != null) NumberOfTotalDoctors.setText(String.valueOf(doctors));
                if (NumberOfTotalAppointments != null) NumberOfTotalAppointments.setText(String.valueOf(appts));
                if (NumberOfTotalPatients != null) NumberOfTotalPatients.setText(String.valueOf(patients));
                if (patientCompleteNum != null) patientCompleteNum.setText(String.valueOf(completed));
                if (RemainingNum != null) RemainingNum.setText(String.valueOf(scheduled));
                updatePatientDetailsChart();
            });
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Counters", ex));
        }
    }

    // Update start_at field for an appointment
    private void updateAppointmentStartAt(long id, LocalDate d, LocalTime t) {
        if (id <= 0 || d == null || t == null) return;
        final String sql = "UPDATE appointments SET appointment_date = ?, updated_at = now() WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            java.time.OffsetDateTime startAt = toAppOffset(d, t); // Asia/Gaza
            ps.setObject(1, startAt); // write timestamptz correctly
            ps.setLong(2, id);
            ps.executeUpdate();
            try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','update')")) {
                n.execute();
            }
        } catch (SQLException e) {
            Platform.runLater(() -> showError("Update appointment_date", e));
        }
    }

    // Delete currently selected appointment
    private void doDeleteAppointment() {
        var row = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
        if (row == null) { showWarn("Delete", "Select an appointment row first."); return; }
        if (!confirm("Delete", "Delete appointment #" + row.getId() + "?")) return;

        try (Connection c = Database.get()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM appointments WHERE id = ?")) {
                ps.setLong(1, row.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','delete')")) {
                n.execute();
            }
            showInfo("Delete", "Appointment deleted.");
            scheduleCoalescedRefresh();
        } catch (Exception e) {
            showError("Delete Appointment", e);
        }
    }

    private void doInsertAppointment() {
        try {
            Long doctorId = null;
            LocalDate day = null;
            LocalTime time = null;
            Integer duration = null;
            String location = null;

            // ===== Path A: from mini booking form (preferred) =====
            var formDoc = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
            var formDay = (AppointmentDate == null) ? null : AppointmentDate.getValue();
            var formSlot = (cmbSlots == null) ? null : cmbSlots.getValue();
            if (formDoc != null && formDay != null && formSlot != null) {
                doctorId = formDoc.doctorId;
                day = formDay;
                time = formSlot.from().toLocalTime();
                duration = (int) java.time.Duration.between(formSlot.from(), formSlot.to()).toMinutes();
            }

            // ===== Path B: from selected table row (draft row) =====
            if (doctorId == null || day == null || time == null) {
                ApptRow row = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
                if (row != null) {
                    if (row.getDoctorId() > 0) doctorId = row.getDoctorId();
                    if (row.getDate() != null) day = row.getDate();
                    if (row.getTime() != null) time = row.getTime();
                    if (row.getRoomNumber() != null && !row.getRoomNumber().isBlank()) location = row.getRoomNumber();
                    // استخدم مدة الجدولة إن وُجدت وإلا القيمة الافتراضية
                    duration = (row.getSessionTime() > 0) ? row.getSessionTime() : DEFAULT_SESSION_MIN;
                }
            }

            // ===== Validation =====
            if (doctorId == null || day == null || time == null) {
                showWarn("Insert Appointment", "Select specialty, doctor and time slot.");
                return;
            }

            Long patientId = resolvePatientId();
            if (patientId == null) {
                showWarn("Insert Appointment", "Invalid Patient. Select a patient from the table or enter a valid Patient ID / National ID.");
                return;
            }

            if (duration == null || duration <= 0) duration = DEFAULT_SESSION_MIN;
            java.time.OffsetDateTime startAt = toAppOffset(day, time);

            final String sql = """
                INSERT INTO appointments
                  (doctor_id, patient_id, appointment_date, duration_minutes, status, location, created_by, created_at, updated_at)
                VALUES
                  (?, ?, ?, ?, 'SCHEDULED'::appt_status, COALESCE(?, (SELECT room_number FROM doctors WHERE id=?)), ?, now(), now())
                RETURNING id, doctor_id, patient_id, appointment_date, duration_minutes, status, location
            """;

            ApptRow draft = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
            try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, doctorId);
                ps.setLong(2, patientId);
                ps.setObject(3, startAt);
                ps.setInt(4, duration);
                if (location != null) ps.setString(5, location); else ps.setNull(5, Types.VARCHAR);
                ps.setLong(6, doctorId); // for COALESCE subselect
                ps.setLong(7, Session.get().getId());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // ابني صفًا جديدًا من قيم الداتابيز
                        ApptRow ar = new ApptRow();
                        ar.setId(rs.getLong("id"));
                        ar.setDoctorId(rs.getLong("doctor_id"));
                        ar.setDoctorName((formDoc != null) ? formDoc.fullName : (draft != null ? draft.getDoctorName() : null));
                        ar.setPatientName((draft != null) ? draft.getPatientName() : null); // للعرض فقط
                        ar.setSpecialty((draft != null) ? draft.getSpecialty() : null);
                        ar.setStatus(rs.getString("status"));
                        java.time.OffsetDateTime odt = rs.getObject("appointment_date", java.time.OffsetDateTime.class);
                        java.time.LocalDateTime ldt = toLocal(odt);
                        if (ldt != null) {
                            ar.setDate(ldt.toLocalDate());
                            ar.setTime(ldt.toLocalTime());
                        }
                        String loc = rs.getString("location");
                        ar.setRoomNumber(loc != null ? loc : (draft != null ? draft.getRoomNumber() : null));
                        ar.setNew(false);
                        ar.setDirty(false);

                        // أزل صف المسودة أو استبدله مباشرة بالمدخل الجديد
                        if (draft != null && apptEditable.contains(draft)) {
                            int idx = apptEditable.indexOf(draft);
                            apptEditable.set(idx, ar);
                        } else {
                            apptEditable.add(0, ar);
                        }

                        // اختَر الصف الجديد ومرّره للفوكس
                        if (TableINAppointment != null) {
                            TableINAppointment.getSelectionModel().select(ar);
                            TableINAppointment.scrollTo(ar);
                        }
                    }
                }

                try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','insert')")) { n.execute(); }
            }

            showInfo("Insert", "Appointment created.");
            scheduleCoalescedRefresh();
        } catch (Exception e) {
            if (e instanceof java.sql.SQLException se && "23505".equals(se.getSQLState())) {
                showWarn("Insert Appointment", "Conflict: another appointment exists for the same doctor or room at this start time.");
                return;
            }
            showError("Insert Appointment", e);
        }
    }

    private void doUpdateAppointment() {
        var row = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
        if (row == null) { showWarn("Update", "Select an appointment row first."); return; }

        try (Connection c = Database.get()) {
            String sql = "UPDATE appointments SET doctor_id=?, appointment_date=?, duration_minutes=?, " +
                         "location = COALESCE(?, (SELECT room_number FROM doctors WHERE id=?)), " +
                         "status=?::appt_status, updated_at=now() WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                java.time.OffsetDateTime startAt = (row.getDate() != null && row.getTime() != null)
                        ? toAppOffset(row.getDate(), row.getTime())
                        : null;
                int duration = (row.getSessionTime() > 0) ? row.getSessionTime() : DEFAULT_SESSION_MIN;

                ps.setLong(1, row.getDoctorId());
                if (startAt != null) ps.setObject(2, startAt); else ps.setNull(2, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
                ps.setInt(3, duration);

                if (row.getRoomNumber() != null && !row.getRoomNumber().isBlank())
                    ps.setString(4, row.getRoomNumber());
                else
                    ps.setNull(4, Types.VARCHAR);

                ps.setLong(5, row.getDoctorId()); // for COALESCE subselect

                if (row.getStatus() != null && !row.getStatus().isBlank())
                    ps.setString(6, row.getStatus());
                else
                    ps.setNull(6, Types.OTHER);

                ps.setLong(7, row.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','update')")) {
                n.execute();
            }
            showInfo("Update", "Appointment updated.");
            scheduleCoalescedRefresh();
        } catch (Exception e) {
            if (e instanceof java.sql.SQLException se && "23505".equals(se.getSQLState())) {
                showWarn("Update Appointment", "Conflict: another appointment exists for the same doctor or room at this start time.");
                return;
            }
            showError("Update Appointment", e);
        }
    }


    private void wireDashboardAppointmentsSearch() { /* optional: implement filtering for dashboard table */ }

//    private void loadDoctorsBG() { /* optional: load doctors into doctorData if needed */ }

    private void updatePatientDetailsChart() {
        if (appointmentStatusChart == null) return;
        appointmentStatusChart.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Appointments");
        s.getData().add(new XYChart.Data<>("Scheduled", apptEditable.stream().filter(r -> "SCHEDULED".equalsIgnoreCase(r.getStatus())).count()));
        s.getData().add(new XYChart.Data<>("Completed", apptEditable.stream().filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus())).count()));
        s.getData().add(new XYChart.Data<>("Cancelled", apptEditable.stream().filter(r -> "CANCELLED".equalsIgnoreCase(r.getStatus())).count()));
        appointmentStatusChart.getData().add(s);
    }


    // Graceful shutdown for listeners/executors
    void shutdown() {
        try { if (apptDbListener != null) apptDbListener.close(); } catch (Exception ignore) {}
        try { autoRefreshExec.shutdownNow(); } catch (Exception ignore) {}
        try { if (monitor != null) monitor.stop(); } catch (Exception ignore) {}
    }
//}
    /** Resolve patient_id from UI: try selected patient row; then try numeric id; then fallback to national_id (9-digit). */
    private Long resolvePatientId() {
        // 1) From selected patient row in patients table (most reliable)
        try {
            PatientRow sel = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getPatientId() > 0) return sel.getPatientId();
        } catch (Exception ignore) {}

        // 2) From text field (could be internal id or national id)
        String raw = (getPatientID == null) ? null : getPatientID.getText();
        if (raw == null) raw = (PatientIdTextField == null) ? null : PatientIdTextField.getText();
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // 2a) If numeric and not 9 digits, treat as internal patients.id
        if (s.matches("\\d+") && s.length() != 9) {
            long id = Long.parseLong(s);
            if (patientExistsById(id)) return id;
        }

        // 2b) If 9-digit, treat as national_id and lookup patients.id via users
        if (s.matches("\\d{9}")) {
            Long pid = findPatientIdByNationalId(s);
            if (pid != null) return pid;
        }

        // 2c) Last chance: numeric direct id (even if 9 digits) – verify existence
        if (s.matches("\\d+")) {
            long id = Long.parseLong(s);
            if (patientExistsById(id)) return id;
        }

        return null;
    }

    private boolean patientExistsById(long id) {
        final String sql = "SELECT 1 FROM patients WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    private Long findPatientIdByNationalId(String nid) {
        final String sql = "SELECT p.id FROM patients p JOIN users u ON u.id = p.user_id WHERE u.national_id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException ignore) {}
        return null;
    }
}