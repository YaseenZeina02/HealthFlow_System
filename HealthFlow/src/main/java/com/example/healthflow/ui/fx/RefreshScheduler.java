package com.example.healthflow.ui.fx;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces many refresh requests and throttles execution.
 * Usage:
 *   RefreshScheduler rs = new RefreshScheduler(500);
 *   rs.request(() -> Platform.runLater(this::refreshUI));
 */
public final class RefreshScheduler {
    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ui-refresh");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private volatile long last = 0;
    private final long minGapMs;

    public RefreshScheduler(long minGapMs) {
        this.minGapMs = minGapMs;
    }

    public void request(Runnable task) {
        if (task == null) return;
        if (scheduled.compareAndSet(false, true)) {
            exec.schedule(() -> {
                scheduled.set(false);
                long now = System.currentTimeMillis();
                if (now - last < minGapMs) {
                    request(task); // re-schedule after gap
                    return;
                }
                last = now;
                task.run();
            }, 250, TimeUnit.MILLISECONDS); // small coalescing window
        }
    }

    public void shutdown() { exec.shutdownNow(); }
}
