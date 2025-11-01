package com.example.healthflow.controllers;

import com.example.healthflow.dao.PrescriptionDAO;
import com.example.healthflow.dao.PrescriptionDAO.DashboardRow;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.dao.PrescriptionItemDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.PrescriptionItem;
import com.example.healthflow.model.ItemStatus;
import com.example.healthflow.model.dto.PrescItemRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.scene.Cursor;
import com.example.healthflow.core.packaging.PackagingSupport;
import com.example.healthflow.core.packaging.PackagingSupport.PackagingInfo;
import com.example.healthflow.core.packaging.PackagingSupport.PackSuggestion;
import com.example.healthflow.db.notify.DbNotifications;

import javafx.util.Duration;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.beans.value.ChangeListener;
import org.controlsfx.control.SegmentedButton;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

public class PharmacyController {

    private final ConnectivityMonitor monitor;
    // Controller state
    private DashboardRow selectedRow;
    private final ObservableList<DashboardRow> dashboardRows = FXCollections.observableArrayList();
    private final ObservableList<PrescItemRow> itemRows = FXCollections.observableArrayList();

    /* ====== Utilities / constants (match DoctorController style) ====== */
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");
    private static final String ACTIVE_CLASS = "current";

    // ===== Dashboard loading state & debounce =====
    private volatile boolean dashboardLoading = false;
    private LocalDate lastLoadedDate = null;
    private PauseTransition dpDebounce;


    @FXML
    private VBox rootPane;

    @FXML
    private Label AppointmentDate;

    @FXML
    private Label ApprovedNumber;

    @FXML
    private Button BackButton;

    @FXML
    private AnchorPane CenterAnchorPane;

    @FXML
    private Button DashboardButton;

    @FXML
    private Label DateOfDay;

    @FXML
    private Label DiagnosisView;

    @FXML
    private Label DoctorNameLabel;

    @FXML
    private Label ExpiringSoonMedicine;

    @FXML
    private DatePicker ExpiryDate;

    @FXML
    private Button Finish_Prescription;

    @FXML
    private AnchorPane InventoryAnchorPane;

    @FXML
    private Button InventoryButton;

    @FXML
    private Label LowStockMedicine;

    @FXML
    private TextField MedicineNameRecive;

    @FXML
    private TextField PatientNameTF;

    @FXML
    private Label PendingNumber;

    @FXML
    private Label PharmacistNameLabel;

    @FXML
    private TableView<DashboardRow> PresciptionsTable;

    @FXML
    private AnchorPane PrescriptionAnchorPane;

    @FXML
    private AnchorPane PrescriptionMedicationAnchorPane;

    @FXML
    private Button PrescriptionsButton;

    @FXML
    private Label PrescriptionsCompleteNum;

    @FXML
    private Label PrescriptionsTodayTotal;

    @FXML
    private Label PrescriptionsWatingNum;
    @FXML
    private Label Prescription_number;

    @FXML
    private TextArea ReasonOfDeduct;

    @FXML
    private AnchorPane ReceivePane;

    @FXML
    private Label RejectedNumber;

    @FXML
    private Button ReportsButton;

    @FXML
    private TableView<?> TableMedicinesInentory;

    @FXML
    private TableView<PrescItemRow> TablePrescriptionItems;

    @FXML
    private DatePicker PrescriptionDatePicker;


    @FXML
    private TableView<?> TableToShowMedicineByBatchNumber;

    @FXML
    private Label TitleBar;

    @FXML
    private Label TotalAppointments2;

    @FXML
    private Label TotalAppointments21;

    @FXML
    private Label TotalAppointments22;

    @FXML
    private Label TotalItemsNumber;

    @FXML
    private Label TotalmedicinesNumber;

    @FXML
    private Label UserIdLabel;

    @FXML
    private Label UsernameLabel;

    @FXML
    private Label alertLabel;

    @FXML
    private FontIcon back;

    @FXML
    private TextField batchNum;

    @FXML
    private ToggleButton btnDeduct;

    @FXML
    private ToggleButton btnReceive;

    @FXML
    private TableColumn<DashboardRow, Void> colActionPhDashbord;

    @FXML
    private TableColumn<DashboardRow, String> colDate_Time;

    @FXML
    private TableColumn<PrescItemRow, Number> colStock;

    @FXML
    private TableColumn<PrescItemRow, String> colItemStatus;

    @FXML
    private TableColumn<?, ?> colDiagnosisInentory;

    @FXML
    private TableColumn<DashboardRow, String> colDoctorName;

    @FXML
    private TableColumn<PrescItemRow, String> colDosage;

    @FXML
    private TableColumn<?, ?> colDosageInentory;

    @FXML
    private TableColumn<PrescItemRow, Number> colDose;

    @FXML
    private TableColumn<PrescItemRow, String> colForm;

    @FXML
    private TableColumn<PrescItemRow, Number> colIdx;

    @FXML
    private TableColumn<?, ?> colIdx11;

    @FXML
    private TableColumn<PrescItemRow, String> colMedicineName;

    @FXML
    private TableColumn<?, ?> colMedicineNameInentory;

    @FXML
    private TableColumn<?, ?> colMedicineNameInentoryByBatchNumber;

    @FXML
    private TableColumn<DashboardRow, String> colPatientName;

    @FXML
    private TableColumn<PrescItemRow, Void> colPresesItemAction;

    @FXML
    private TableColumn<?, ?> colPresesActionInentory;

    @FXML
    private TableColumn<PrescItemRow, Number> colQuantity;
    @FXML
    private TableColumn<PrescItemRow, Number> colSuggestionQty;
    // (display helper; we render text inside colSuggestionQty via cellFactory)
    private static final String SUGG_PLACEHOLDER = "—";

    @FXML
    private TableColumn<?, ?> colQuantityInentory;

    @FXML
    private TableColumn<DashboardRow, Number> colSerialPhDashboard;

    @FXML
    private TableColumn<PrescItemRow, String> colStrength;

    @FXML
    private TableColumn<DashboardRow, String> colprescriptionStutus;


    @FXML
    private TextField deductBatchNumber;

    @FXML
    private AnchorPane deductPane;

    @FXML
    private TextField guantityToDeduct;

    @FXML
    private Label lableToShowMedicinesName;

    @FXML
    private AnchorPane pharmacyDashboardAnchorPane;

    @FXML
    private TextField quantity;

    @FXML
    private Button saveBtnDeduct;

    @FXML
    private Button saveBtnReceive;

    @FXML
    private TextField searchDashbord;

    @FXML
    private TextField searchItems;

    @FXML
    private TextField searchOnInentory;

    @FXML
    private SegmentedButton segInv;

    @FXML
    private Label time;

    @FXML
    private Label userStatus;

    @FXML
    private Label welcomeUser;

    private volatile java.time.Instant lastPrescItemsFp = null;   // fingerprint لعناصر الوصفة المفتوحة
    private volatile java.time.Instant lastInventoryFp  = null;   // fingerprint للمخزون


    private com.example.healthflow.db.notify.DbNotifications dbn;
    private final javafx.animation.PauseTransition dashCoalesce  = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
    private final javafx.animation.PauseTransition itemsCoalesce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
    private final javafx.animation.PauseTransition invCoalesce   = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));


    private static final class DashboardData {
        int total;
        int waiting;
        int completed;
        java.util.List<PrescriptionDAO.DashboardRow> rows;
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean value) {
        if (node != null) {
            node.setVisible(value);
            node.setManaged(value);
        }
    }

    private void markNavActive(Button active) {
        Button[] all = {DashboardButton, PrescriptionsButton, InventoryButton};
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove(ACTIVE_CLASS);
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
            active.getStyleClass().add(ACTIVE_CLASS);
        }
    }

    private void startClock() {
        if (time != null) {
            var tf = java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a");
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> time.setText(java.time.ZonedDateTime.now(APP_TZ).format(tf))),
                    new KeyFrame(Duration.seconds(1))
            );
            tl.setCycleCount(Timeline.INDEFINITE);
            tl.play();
        }
        if (DateOfDay != null) {
            var df = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
            var todayGaza = java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
            DateOfDay.setText(todayGaza.format(df));
        }
    }

    // Resolve pharmacist/user display names and support both users.id and pharmacists.id in UserIdLabel.
    // Resolve pharmacist/user labels from the authenticated session FIRST; then fall back to labels/DB.
    private void resolveLoggedInUserLabels() {
        try {
            String full = null;
            Long userId = null;

            // --- 0) Primary source: Auth session (set by LoginController) ---
            try {
                var u = Session.get();
                if (u != null) {
                    userId = u.getId();
                    full = u.getFullName();
                }
            } catch (Throwable ignored) {}

            // --- 1) If session missing, try reading users.id from UserIdLabel and fetch full_name ---
            if (full == null || full.isBlank()) {
                Long rawId = null;
                if (UserIdLabel != null) {
                    String uidTxt = UserIdLabel.getText();
                    if (uidTxt != null) {
                        uidTxt = uidTxt.replaceAll("[^0-9]", "").trim();
                        if (!uidTxt.isEmpty()) {
                            try { rawId = Long.parseLong(uidTxt); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                if (rawId != null) {
                    try (Connection c = Database.get();
                         PreparedStatement ps = c.prepareStatement("SELECT id, full_name FROM users WHERE id = ?")) {
                        ps.setLong(1, rawId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getLong(1);
                                full = rs.getString(2);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[PharmacyController] resolveLoggedInUserLabels users.id lookup failed: " + e.getMessage());
                    }
                }
            }

            // --- 2) Final fallback: use UsernameLabel text if it looks real ---
            if ((full == null || full.isBlank()) && UsernameLabel != null) {
                String txt = UsernameLabel.getText();
                if (txt != null && !txt.isBlank() && !txt.equalsIgnoreCase("user name")) {
                    full = txt.trim();
                }
            }

            // --- Apply to UI ---
            if (full != null && !full.isBlank()) {
                String first = full.trim().split("\\s+")[0];
                if (welcomeUser != null) welcomeUser.setText(first);
                if (PharmacistNameLabel != null) PharmacistNameLabel.setText(full);
                if (UsernameLabel != null) UsernameLabel.setText(full);
            } else {
                System.out.println("[PharmacyController] resolveLoggedInUserLabels: full name not resolved; check Session and labels.");
            }

            if (UserIdLabel != null && userId != null) {
                UserIdLabel.setText(String.valueOf(userId));
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] resolveLoggedInUserLabels: " + ex);
        }
    }
    // Helper to get selected date or today (Asia/Gaza)
    private LocalDate getSelectedDateOrToday() {
        try {
            LocalDate picked = (PrescriptionDatePicker != null) ? PrescriptionDatePicker.getValue() : null;
            if (picked != null) return picked;
        } catch (Throwable ignored) {}
        return java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
    }

    // Helper: is the currently opened prescription read-only (completed)?
    private boolean isCurrentPrescriptionReadOnly() {
        return selectedRow != null && selectedRow.status != null && "DISPENSED".equalsIgnoreCase(selectedRow.status.name());
    }

    /** Resolve pharmacist_id strictly (no auto-create). */
    private Long requireCurrentPharmacistId() {
        Long userId = null; String email = null; String role = null;
        try { var u = Session.get(); if (u != null) { userId = u.getId(); email = u.getEmail(); } } catch (Throwable ignored) {}
        if (userId == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "No logged-in user. Please re-login.").showAndWait();
            return null;
        }
        try (Connection c = Database.get()) {
            // role + email
            try (PreparedStatement ps = c.prepareStatement("SELECT role, email FROM users WHERE id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { role = rs.getString(1); if (email == null) email = rs.getString(2); } }
            }
            if (role == null || !"PHARMACIST".equalsIgnoreCase(role)) {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                        "You are not authorized as a pharmacist (role='" + String.valueOf(role) + "').").showAndWait();
                return null;
            }
            // by user_id
            Long pharmId = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM pharmacists WHERE user_id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pharmId = rs.getLong(1); }
            }
            if (pharmId != null) return pharmId;

            // by email join (handles migrated data)
            if (email != null && !email.isBlank()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT p.id FROM pharmacists p JOIN users u ON u.id = p.user_id WHERE LOWER(u.email)=LOWER(?) LIMIT 1")) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pharmId = rs.getLong(1); }
                }
                if (pharmId != null) return pharmId;
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] requireCurrentPharmacistId error: " + ex);
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to resolve pharmacist: " + ex.getMessage()).showAndWait();
            return null;
        }
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                "Your account has role PHARMACIST but is not linked in pharmacists table. Please ask admin to link it.").showAndWait();
        return null;
    }

    private java.time.OffsetDateTime fetchAppointmentDateById(Long apptId) {
        if (apptId == null) return null;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT appointment_date FROM appointments WHERE id = ?")) {
            ps.setLong(1, apptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    if (ts != null) return ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
                }
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] fetchAppointmentDateById: " + ex);
        }
        return null;
    }

    private void refreshPharmacyDashboardCounts() {
        if (PrescriptionsTodayTotal == null && PrescriptionsWatingNum == null && PrescriptionsCompleteNum == null) return;
        LocalDate day = getSelectedDateOrToday();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("""
             SELECT
                 COUNT(*) AS total,
                 COUNT(*) FILTER (WHERE status = 'PENDING') AS waiting,
                 COUNT(*) FILTER (WHERE status IN ('APPROVED','DISPENSED')) AS completed
             FROM prescriptions
             WHERE created_at::date = ?
         """)) {
            ps.setDate(1, java.sql.Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (PrescriptionsTodayTotal != null) PrescriptionsTodayTotal.setText(String.valueOf(rs.getInt("total")));
                    if (PrescriptionsWatingNum != null) PrescriptionsWatingNum.setText(String.valueOf(rs.getInt("waiting")));
                    if (PrescriptionsCompleteNum != null) PrescriptionsCompleteNum.setText(String.valueOf(rs.getInt("completed")));
                }
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] refreshPharmacyDashboardCounts: " + ex);
            if (alertLabel != null) alertLabel.setText("Failed to refresh counts: " + ex.getMessage());
        }
    }

    /* ====== Diagnostics for FXML wiring ====== */
    private boolean warnIfMissing() {
        StringBuilder sb = new StringBuilder();
        if (pharmacyDashboardAnchorPane == null) sb.append("pharmacyDashboardAnchorPane, ");
        if (PrescriptionAnchorPane == null)     sb.append("PrescriptionAnchorPane, ");
        if (InventoryAnchorPane == null)        sb.append("InventoryAnchorPane, ");
        if (DashboardButton == null)            sb.append("DashboardButton, ");
        if (PrescriptionsButton == null)        sb.append("PrescriptionsButton, ");
        if (InventoryButton == null)            sb.append("InventoryButton, ");
        if (BackButton == null)                 sb.append("BackButton, ");
        if (btnReceive == null)                 sb.append("btnReceive, ");
        if (btnDeduct == null)                  sb.append("btnDeduct, ");
        if (segInv == null)                     sb.append("segInv, ");
        if (sb.length() > 0) {
            String miss = sb.substring(0, sb.length() - 2);
            System.out.println("[PharmacyController] Missing fx:id(s): " + miss);
            if (alertLabel != null) {
                alertLabel.setText("Missing fx:id(s): " + miss);
            }
            return true;
        }
        return false;
    }

    /* ====== Section switching (Dashboard / Prescriptions / Inventory) ====== */
    private void showDashboardPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, true);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, false);
        markNavActive(DashboardButton);
        System.out.println("[PharmacyController] showDashboardPane -> ensure listeners running");
        startPharmacyDbNotifications();
        loadDashboardAsync(false);
    }
    private void loadDashboardTable() {
        loadDashboardAsync(true);
    }
    private void loadDashboardAsync(boolean force) {
        LocalDate day = getSelectedDateOrToday();
        if (!force && lastLoadedDate != null && lastLoadedDate.equals(day) && !dashboardRows.isEmpty()) {
            return; // already loaded for this day
        }
        if (dashboardLoading) return;
        dashboardLoading = true;
        lastLoadedDate = day;

        if (alertLabel != null) alertLabel.setText("Loading " + day + " ...");
        System.out.println("[PharmacyController] loadDashboardAsync start day=" + day);
        if (PresciptionsTable != null) PresciptionsTable.setPlaceholder(new Label("Loading..."));

        // Busy UI hint
        if (rootPane != null) rootPane.setCursor(Cursor.WAIT);
        if (DashboardButton != null && !DashboardButton.disableProperty().isBound()) {
            DashboardButton.setDisable(true);
        }

        Task<DashboardData> task = new Task<>() {
            @Override protected DashboardData call() throws Exception {
                DashboardData data = new DashboardData();
                try (Connection c = Database.get()) {
                    PrescriptionDAO dao = new PrescriptionDAO();
                    data.total     = dao.countTotalOnDate(c, day);
                    data.waiting   = dao.countPendingOnDate(c, day);
                    data.completed = dao.countCompletedOnDate(c, day);
                    data.rows      = dao.listDashboardRowsByDate(c, day);
                }
                return data;
            }
        };

        task.setOnSucceeded(ev -> {
            DashboardData d = task.getValue();
            System.out.println("[PharmacyController] loadDashboardAsync SUCCESS day=" + lastLoadedDate
                    + " rows=" + (d == null || d.rows == null ? 0 : d.rows.size()));

            // Update counts
            if (d != null) {
                if (PrescriptionsTodayTotal != null)   PrescriptionsTodayTotal.setText(String.valueOf(d.total));
                if (PrescriptionsWatingNum != null)    PrescriptionsWatingNum.setText(String.valueOf(d.waiting));
                if (PrescriptionsCompleteNum != null)  PrescriptionsCompleteNum.setText(String.valueOf(d.completed));
                // Update table
                dashboardRows.setAll(d.rows == null ? java.util.Collections.emptyList() : d.rows);
            }
            if (PresciptionsTable != null) PresciptionsTable.setPlaceholder(new Label(dashboardRows.isEmpty() ? "No data" : ""));

            // Finalize UI state
            dashboardLoading = false;
            if (rootPane != null) rootPane.setCursor(Cursor.DEFAULT);
            if (DashboardButton != null && !DashboardButton.disableProperty().isBound()) {
                DashboardButton.setDisable(false);
            }
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            System.err.println("[PharmacyController] loadDashboardAsync FAILED day=" + lastLoadedDate + " ex=" + ex);
            if (alertLabel != null) alertLabel.setText("Failed to load dashboard: " + (ex == null ? "Unknown error" : ex.getMessage()));
            if (PresciptionsTable != null) PresciptionsTable.setPlaceholder(new Label("Failed to load"));

            // Finalize UI state
            dashboardLoading = false;
            if (rootPane != null) rootPane.setCursor(Cursor.DEFAULT);
            if (DashboardButton != null && !DashboardButton.disableProperty().isBound()) {
                DashboardButton.setDisable(false);
            }
        });

        Thread th = new Thread(task, "pharm-dashboard-loader");
        th.setDaemon(true);
        th.start();
    }


    // ---- Auto-refresh (polling-based) for the pharmacy dashboard ----
    private final java.util.concurrent.ScheduledExecutorService pharmAutoRefresher =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pharm-auto-refresh");
                t.setDaemon(true);
                return t;
            });
    private volatile java.time.Instant lastDashFingerprint = null;
    private volatile boolean autoRefreshStarted = false;


    // لازم تتعدل عندما ننتهي من المخزن
    private void reloadInventoryTable() {
        // استعمل اللودر الموجود في المخزون
    }

    private boolean isDashboardVisible() {
        return pharmacyDashboardAnchorPane != null && pharmacyDashboardAnchorPane.isVisible();
    }

    private boolean isPrescriptionVisible() {
        return PrescriptionAnchorPane != null && PrescriptionAnchorPane.isVisible();
    }

    private boolean isInventoryVisible() {
        return InventoryAnchorPane != null && InventoryAnchorPane.isVisible();
    }

    /**
     * Returns a timestamp fingerprint (max of created/updated_at) for prescriptions of the selected date.
     * Any change will advance the fingerprint and trigger a UI reload.
     */
    private java.time.Instant fetchDashboardFingerprint(java.time.LocalDate day) {
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT MAX(COALESCE(p.dispensed_at, p.approved_at, p.decision_at, p.created_at))\n" +
                             "FROM prescriptions p\n" +
                             "WHERE (p.created_at::date = ? OR p.decision_at::date = ? OR p.approved_at::date = ? OR p.dispensed_at::date = ?)")) {
            ps.setDate(1, java.sql.Date.valueOf(day));
            ps.setDate(2, java.sql.Date.valueOf(day));
            ps.setDate(3, java.sql.Date.valueOf(day));
            ps.setDate(4, java.sql.Date.valueOf(day));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
            }
        } catch (Exception e) {
            System.err.println("[PharmacyController] fetchDashboardFingerprint: " + e);
        }
        return null;
    }
    /** Fingerprint لعناصر الوصفة المفتوحة: أكبر (approved_at/dispensed_at/decision_at/created_at/items.count/qty_dispensed) */
    private java.time.Instant fetchCurrentPrescriptionFingerprint() {
        if (selectedRow == null || selectedRow.prescriptionId <= 0) return null;
        long pid = selectedRow.prescriptionId;
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     // أي تعديل على حالة الوصفة أو على عناصرها سيغيّر هذه البصمة
                     "SELECT MAX(v) FROM ( " +
                             "  SELECT COALESCE(p.dispensed_at, p.approved_at, p.decision_at, p.created_at) AS v " +
                             "  FROM prescriptions p WHERE p.id = ? " +
                             "  UNION ALL " +
                             "  SELECT NOW() - make_interval(secs => (SELECT COUNT(*) FROM prescription_items i WHERE i.prescription_id = ?)) " +
                             "  UNION ALL " +
                             "  SELECT NOW() - make_interval(secs => (SELECT COALESCE(SUM(i.qty_dispensed),0) FROM prescription_items i WHERE i.prescription_id = ?)) " +
                             ") t"
             )) {
            ps.setLong(1, pid);
            ps.setLong(2, pid);
            ps.setLong(3, pid);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
            }
        } catch (Exception e) {
            System.err.println("[PharmacyController] fetchCurrentPrescriptionFingerprint: " + e);
        }
        return null;
    }

    /** Fingerprint لواجهة الجرد: أي تغيير في inventory_transactions أو تحديث available_quantity للأدوية */
    private java.time.Instant fetchInventoryFingerprint() {
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT GREATEST( " +
                             "  COALESCE((SELECT MAX(created_at) FROM inventory_transactions), 'epoch'), " +
                             "  COALESCE((SELECT MAX(updated_at) FROM medicines), 'epoch') " +
                             ")"
             )) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
            }
        } catch (Exception e) {
            System.err.println("[PharmacyController] fetchInventoryFingerprint: " + e);
        }
        return null;
    }
    /** يعيد تحميل عناصر الوصفة المفتوحة (إن وُجدت). */
    private void reloadCurrentPrescriptionItems() {
        if (selectedRow != null && selectedRow.prescriptionId > 0) {
            loadPrescriptionItems(selectedRow.prescriptionId);
            // بعد ما نحمّل العناصر، حدّث العدّادات
            refreshPrescriptionItemCounts(selectedRow.prescriptionId);
        }
    }


    /** حوار صرف ذكي: عبوات/أشرطة أو وحدات (فراطة) مع حساب تلقائي وإشارة إن كانت كاملة أو فراطة */
    private DispenseDecision showPackAwareDispenseDialog(PrescItemRow r) {
        int prescribed = Math.max(0, r.getQuantity());
        int inStock    = Math.max(0, r.getStockAvailable());
        int requested  = prescribed;

        PackagingSupport.PackagingInfo p =
                (r.getMedicineId() > 0) ? fetchPackaging(r.getMedicineId()) : null;

        // suggested to preselect
        PackagingSupport.PackSuggestion sugg = PackagingSupport.suggestPackFor(requested, p);

        Dialog<DispenseDecision> dlg = new Dialog<>();
        dlg.setTitle("Dispense quantity");
        dlg.setHeaderText("How do you want to dispense? (packs or units)");

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, okType);

        ToggleGroup mode = new ToggleGroup();
        RadioButton byPack  = new RadioButton("By pack");
        RadioButton byUnits = new RadioButton("By units (فراطة)");
        byPack.setToggleGroup(mode); byUnits.setToggleGroup(mode); byPack.setSelected(true);

        // limits
        Label limits = new Label("Prescribed: " + prescribed + "  |  In stock: " + inStock);

        // pack choice
        ChoiceBox<String> unitChoice = new ChoiceBox<>();
        java.util.List<String> opts = new java.util.ArrayList<>();
        if (p != null) {
            if (p.mlPerBottle != null)      opts.add("BOTTLE");
            else if (p.gramsPerTube != null) opts.add("TUBE");
            else {
                if (p.blistersPerBox != null && p.tabletsPerBlister != null) opts.add("BOX");
                if (p.tabletsPerBlister != null)                              opts.add("BLISTER");
            }
        }
        opts.add("UNIT");
        unitChoice.setItems(FXCollections.observableArrayList(opts));
        String defaultUnit = (sugg != null && opts.contains(sugg.unit)) ? sugg.unit : opts.get(0);
        unitChoice.getSelectionModel().select(defaultUnit);

        Spinner<Integer> packCount = new Spinner<>(1, 10_000, Math.max(1, (sugg != null ? sugg.count : 1)));
        packCount.setEditable(true);

        // breakdown
        Label breakdown = new Label();
        breakdown.setWrapText(true);

        // units (free) +/-
        TextField unitsField = new TextField(String.valueOf(requested));
        Button btnMinus = new Button("–");
        Button btnPlus  = new Button("+");
        unitsField.setDisable(true); btnMinus.setDisable(true); btnPlus.setDisable(true);
        HBox unitsRow = new HBox(6, new Label("Units:"), btnMinus, unitsField, btnPlus);
        unitsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // result + warnings
        Label calc = new Label(); calc.setWrapText(true);
        Label warn = new Label(); warn.getStyleClass().add("text-danger");

        VBox box = new VBox(8,
                limits,
                new HBox(10, byPack, byUnits),
                new HBox(10, new Label("Pack:"), unitChoice, new Label("Count:"), packCount),
                breakdown,
                unitsRow,
                calc,
                warn
        );
        dlg.getDialogPane().setContent(box);

        // helper: units per selected pack
        java.util.function.Function<String, Integer> unitsPerPack = u -> {
            if (p == null) return 1;
            if ("BOX".equals(u) && p.tabletsPerBlister != null && p.blistersPerBox != null)
                return p.tabletsPerBlister * p.blistersPerBox;
            if ("BLISTER".equals(u) && p.tabletsPerBlister != null)
                return p.tabletsPerBlister;
            if ("BOTTLE".equals(u) || "TUBE".equals(u))
                return 1;
            return 1;
        };

        final Button okBtn = (Button) dlg.getDialogPane().lookupButton(okType);

        Runnable recompute = () -> {
            boolean free = byUnits.isSelected();
            unitsField.setDisable(!free);
            btnMinus.setDisable(!free);
            btnPlus.setDisable(!free);
            unitChoice.setDisable(free);
            packCount.setDisable(free);

            String u = unitChoice.getSelectionModel().getSelectedItem();
            String btxt = "";
            if (p != null) {
                if ("BOX".equals(u) && p.tabletsPerBlister != null && p.blistersPerBox != null) {
                    btxt = "1 BOX = " + p.blistersPerBox + " BLISTER × " + p.tabletsPerBlister + " UNIT = "
                            + (p.blistersPerBox * p.tabletsPerBlister) + " units";
                } else if ("BLISTER".equals(u) && p.tabletsPerBlister != null) {
                    btxt = "1 BLISTER = " + p.tabletsPerBlister + " UNIT";
                } else if ("BOTTLE".equals(u) && p.mlPerBottle != null) {
                    btxt = "1 BOTTLE = " + p.mlPerBottle + " ml";
                } else if ("TUBE".equals(u) && p.gramsPerTube != null) {
                    btxt = "1 TUBE = " + p.gramsPerTube + " g";
                } else if ("UNIT".equals(u)) {
                    btxt = "UNIT = single piece";
                }
            }
            breakdown.setText(btxt);

            int units;
            if (free) {
                try { units = Math.max(0, Integer.parseInt(unitsField.getText().trim())); }
                catch (Exception e) { units = 0; }
            } else {
                int cnt = packCount.getValue();
                int per = unitsPerPack.apply(u);
                units = cnt * Math.max(1, per);
            }

            String note;
            if (!free && p != null && p.tabletsPerBlister != null) {
                int perBlister = Math.max(1, p.tabletsPerBlister);
                note = (units % perBlister == 0) ? "Full pack" : "فراطة (not a full blister)";
            } else {
                note = "فراطة";
            }
            calc.setText("Will dispense " + units + " units — " + note);

            String w = "";
            boolean ok = true;
            if (units > prescribed) { w = "Exceeds prescribed (" + prescribed + ")."; ok = false; }
            if (units > inStock)     { w = (w.isEmpty()? "" : w + " ") + "Exceeds stock (" + inStock + ")."; ok = false; }
            warn.setText(w);
            okBtn.setDisable(!ok);

            final int unitsFinal = units;
            dlg.setResultConverter(btn -> (btn == okType)
                    ? new DispenseDecision(
                    free ? null : u,
                    free ? 0    : packCount.getValue(),
                    unitsFinal,
                    free
            )
                    : null);
        };

        unitChoice.getSelectionModel().selectedItemProperty().addListener((obs,a,b) -> recompute.run());
        packCount.valueProperty().addListener((obs,a,b) -> recompute.run());
        unitsField.textProperty().addListener((obs,a,b) -> recompute.run());
        byPack.setOnAction(e -> recompute.run());
        byUnits.setOnAction(e -> recompute.run());

        btnMinus.setOnAction(e -> {
            try { int v = Integer.parseInt(unitsField.getText().trim()); if (v > 0) unitsField.setText(String.valueOf(v - 1)); }
            catch (Exception ignored) {}
        });
        btnPlus.setOnAction(e -> {
            try { int v = Integer.parseInt(unitsField.getText().trim()); unitsField.setText(String.valueOf(v + 1)); }
            catch (Exception ignored) {}
        });

        recompute.run();
        return dlg.showAndWait().orElse(null);
    }

    /** بديل بسيط للاستخدام داخل زر Accept */
    private int promptDispenseQuantity(PrescItemRow r) {
        DispenseDecision d = showPackAwareDispenseDialog(r);
        return (d == null) ? -1 : d.unitsTotal;
    }

    /** Update Total/Approved/Rejected/Pending counters for a given prescription. */
    private void refreshPrescriptionItemCounts(long prescId) {
        int total = 0, approved = 0, rejected = 0, pending = 0;

        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     """
                     SELECT
                         COUNT(*)                                     AS total,
                         COUNT(*) FILTER (WHERE status = 'APPROVED')  AS approved,
                         COUNT(*) FILTER (WHERE status = 'CANCELLED') AS rejected,
                         COUNT(*) FILTER (WHERE status = 'PENDING')   AS pending
                     FROM prescription_items
                     WHERE prescription_id = ?
                     """
             )) {
            ps.setLong(1, prescId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total    = rs.getInt("total");
                    approved = rs.getInt("approved");
                    rejected = rs.getInt("rejected");
                    pending  = rs.getInt("pending");
                }
            }
        } catch (Exception e) {
            System.err.println("[PharmacyController] refreshPrescriptionItemCounts error: " + e);
        }

        if (TotalItemsNumber != null) TotalItemsNumber.setText(String.valueOf(total));
        if (ApprovedNumber   != null) ApprovedNumber.setText(String.valueOf(approved));
        if (RejectedNumber   != null) RejectedNumber.setText(String.valueOf(rejected));
        if (PendingNumber    != null) PendingNumber.setText(String.valueOf(pending));
    }

    /** Query packaging for a given medicine id. Safe to call per-row; tiny select. */
    private PackagingInfo fetchPackaging(long medId) {
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT base_unit::text, tablets_per_blister, blisters_per_box, ml_per_bottle, grams_per_tube, split_allowed " +
                             "FROM medicines WHERE id = ?")) {
            ps.setLong(1, medId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PackagingInfo(
                            rs.getString(1),
                            (Integer)rs.getObject(2),
                            (Integer)rs.getObject(3),
                            (Integer)rs.getObject(4),
                            (Integer)rs.getObject(5),
                            (Boolean)rs.getObject(6)
                    );
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    // --- ضع هاتين الدالتين داخل PharmacyController ---

    private static final class DispenseDecision {
        final String approvedUnit;   // UNIT/BLISTER/BOX/BOTTLE/TUBE أو null لو "فراطة"
        final int approvedCount;     // عدد العبوات لو بالعبوات
        final int unitsTotal;        // إجمالي الوحدات الفعلية للصرف
        final boolean isFreeUnits;   // فراطة؟
        DispenseDecision(String unit, int count, int unitsTotal, boolean free) {
            this.approvedUnit = unit; this.approvedCount = count; this.unitsTotal = unitsTotal; this.isFreeUnits = free;
        }
    }

    private void setupDashboardTableColumns() {
        // --- Suggested pack/quantity column (renders text with tooltip, stays read-only) ---
        if (colSuggestionQty != null) {
            colSuggestionQty.setCellFactory(col -> new TableCell<PrescItemRow, Number>() {
                private final Tooltip tip = new Tooltip();
                @Override protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setText(null); setTooltip(null); return; }
                    PrescItemRow r = getTableView().getItems().get(getIndex());
                    int req = Math.max(0, r.getQuantity());
                    long medId = r.getMedicineId();
                    PackagingInfo p = (medId > 0) ? fetchPackaging(medId) : null;
                    PackSuggestion s = PackagingSupport.suggestPackFor(req, p);
                    String txt = PackagingSupport.formatSuggestionText(s);
                    setText(txt);
                    tip.setText(txt);
                    setTooltip(tip);
                }
            });
            colSuggestionQty.setStyle("-fx-alignment: CENTER-LEFT;");
            colSuggestionQty.setPrefWidth(180);
        }
        if (PresciptionsTable == null) return;
        PresciptionsTable.setColumnResizePolicy(javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY);
        // Serial #
        // -------- Serial # --------
        if (colSerialPhDashboard != null) {
            colSerialPhDashboard.setCellFactory((TableColumn<DashboardRow, Number> col) ->
                    new TableCell<DashboardRow, Number>() {
                        @Override
                        protected void updateItem(Number item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setText(null);
                            } else {
                                // الترقيم بحسب الترتيب الحالي للجدول
                                setText(Integer.toString(getIndex() + 1));
                            }
                        }
                    }
            );
            // (اختياري) عرض صغير وثابت
            colSerialPhDashboard.setStyle("-fx-alignment: CENTER;");
            colSerialPhDashboard.setMinWidth(60);
            colSerialPhDashboard.setPrefWidth(70);
            colSerialPhDashboard.setMaxWidth(100);
        }
        // Names
        if (colPatientName != null) colPatientName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().patientName));
        if (colDoctorName  != null) colDoctorName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().doctorName));
        // Date/Time formatted: appt date (if exists) + created time
        if (colDate_Time != null) {
            var dateFmt  = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
            var timeFmt  = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            colDate_Time.setCellValueFactory(cd -> {
                var appt    = cd.getValue().appointmentDateTime;
                var created = cd.getValue().createdAt;
                String left  = (appt    != null) ? appt.atZoneSameInstant(APP_TZ).toLocalDateTime().format(dateFmt) : "";
                String right = (created != null) ? created.atZoneSameInstant(APP_TZ).toLocalDateTime().format(timeFmt) : "";
                String txt = left;
                if (!left.isEmpty() && !right.isEmpty()) txt += "  |  " + right;
                else if (left.isEmpty()) txt = right; // fallback
                return new javafx.beans.property.SimpleStringProperty(txt);
            });
        }
        // Status beautified
        if (colprescriptionStutus != null) {
            colprescriptionStutus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                    cd.getValue().status == null ? "" : switch (cd.getValue().status) {
                        case PENDING -> "PENDING";
                        case APPROVED -> "Approved";
                        case REJECTED -> "Rejected";
                        case DISPENSED -> "Dispensed";
                    }
            ));
        }
        // Action column (View)
        if (colActionPhDashbord != null) {
            colActionPhDashbord.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final Button btn = new Button("View");
                {
//                    btn.getStyleClass().add("btn-primary");
                    btn.getStyleClass().addAll("btn", "btn-primary", "btn--deep", "btn--compact");
                    btn.setOnAction(e -> {
                        DashboardRow row = getTableView().getItems().get(getIndex());
                        showPrescriptionDetails(row);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }
    }

    private void showPrescriptionDetails(DashboardRow row) {
        if (row == null) return;
        this.selectedRow = row;
        if (Prescription_number != null) Prescription_number.setText("");

        // Switch to prescription pane
        showPrescriptionsPane();

        // Enable/disable Finish button based on prescription status
        if (Finish_Prescription != null) {
            boolean ro = row.status != null && "DISPENSED".equalsIgnoreCase(row.status.name());
            Finish_Prescription.setDisable(ro);
        }

        // Patient name + NID (read-only)
        if (PatientNameTF != null) {
            String nid = row.patientNid == null ? "" : (" (" + row.patientNid + ")");
            PatientNameTF.setText(row.patientName + nid);
            PatientNameTF.setEditable(false);
        }

        // Doctor name
        if (DoctorNameLabel != null) {
            DoctorNameLabel.setText(row.doctorName);
        }

        // Prescription number (ID)
        if (Prescription_number != null) {
            if (row.prescriptionId > 0) {
                Prescription_number.setText(String.valueOf(row.prescriptionId));
            } else if (row.appointmentId != null) {
                // Optional fallback: try to resolve prescription id by appointment id if needed
                Prescription_number.setText("–");
            } else {
                Prescription_number.setText("–");
            }
        }

        // Pharmacist full name + Welcome first name
        resolveLoggedInUserLabels();

        // Appointment date/time: day + date + time, include ID if available, with DB fallback
        if (AppointmentDate != null) {
            var dateFmt = java.time.format.DateTimeFormatter.ofPattern("EEE MM/dd/yyyy");
            var timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            var when = (row.appointmentDateTime != null)
                    ? row.appointmentDateTime
                    : fetchAppointmentDateById(row.appointmentId);

            if (when != null) {
                var zdt = when.atZoneSameInstant(APP_TZ).toLocalDateTime();
                String txt = zdt.format(dateFmt) + "  |  " + zdt.format(timeFmt);
                if (row.appointmentId != null) {
                    txt += "  (ID: " + row.appointmentId + ")";
                }
                AppointmentDate.setText(txt);
            } else {
                AppointmentDate.setText("—");
            }
        }

        // Diagnosis
        if (DiagnosisView != null) {
            DiagnosisView.setText(row.diagnosisNote == null ? "" : row.diagnosisNote);
        }

        // Load items
        loadPrescriptionItems(row.prescriptionId);
    }

    private void loadPrescriptionItems(long prescId) {
        itemRows.clear();

        // 1) Load items for the prescription (current behavior)
        java.util.List<PrescriptionItem> items = java.util.Collections.emptyList();
        try (Connection c = Database.get()) {
            PrescriptionItemDAO itemDao = new PrescriptionItemDAO();
            items = itemDao.listByPrescription(c, prescId);
        } catch (Exception ex) {
            System.err.println("[PharmacyController] loadPrescriptionItems (fetch items) error: " + ex);
        }

        if (items == null || items.isEmpty()) {
            if (TablePrescriptionItems != null) TablePrescriptionItems.setItems(itemRows);
            refreshPrescriptionItemCounts(prescId);

            return;
        }

        // 2) Pre-collect medicine ids and names to lookup stock in one go
        java.util.Set<Long> medIds = new java.util.LinkedHashSet<>();
        java.util.Set<String> medNames = new java.util.LinkedHashSet<>();
        for (PrescriptionItem it : items) {
            if (it.getMedicineId() != null) medIds.add(it.getMedicineId());
            else if (it.getMedicineName() != null && !it.getMedicineName().isBlank()) medNames.add(it.getMedicineName().trim());
        }

        // 3) Build maps id->available and lower(name)->available
        java.util.Map<Long, Integer> stockById = new java.util.HashMap<>();
        java.util.Map<String, Integer> stockByLowerName = new java.util.HashMap<>();

        try (Connection c = Database.get()) {
            // 3.a) Lookup by ids
            if (!medIds.isEmpty()) {
                String in = medIds.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));
                String sql = "SELECT id, available_quantity FROM medicines WHERE id IN (" + in + ")";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int i = 1; for (Long id : medIds) ps.setLong(i++, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            stockById.put(rs.getLong(1), rs.getInt(2));
                        }
                    }
                }
            }
            // 3.b) Lookup by names (case-insensitive)
            if (!medNames.isEmpty()) {
                String in = medNames.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));
                String sql = "SELECT LOWER(name), available_quantity FROM medicines WHERE LOWER(name) IN (" + in + ")";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int i = 1; for (String nm : medNames) ps.setString(i++, nm.toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            stockByLowerName.put(rs.getString(1), rs.getInt(2));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] loadPrescriptionItems (fetch stock) error: " + ex);
        }

        // 4) Build UI rows and include stockAvailable
        for (PrescriptionItem it : items) {
            PrescItemRow r = new PrescItemRow();
            r.setId(it.getId());
            if (it.getMedicineId() != null) r.setMedicineId(it.getMedicineId());
            r.setMedicineName(it.getMedicineName());
            r.setStrength(it.getStrength());
            r.setForm(it.getForm());
            if (it.getDose() != null) r.setDose(it.getDose());
            if (it.getDurationDays() != null) r.setDurationDays(it.getDurationDays());
            if (it.getFreqPerDay() != null) r.setFreqPerDay(it.getFreqPerDay());
            r.setDosage(it.getDosage());
            r.setDosageText(it.getDosageText());
            r.setQuantity(it.getQuantity());
            r.setQtyDispensed(it.getQtyDispensed());
            r.setStatus(it.getStatus().name());

            // Resolve stock: prefer by id, otherwise by lower(name)
            Integer stock = null;
            if (it.getMedicineId() != null) stock = stockById.get(it.getMedicineId());
            if (stock == null && it.getMedicineName() != null) stock = stockByLowerName.get(it.getMedicineName().toLowerCase());
            r.setStockAvailable(stock == null ? 0 : stock);

            itemRows.add(r);
        }

        if (TablePrescriptionItems != null) TablePrescriptionItems.setItems(itemRows);
        refreshPrescriptionItemCounts(prescId);    }

    private void setupItemsTableColumns() {
        if (TablePrescriptionItems == null) return;
        if (colIdx != null) {
            colIdx.setCellFactory(col -> new TableCell<PrescItemRow, Number>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : Integer.toString(getIndex() + 1));
                }
            });
        }        if (colMedicineName != null) colMedicineName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        if (colStrength != null) colStrength.setCellValueFactory(new PropertyValueFactory<>("strength"));
        if (colForm != null) colForm.setCellValueFactory(new PropertyValueFactory<>("form"));
        if (colDose != null) colDose.setCellValueFactory(new PropertyValueFactory<>("dose"));
        if (colDosage != null) {
            colDosage.setCellValueFactory(cd -> {
                String txt = cd.getValue().getDosageText();
                if (txt == null || txt.isBlank()) txt = cd.getValue().getDosage();
                return new javafx.beans.property.SimpleStringProperty(txt == null ? "" : txt);
            });
        }
        if (colQuantity != null) colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        if (colStock != null) colStock.setCellValueFactory(new PropertyValueFactory<>("stockAvailable"));
        if (colItemStatus != null) colItemStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colPresesItemAction != null) {
            colPresesItemAction.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final Button approve = new Button("Accept");
                private final Button reject  = new Button("Reject");
                {
                    approve.getStyleClass().add("btn-success");
                    reject.getStyleClass().add("btn-danger");
                    // Inline fallback styles if CSS classes are not present
                    approve.setStyle("-fx-background-color: #298e40; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
                    reject.setStyle("-fx-background-color: #b3081a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
                    approve.setOnAction(e -> onApproveItem(getIndex()));
                    reject.setOnAction(e -> onRejectItem(getIndex()));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    PrescItemRow row = getTableView().getItems().get(getIndex());
                    boolean ro = isCurrentPrescriptionReadOnly();
                    boolean pending = row != null && row.getStatus() != null && row.getStatus().equalsIgnoreCase("PENDING");
                    approve.setDisable(ro || !pending);
                    reject.setDisable(ro || !pending);
                    var box = new javafx.scene.layout.HBox(6, approve, reject);
                    setGraphic(box);
                }
            });
        }


        if (colIdx != null) colIdx.setPrefWidth(50);
        if (colMedicineName != null) colMedicineName.setPrefWidth(180);
        if (colStrength != null) colStrength.setPrefWidth(120);
        if (colForm != null) colForm.setPrefWidth(120);
        if (colDose != null) colDose.setPrefWidth(80);
        if (colDosage != null) colDosage.setPrefWidth(360);
        if (colQuantity != null) colQuantity.setPrefWidth(100);
        if (colStock != null) colStock.setPrefWidth(120);
        if (colItemStatus != null) colItemStatus.setPrefWidth(120);
        if (colPresesItemAction != null) colPresesItemAction.setPrefWidth(160);
    }

    private void onApproveItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= itemRows.size() || selectedRow == null) return;
        PrescItemRow r = itemRows.get(rowIndex);

        // 1) Ask pharmacist how many to dispense (allow partial)
        int prescribed = Math.max(0, r.getQuantity());
        int inStock    = Math.max(0, r.getStockAvailable()); // 0 if null

        int units = promptDispenseQuantity(r);
        if (units < 0) return; // Cancel

        int dispense = units;   // اسم موحّد للمتغيّر

        // 2) Validate bounds (مرة واحدة فقط)
        if (dispense < 0) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Dispense must be >= 0").showAndWait();
            return;
        }
        if (dispense > prescribed) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Cannot dispense more than prescribed (" + prescribed + ")").showAndWait();
            return;
        }
        if (dispense > inStock) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Not enough stock (available: " + inStock + ")").showAndWait();
            return;
        }

        // 3) Persist, **including inventory deduction now** (not only on finish)
        try (Connection c = Database.get()) {
            try {
                c.setAutoCommit(false);

                PrescriptionItemDAO dao = new PrescriptionItemDAO();
                ItemStatus newStatus = (dispense > 0) ? ItemStatus.APPROVED : ItemStatus.CANCELLED;
                dao.updateDispensed(c, r.getId(), dispense, newStatus, null);

                // Deduct inventory immediately so Stock reflects the change
                Long medId = r.getMedicineId();
                if (medId == null) {
                    // Try to resolve medicine id by name (case-insensitive)
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT id FROM medicines WHERE LOWER(name)=LOWER(?) LIMIT 1")) {
                        ps.setString(1, r.getMedicineName());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) medId = rs.getLong(1);
                        }
                    }
                }
                if (medId != null && dispense > 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO inventory_transactions (medicine_id, qty_change, reason, ref_type, ref_id)\n" +
                                    "VALUES (?, ?, ?, ?, ?)")) {
                        ps.setLong(1, medId);
                        ps.setInt(2, -dispense);                 // negative = outflow
                        ps.setString(3, "DISPENSE");              // reason
                        ps.setString(4, "prescription_item");     // ref_type
                        ps.setLong(5, r.getId());                 // ref_id = item id
                        ps.executeUpdate();
                    }
                }

                c.commit();

                // Update UI model
                r.setQtyDispensed(dispense);
                r.setStatus(newStatus.name());
                if (dispense > 0) {
                    int newStock = Math.max(0, inStock - dispense);
                    r.setStockAvailable(newStock);
                }
                TablePrescriptionItems.refresh();

                // Refresh side counts (optional)
                refreshPharmacyDashboardCounts();
            } catch (Exception txErr) {
                try { c.rollback(); } catch (Exception ignored) {}
                throw txErr;
            } finally {
                try { c.setAutoCommit(true); } catch (Exception ignored) {}
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] approve item error: " + ex);
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to update item: " + ex.getMessage()).showAndWait();
        }
    }

    private void onRejectItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= itemRows.size() || selectedRow == null) return;
        PrescItemRow r = itemRows.get(rowIndex);
        try (Connection c = Database.get()) {
            PrescriptionItemDAO dao = new PrescriptionItemDAO();
            dao.updateDispensed(c, r.getId(), 0, ItemStatus.CANCELLED, null);
            r.setStatus("CANCELLED");
            r.setQtyDispensed(0);
            TablePrescriptionItems.refresh();
        } catch (Exception ex) {
            System.err.println("[PharmacyController] reject item error: " + ex);
        }
    }

    // Handler: Mark prescription as DISPENSED (completed) and prevent further edits
    private void onFinishPrescription() {
        if (selectedRow == null) return;
        long prescId = selectedRow.prescriptionId;
        if (prescId <= 0) return;

        // Strictly require pharmacist mapping (no auto-create)
        Long pharmacistId = requireCurrentPharmacistId();
        if (pharmacistId == null) return;

        // Mark prescription as DISPENSED (completed)
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE prescriptions\n" +
                     "SET status = 'DISPENSED',\n" +
                     "    pharmacist_id = ?,\n" +
                     "    decision_at   = COALESCE(decision_at, NOW()),\n" +
                     "    approved_at   = COALESCE(approved_at, decision_at),\n" +
                     "    approved_by   = COALESCE(approved_by, ?),\n" +
                     "    dispensed_at  = NOW(),\n" +
                     "    dispensed_by  = ?\n" +
                     "WHERE id = ? AND status <> 'DISPENSED'")) {
            ps.setLong(1, pharmacistId);  // pharmacist_id
            ps.setLong(2, pharmacistId);  // approved_by (fallback if null)
            ps.setLong(3, pharmacistId);  // dispensed_by
            ps.setLong(4, prescId);       // WHERE id = ?
            int changed = ps.executeUpdate();

            if (changed > 0) {
                // Reflect in memory row and UI
                selectedRow.status = com.example.healthflow.model.PrescriptionStatus.DISPENSED;
                if (Finish_Prescription != null) Finish_Prescription.setDisable(true);
                // Force items column actions to re-evaluate disabled state
                if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
                refreshPharmacyDashboardCounts();
                loadDashboardTable();
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Prescription marked as completed.").showAndWait();
            } else {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Already completed.").showAndWait();
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] finish prescription error: " + ex);
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to complete prescription: " + ex.getMessage()).showAndWait();
        }
    }

    private void showPrescriptionsPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, true);
        setVisibleManaged(InventoryAnchorPane, false);
        markNavActive(PrescriptionsButton);
        // Show the details area only when a prescription is selected later
        if (PrescriptionMedicationAnchorPane != null){
            setVisibleManaged(PrescriptionMedicationAnchorPane, true);
        }
        startPharmacyDbNotifications();
    }

    private void showInventoryPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, true);
        markNavActive(InventoryButton);
        startPharmacyDbNotifications();
        wireInventoryToggles();
    }

    /* ====== Inventory Receive vs Deduct (like segmented behavior) ====== */
    private ToggleGroup invToggleGroup;
    private ToggleButton lastSelectedInvBtn;


    private void showInventoryReceiveMode() {
        ReceivePane.setVisible(true);
        deductPane.setVisible(false);
    }

    private void showInventoryDeductMode() {
        ReceivePane.setVisible(false);
        deductPane.setVisible(true);
    }

private void wireInventoryToggles() {
    invToggleGroup = new ToggleGroup();
    if (btnReceive != null) btnReceive.setToggleGroup(invToggleGroup);
    if (btnDeduct != null)  btnDeduct.setToggleGroup(invToggleGroup);

    // امنع إلغاء التحديد بالضغط على الزر المحدد
    if (btnReceive != null) {
        btnReceive.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (btnReceive.isSelected()) e.consume();
        });
    }
    if (btnDeduct != null) {
        btnDeduct.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (btnDeduct.isSelected()) e.consume();
        });
    }

    // تأكد أن الـ SegmentedButton يحتوي أزرارنا
    if (segInv != null) {
        if (btnReceive != null && !segInv.getButtons().contains(btnReceive)) segInv.getButtons().add(btnReceive);
        if (btnDeduct  != null && !segInv.getButtons().contains(btnDeduct))  segInv.getButtons().add(btnDeduct);
    }

    // الوضع الافتراضي = Receive
    if (btnReceive != null) btnReceive.setSelected(true);
    if (btnDeduct  != null) btnDeduct.setSelected(false);
    lastSelectedInvBtn = (btnReceive != null) ? btnReceive : btnDeduct;

    // أظهر البانل المناسب بدايةً
    if (btnReceive != null && btnReceive.isSelected()) {
        showInventoryReceiveMode();
    } else {
        showInventoryDeductMode();
    }

    // بدّل البانلز عند تغيير الاختيار + لا تسمح بـ null
    if (invToggleGroup != null) {
        invToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                if (lastSelectedInvBtn != null) invToggleGroup.selectToggle(lastSelectedInvBtn);
                return;
            }
            if (newT == btnReceive) {
                showInventoryReceiveMode();
            } else if (newT == btnDeduct) {
                showInventoryDeductMode();
            }
            if (newT instanceof ToggleButton tb) {
                lastSelectedInvBtn = tb;
            }
        });
    }
}

    public PharmacyController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader
    public PharmacyController() {
        this(new ConnectivityMonitor());
    }

    private void ensureConnectivityBannerOnce() {
        if (rootPane == null) return;
        // لا تكرار: افحص إن كان مضاف مسبقًا
        for (javafx.scene.Node n : rootPane.getChildren()) {
            if (n instanceof ConnectivityBanner) {
                return; // موجود بالفعل
            }
        }
        ConnectivityBanner banner = new ConnectivityBanner(monitor);
        rootPane.getChildren().add(0, banner);
    }

    @FXML
    private void initialize() {
        resolveLoggedInUserLabels();

        if (rootPane != null) {
            ensureConnectivityBannerOnce();

            // attach shared CSS defensively
            try {
                var cssUrl = getClass().getResource("/Design/ReceptionDesign.css");
                if (cssUrl != null && !rootPane.getStylesheets().contains(cssUrl.toExternalForm())) {
                    rootPane.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Throwable ignored) {}
        }
        // Defensive: hide all sections first; we'll explicitly show one below
        btnReceive.setOnAction(e -> showInventoryReceiveMode());
        btnDeduct.setOnAction(e -> showInventoryDeductMode());

        // Quick sanity check: if IDs are not wired, navigation won't work
        boolean missing = warnIfMissing();

        // Start connectivity monitor
        monitor.start();


        // Start header clock & date (12h, Asia/Gaza)
        startClock();

        if (PrescriptionDatePicker != null) {
            PrescriptionDatePicker.setValue(java.time.ZonedDateTime.now(APP_TZ).toLocalDate());
            // Debounce: تجميع تغييرات التاريخ 250ms قبل التحميل
            dpDebounce = new PauseTransition(Duration.millis(250));
            dpDebounce.setOnFinished(e2 -> {
                loadDashboardAsync(true);
            });
            PrescriptionDatePicker.valueProperty().addListener((obs, oldV, newV) -> {
                if (dpDebounce != null) {
                    dpDebounce.stop();
                    dpDebounce.playFromStart();
                } else {
                    loadDashboardAsync(true);
                }
            });
            // بعض المنصات تطلق فقط ActionEvent عند اختيار اليوم
            PrescriptionDatePicker.setOnAction(e -> {
                if (dpDebounce != null) {
                    dpDebounce.stop();
                    dpDebounce.playFromStart();
                } else {
                    loadDashboardAsync(true);
                }
            });
        }

        // ===== Top navigation buttons =====
        if (DashboardButton != null) {
            DashboardButton.setOnAction(e -> showDashboardPane());
        }
        if (PrescriptionsButton != null) {
            PrescriptionsButton.setOnAction(e -> showPrescriptionsPane());
        }
        if (InventoryButton != null) {
            InventoryButton.setOnAction(e -> showInventoryPane());
        }
        // Wire Finish button to handler
        if (Finish_Prescription != null) {
            Finish_Prescription.setOnAction(e -> onFinishPrescription());
        }

        // Back button: keep it simple for now — return to Dashboard
        if (BackButton != null) {
            BackButton.setOnAction(e -> showDashboardPane());
        }

        if (missing) {
            System.out.println("[PharmacyController] Some fx:id are missing. Verify FXML ids match controller fields.");
        }

        // Disable actions when offline (if OnlineBindings present)
        try { OnlineBindings.disableWhenOffline(monitor, DashboardButton, PrescriptionsButton, InventoryButton, saveBtnReceive, saveBtnDeduct); } catch (Throwable ignored) {}
        // Prevent multiple async loads via rapid clicks (if button stays enabled from bindings)
        if (DashboardButton != null) {
            DashboardButton.setOnAction(e -> {
                if (!dashboardLoading) showDashboardPane();
            });
        }

        // Inventory toggles
        wireInventoryToggles();

        // Initial section
        showDashboardPane();
        System.out.println("[PharmacyController] init -> startPharmacyDbNotifications()");
        startPharmacyDbNotifications();

        // Hard fallback: enforce Dashboard default if panes exist
        if (pharmacyDashboardAnchorPane != null && InventoryAnchorPane != null) {
            if (!pharmacyDashboardAnchorPane.isVisible() && InventoryAnchorPane.isVisible()) {
                setVisibleManaged(InventoryAnchorPane, false);
                setVisibleManaged(pharmacyDashboardAnchorPane, true);
            }
        }
        refreshPharmacyDashboardCounts();
        setupDashboardTableColumns();
        if (PresciptionsTable != null) PresciptionsTable.setItems(dashboardRows);

        setupItemsTableColumns();
        if (TablePrescriptionItems != null) {
            TablePrescriptionItems.setColumnResizePolicy(
                    javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
            );
        }
        loadDashboardTable();
    }
    private void startPharmacyDbNotifications() {
        if (dbn != null) return; // already started
        System.out.println("[PharmacyController] starting DbNotifications listeners...");
        dbn = new com.example.healthflow.db.notify.DbNotifications();

        dashCoalesce.setOnFinished(e  -> { System.out.println("[Pharm] dashCoalesce -> loadDashboardAsync(true)"); loadDashboardAsync(true); });
        itemsCoalesce.setOnFinished(e -> { System.out.println("[Pharm] itemsCoalesce -> reloadCurrentPrescriptionItems()"); reloadCurrentPrescriptionItems(); });
        invCoalesce.setOnFinished(e   -> { System.out.println("[Pharm] invCoalesce   -> reloadInventoryTable()"); reloadInventoryTable(); });

        dbn.listen("prescriptions_changed", p -> Platform.runLater(() -> {
            dashCoalesce.playFromStart();
            if (isPrescriptionVisible()) itemsCoalesce.playFromStart();
        }));

        dbn.listen("prescription_items_changed", p -> Platform.runLater(() -> {
            itemsCoalesce.playFromStart();
            dashCoalesce.playFromStart();
        }));

        dbn.listen("inventory_changed", p -> Platform.runLater(() -> {
            invCoalesce.playFromStart();
            itemsCoalesce.playFromStart();
            dashCoalesce.playFromStart();
        }));


        // أغلق المستمع عند إغلاق النافذة
        javafx.application.Platform.runLater(() -> {
            var hook = (rootPane != null) ? rootPane : pharmacyDashboardAnchorPane;
            if (hook != null && hook.getScene() != null) {
                var win = hook.getScene().getWindow();
                if (win != null) {
                    win.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, e -> stopPharmacyDbNotifications());
                    win.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDDEN, e -> stopPharmacyDbNotifications());
                }
            }
        });
    }

    private void stopPharmacyDbNotifications() {
        try { if (dbn != null) dbn.close(); } catch (Exception ignore) {}
        dbn = null;
//        System.out.println("[PharmacyController] DbNotifications stopped.");
    }
}