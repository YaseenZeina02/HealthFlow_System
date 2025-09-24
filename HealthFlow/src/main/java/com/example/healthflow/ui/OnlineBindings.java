package com.example.healthflow.ui;


import com.example.healthflow.net.ConnectivityMonitor;


import javafx.scene.Node;

public final class OnlineBindings {
    private OnlineBindings() {}

    // تعطيل عناصر عند انقطاع الاتصال
    public static void disableWhenOffline(ConnectivityMonitor monitor, Node... nodes) {
        for (Node n : nodes) {
            n.disableProperty().bind(monitor.onlineProperty().not());
        }
    }

    // إخفاء عناصر عند انقطاع الاتصال (اختياري)
    public static void hideWhenOffline(ConnectivityMonitor monitor, Node... nodes) {
        for (Node n : nodes) {
            n.visibleProperty().bind(monitor.onlineProperty());
            n.managedProperty().bind(n.visibleProperty());
        }
    }
}