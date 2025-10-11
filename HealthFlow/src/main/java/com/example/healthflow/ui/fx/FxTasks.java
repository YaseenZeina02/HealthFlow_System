package com.example.healthflow.ui.fx;

import javafx.application.Platform;

public final class FxTasks {
    private FxTasks() {}

    /** Run a background task on a daemon thread with a readable name. */
    public static void runBG(String name, Runnable r) {
        Thread t = new Thread(r, name == null ? "bg-task" : name);
        t.setDaemon(true);
        t.start();
    }

    /** Dispatch to the JavaFX Application Thread. */
    public static void runFx(Runnable r) {
        Platform.runLater(r);
    }
}
