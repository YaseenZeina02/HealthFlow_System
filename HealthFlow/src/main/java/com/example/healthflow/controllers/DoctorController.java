package com.example.healthflow.controllers;

import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;


public class DoctorController {
    @FXML
    private AnchorPane Appointments; //Today's Appointments

    @FXML
    private Label TotalAppointments;

    @FXML
    private AnchorPane Appointments21;  //Today's Patients

    @FXML
    private Label TotalAppointments21;

    @FXML
    private AnchorPane Appointments2; //Completed

    @FXML
    private Label TotalAppointments2;

    @FXML
    private AnchorPane Appointments22; //Remaining

    @FXML
    private Label TotalAppointments22;

    @FXML
    private TableView<?> AppointmentsTable;

    @FXML
    private Button BackButton;

    @FXML
    private AnchorPane CenterAnchorPane;

    @FXML
    private Button DachboardButton;

    @FXML
    private AnchorPane DashboardAnchorPane;

    @FXML
    private Label DateOfDay;

    @FXML
    private AnchorPane PatientAnchorPane;

    @FXML
    private Button PatientsButton;

    @FXML
    private Label RemainingNum;

    @FXML
    private Label TotalAppointmentsNum;

    @FXML
    private Label TotalPatientsNum;

    @FXML
    private Label UserIdLabel;

    @FXML
    private Label UsernameLabel;

    @FXML
    private TableColumn<?, ?> colAction;

    @FXML
    private TableColumn<?, ?> colAction2;

    @FXML
    private TableColumn<?, ?> colDate;

    @FXML
    private TableColumn<?, ?> colDob;

    @FXML
    private TableColumn<?, ?> colGender;

    @FXML
    private TableColumn<?, ?> colMedicalHistory;

    @FXML
    private TableColumn<?, ?> colName;

    @FXML
    private TableColumn<?, ?> colNationalId;

    @FXML
    private TableColumn<?, ?> colPatientName;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private TableColumn<?, ?> colTime;

    @FXML
    private Label patientCompleteNum;

    @FXML
    private TableView<?> patientTable;

    @FXML
    private TextField search;

    @FXML
    private TextField searchLabel;

    @FXML
    private Label time;

    @FXML
    private Label userStatus;

    @FXML
    private Label welcomeUser;

    private final ConnectivityMonitor monitor;

    @FXML
    private VBox rootPane;

    public DoctorController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader
    public DoctorController() {
        this(new ConnectivityMonitor());
    }

    @FXML
    private void initialize() {
        // Start connectivity monitor
        monitor.start();

        // Add connectivity banner at the top of the UI
        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
        }

        // Disable buttons when offline
        // Example: OnlineBindings.disableWhenOffline(monitor, button1, button2);
    }
}
