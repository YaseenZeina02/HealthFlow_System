package com.example.healthflow.controllers;

import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class ParientController {

    private final ConnectivityMonitor monitor;

    @FXML
    private VBox rootPane;

    public ParientController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader
    public ParientController() {
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
