package com.example.healthflow.net;


import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class ConnectivityMonitor {
    private final String probeUrl;
    private final Duration interval;
    private final Duration timeout;

    private final BooleanProperty online = new SimpleBooleanProperty(false);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "connectivity-monitor");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> task;

    public ConnectivityMonitor(String probeUrl, Duration interval, Duration timeout) {
        this.probeUrl = probeUrl;
        this.interval = interval;
        this.timeout  = timeout;
    }

    public BooleanProperty onlineProperty() { return online; }
    public boolean isOnline() { return online.get(); }

    public void start() {
        if (task != null && !task.isCancelled()) return;
        task = scheduler.scheduleAtFixedRate(this::probe, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(true);
        scheduler.shutdownNow();
    }

    public void checkNow() { scheduler.execute(this::probe); }

    private void probe() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(probeUrl))
                    .timeout(timeout)
                    .header("Cache-Control", "no-cache")
                    .GET().build();

            client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .thenApply(resp -> resp.statusCode() >= 200 && resp.statusCode() < 400)
                    .exceptionally(ex -> false)
                    .thenAccept(isUp -> Platform.runLater(() -> online.set(isUp)));
        } catch (Exception ex) {
            Platform.runLater(() -> online.set(false));
        }
    }
}