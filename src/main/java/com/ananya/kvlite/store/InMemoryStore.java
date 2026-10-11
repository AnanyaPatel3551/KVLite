package com.ananya.kvlite.store;

import com.ananya.kvlite.eviction.EvictionPolicy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.LongSupplier;

/**
 * Thread-safe in-memory implementation of {@link Store}.
 *
 * Design:
 *  - A HashMap holds the data.
 *  - A ReentrantReadWriteLock guards it: many readers or one writer at a time.
 *  - An EvictionPolicy (Strategy pattern) decides which key to drop at capacity.
 *  - The clock is injected (LongSupplier) so tests can control time without Thread.sleep.
 *
 * Note: get() takes the WRITE lock, not the read lock, because it updates the
 * LRU order in the eviction policy, which is a mutation.
 */
public class InMemoryStore implements Store {

    private final Map<String, Entry> data = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final EvictionPolicy evictionPolicy;
    private final int capacity;
    private final LongSupplier clock;

    public InMemoryStore(int capacity, EvictionPolicy evictionPolicy) {
        this(capacity, evictionPolicy, System::currentTimeMillis);
    }

    /** Constructor that accepts a custom clock (used by tests). */
    public InMemoryStore(int capacity, EvictionPolicy evictionPolicy, LongSupplier clock) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.evictionPolicy = evictionPolicy;
        this.clock = clock;
    }

    @Override
    public void set(String key, String value) {
        lock.writeLock().lock();
        try {
            boolean isNewKey = !data.containsKey(key);
            if (isNewKey && data.size() >= capacity) {
                evictionPolicy.evict().ifPresent(data::remove);   // make room
            }
            data.put(key, new Entry(value));   // a fresh Entry also clears any old TTL
            evictionPolicy.recordAccess(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> get(String key) {
        lock.writeLock().lock();   // write lock: recordAccess() changes the LRU list
        try {
            Entry entry = data.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            if (entry.isExpired(clock.getAsLong())) {   // lazy expiry
                data.remove(key);
                evictionPolicy.remove(key);
                return Optional.empty();
            }
            evictionPolicy.recordAccess(key);
            return Optional.of(entry.getValue());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean delete(String key) {
        lock.writeLock().lock();
        try {
            evictionPolicy.remove(key);
            return data.remove(key) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean expire(String key, long ttlMillis) {
        lock.writeLock().lock();
        try {
            Entry entry = data.get(key);
            long now = clock.getAsLong();
            if (entry == null || entry.isExpired(now)) {
                return false;
            }
            data.put(key, entry.withExpiry(now + ttlMillis));   // Entry is immutable, so replace it
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public long ttl(String key) {
        lock.readLock().lock();   // read lock is enough: nothing is modified here
        try {
            Entry entry = data.get(key);
            long now = clock.getAsLong();
            if (entry == null || entry.isExpired(now)) {
                return -2;   // key does not exist
            }
            if (!entry.hasExpiry()) {
                return -1;   // key exists but never expires
            }
            return entry.getExpiresAtMillis() - now;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return data.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int removeExpired() {
        lock.writeLock().lock();
        try {
            long now = clock.getAsLong();
            int removed = 0;
            // An explicit Iterator allows safe removal while looping;
            // calling data.remove() inside a for-each loop would throw ConcurrentModificationException.
            Iterator<Map.Entry<String, Entry>> it = data.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Entry> e = it.next();
                if (e.getValue().isExpired(now)) {
                    it.remove();
                    evictionPolicy.remove(e.getKey());
                    removed++;
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
