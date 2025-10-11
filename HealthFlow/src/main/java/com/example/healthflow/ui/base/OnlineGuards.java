package com.example.healthflow.ui.base;

import com.example.healthflow.net.ConnectivityMonitor;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;

/**
 * Thin helpers around ConnectivityMonitor to avoid repeating boilerplate.
 * (If you already use OnlineBindings elsewhere, this class is complementary.)
 */
public final class OnlineGuards {
    private OnlineGuards() {}

    /** Disable the given nodes while offline. */
    public static void disableWhenOffline(ConnectivityMonitor monitor, Node... nodes) {
        if (monitor == null || nodes == null) return;
        BooleanBinding offline = monitor.onlineProperty().not();
        for (Node n : nodes) {
            if (n != null) n.disableProperty().bind(offline);
        }
    }

    /** Convenience check + dialog. */
    public static boolean ensureOnlineOrAlert(ConnectivityMonitor monitor) {
        if (monitor != null && !monitor.isOnline()) {
            Dialogs.warn("Offline", "You're offline. Please reconnect and try again.");
            return false;
        }
        return true;
    }
}
