package com.ananya.kvlite.ttl;

import com.ananya.kvlite.store.Store;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Active expiry: a background thread that periodically removes expired keys.
 *
 * KVLite combines two strategies, like Redis:
 *   1. Lazy expiry   - InMemoryStore.get() drops an expired key when it is read.
 *   2. Active expiry - this sweeper removes expired keys nobody reads, so they don't waste memory.
 */
public class TTLManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(TTLManager.class.getName());

    private final Store store;
    private final long intervalMillis;
    private final ScheduledExecutorService scheduler;

    public TTLManager(Store store, long intervalMillis) {
        this.store = store;
        this.intervalMillis = intervalMillis;
        // Daemon thread, so it never keeps the JVM alive on its own.
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kvlite-ttl-sweeper");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sweep, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    /** One sweep. Package-private so tests can call it directly. */
    void sweep() {
        // Catch everything: if a scheduled task throws, the executor silently cancels all future runs.
        try {
            int removed = store.removeExpired();
            if (removed > 0) {
                LOG.fine("TTL sweep removed " + removed + " expired keys");
            }
        } catch (Exception e) {
            LOG.warning("TTL sweep failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
