package com.ananya.kvlite.store;

import java.util.Optional;

/**
 * Contract for the key-value store.
 * Commands, the server and persistence depend on this interface rather than on a concrete
 * implementation (dependency inversion), so the storage engine can be swapped freely.
 */
public interface Store {

    /** Stores a value with no expiry, overwriting any existing value and clearing its TTL. */
    void set(String key, String value);

    /** Returns the value, or empty if the key is missing or expired. */
    Optional<String> get(String key);

    /** Removes the key. Returns true if it existed. */
    boolean delete(String key);

    /**
     * Sets a time-to-live on an existing key.
     * @return true if the key existed and the TTL was set
     */
    boolean expire(String key, long ttlMillis);

    /** Remaining TTL in ms; -1 if the key has no TTL; -2 if the key does not exist (as in Redis). */
    long ttl(String key);

    /** Number of keys currently stored (may include expired keys not yet swept). */
    int size();

    /** Removes every expired key. Returns how many were removed. */
    int removeExpired();
}
