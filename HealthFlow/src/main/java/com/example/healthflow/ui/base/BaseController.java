package com.example.healthflow.ui.base;

import com.example.healthflow.net.ConnectivityMonitor;
import javafx.application.Platform;

/**
 * Common base class for controllers. Keep it thin.
 */
public abstract class BaseController {
    protected final ConnectivityMonitor monitor;

    protected BaseController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }
    protected BaseController() {
        this.monitor = null;
    }

    protected void runFx(Runnable r) { Platform.runLater(r); }

    protected boolean ensureOnlineOrAlert() {
        return OnlineGuards.ensureOnlineOrAlert(monitor);
    }
}
