package com.ananya.kvlite.eviction;

import java.util.Optional;

/**
 * Decides which key to remove when the store is full (Strategy pattern).
 * New policies (LFU, FIFO) can be added without changing the store.
 *
 * Implementations need not be thread-safe: the store calls them while holding its write lock.
 */
public interface EvictionPolicy {

    /** Called whenever a key is read or written. */
    void recordAccess(String key);

    /** Called when a key is deleted or expires, so the policy stops tracking it. */
    void remove(String key);

    /** Chooses a key to evict, stops tracking it, and returns it. Empty if nothing is tracked. */
    Optional<String> evict();

    /** Number of keys being tracked. */
    int size();
}
