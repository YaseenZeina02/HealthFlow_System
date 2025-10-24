package com.example.healthflow.controllers;

import com.example.healthflow.dao.PrescriptionDAO;
import com.example.healthflow.db.Database;
import java.sql.Connection;
import java.time.LocalDate;

import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.control.ToggleGroup;
import javafx.beans.value.ChangeListener;
import org.controlsfx.control.SegmentedButton;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;

public class PharmacyController {

    private final ConnectivityMonitor monitor;

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
    private TableView<?> PresciptionsTable;

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
    private TableView<?> TablePrescriptionItems;

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
    private TableColumn<?, ?> colActionPhDashbord;

    @FXML
    private TableColumn<?, ?> colDate_Time;

    @FXML
    private TableColumn<?, ?> colStock;

    @FXML
    private TableColumn<?, ?> colItemStatus;

    @FXML
    private TableColumn<?, ?> colDiagnosisInentory;

    @FXML
    private TableColumn<?, ?> colDoctorName;

    @FXML
    private TableColumn<?, ?> colDosage;

    @FXML
    private TableColumn<?, ?> colDosageInentory;

    @FXML
    private TableColumn<?, ?> colDose;

    @FXML
    private TableColumn<?, ?> colForm;

    @FXML
    private TableColumn<?, ?> colIdx;

    @FXML
    private TableColumn<?, ?> colIdx11;

    @FXML
    private TableColumn<?, ?> colMedicineName;

    @FXML
    private TableColumn<?, ?> colMedicineNameInentory;

    @FXML
    private TableColumn<?, ?> colMedicineNameInentoryByBatchNumber;

    @FXML
    private TableColumn<?, ?> colPatientName;

    @FXML
    private TableColumn<?, ?> colPresesItemAction;

    @FXML
    private TableColumn<?, ?> colPresesActionInentory;

    @FXML
    private TableColumn<?, ?> colQuantity;

    @FXML
    private TableColumn<?, ?> colQuantityInentory;

    @FXML
    private TableColumn<?, ?> colSerialPhDashboard;

    @FXML
    private TableColumn<?, ?> colStrength;

    @FXML
    private TableColumn<?, ?> colprescriptionStutus;

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


    /* ====== Utilities / constants (match DoctorController style) ====== */
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");
    private static final String ACTIVE_CLASS = "current";

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

    // Helper to get selected date or today (Asia/Gaza)
    private LocalDate getSelectedDateOrToday() {
        try {
            LocalDate picked = (PrescriptionDatePicker != null) ? PrescriptionDatePicker.getValue() : null;
            if (picked != null) return picked;
        } catch (Throwable ignored) {}
        return java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
    }

    private void refreshPharmacyDashboardCounts() {
        if (PrescriptionsTodayTotal == null && PrescriptionsWatingNum == null && PrescriptionsCompleteNum == null) return;
        LocalDate day = getSelectedDateOrToday();
        try (Connection c = Database.get()) {
            PrescriptionDAO dao = new PrescriptionDAO();
            int total = dao.countTotalOnDate(c, day);
            int waiting = dao.countPendingOnDate(c, day);
            int completed = dao.countCompletedOnDate(c, day);
            if (PrescriptionsTodayTotal != null) PrescriptionsTodayTotal.setText(String.valueOf(total));
            if (PrescriptionsWatingNum != null) PrescriptionsWatingNum.setText(String.valueOf(waiting));
            if (PrescriptionsCompleteNum != null) PrescriptionsCompleteNum.setText(String.valueOf(completed));
        } catch (Exception ex) {
            if (alertLabel != null) alertLabel.setText("Failed to load counts: " + ex.getMessage());
            System.err.println("[PharmacyController] refresh counts error: " + ex);
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
        if (DashboardButton != null) markNavActive(DashboardButton);
        refreshPharmacyDashboardCounts();
    }

    private void showPrescriptionsPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, true);
        setVisibleManaged(InventoryAnchorPane, false);
        markNavActive(PrescriptionsButton);
        // Show the details area only when a prescription is selected later
        if (PrescriptionMedicationAnchorPane != null)
            setVisibleManaged(PrescriptionMedicationAnchorPane, true);
    }

    private void showInventoryPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, true);
        markNavActive(InventoryButton);
        // Ensure a default inventory mode
        if (btnReceive != null) {
            btnReceive.setSelected(true);
//            showInventoryMode(true);
            lastSelectedInvBtn = btnReceive;
        }
        if (segInv != null) {
            // ensure the segmented control actually contains our toggles
            if (!segInv.getButtons().contains(btnReceive) && btnReceive != null) segInv.getButtons().add(btnReceive);
            if (!segInv.getButtons().contains(btnDeduct)  && btnDeduct  != null) segInv.getButtons().add(btnDeduct);
        }
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

        // Prevent deselection of the currently selected toggle by clicking it again
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

        // default & listener
        if (btnReceive != null) {
            btnReceive.setSelected(true);
//            showInventoryMode(true);
        }

        if (btnDeduct != null) {
            btnDeduct.setSelected(true);
//            showInventoryMode(false);
        }

//
        // Remember last selected; if selection becomes null, restore last
        if (invToggleGroup != null) {
            lastSelectedInvBtn = btnReceive != null ? btnReceive : btnDeduct;
            invToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT == null) {
                    if (lastSelectedInvBtn != null) invToggleGroup.selectToggle(lastSelectedInvBtn);
                    return;
                }
                if (newT instanceof ToggleButton tb) {
                    lastSelectedInvBtn = tb;
                }
            });
        }
        if (segInv != null) {
            if (btnReceive != null && !segInv.getButtons().contains(btnReceive)) segInv.getButtons().add(btnReceive);
            if (btnDeduct  != null && !segInv.getButtons().contains(btnDeduct))  segInv.getButtons().add(btnDeduct);
        }
    }

    public PharmacyController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader
    public PharmacyController() {
        this(new ConnectivityMonitor());
    }

    @FXML
    private void initialize() {
        // Defensive: hide all sections first; we'll explicitly show one below
        btnReceive.setOnAction(e -> showInventoryReceiveMode());
        btnDeduct.setOnAction(e -> showInventoryDeductMode());

        // Quick sanity check: if IDs are not wired, navigation won't work
        boolean missing = warnIfMissing();

        // Start connectivity monitor
        monitor.start();

        // Add connectivity banner at the top of the UI
        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
        }

        // Start header clock & date (12h, Asia/Gaza)
        startClock();

        // Default the date picker to today (Asia/Gaza) and listen for changes
        if (PrescriptionDatePicker != null) {
            PrescriptionDatePicker.setValue(java.time.ZonedDateTime.now(APP_TZ).toLocalDate());
            PrescriptionDatePicker.valueProperty().addListener((obs, oldV, newV) -> refreshPharmacyDashboardCounts());
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

        // Back button: keep it simple for now — return to Dashboard
        if (BackButton != null) {
            BackButton.setOnAction(e -> showDashboardPane());
        }

        if (missing) {
            System.out.println("[PharmacyController] Some fx:id are missing. Verify FXML ids match controller fields.");
        }

        // Disable actions when offline (if OnlineBindings present)
        try { OnlineBindings.disableWhenOffline(monitor, DashboardButton, PrescriptionsButton, InventoryButton, saveBtnReceive, saveBtnDeduct); } catch (Throwable ignored) {}

        // Inventory toggles
        wireInventoryToggles();

        // Initial section
        showDashboardPane();

        // Hard fallback: enforce Dashboard default if panes exist
        if (pharmacyDashboardAnchorPane != null && InventoryAnchorPane != null) {
            if (!pharmacyDashboardAnchorPane.isVisible() && InventoryAnchorPane.isVisible()) {
                setVisibleManaged(InventoryAnchorPane, false);
                setVisibleManaged(pharmacyDashboardAnchorPane, true);
            }
        }
        refreshPharmacyDashboardCounts();
    }
}
