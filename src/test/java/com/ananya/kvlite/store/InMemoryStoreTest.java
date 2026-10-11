package com.ananya.kvlite.store;

import com.ananya.kvlite.eviction.LRUEvictionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for InMemoryStore. A fake clock (AtomicLong) makes TTL tests instant and
 * deterministic, with no Thread.sleep().
 */
class InMemoryStoreTest {

    private AtomicLong now;
    private InMemoryStore store;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(1_000_000);
        store = new InMemoryStore(3, new LRUEvictionPolicy(), now::get);
    }

    @Test
    void setThenGet() {
        store.set("name", "ananya");
        assertEquals(Optional.of("ananya"), store.get("name"));
    }

    @Test
    void getMissingKeyIsEmpty() {
        assertEquals(Optional.empty(), store.get("missing"));
    }

    @Test
    void setOverwrites() {
        store.set("k", "1");
        store.set("k", "2");
        assertEquals(Optional.of("2"), store.get("k"));
        assertEquals(1, store.size());
    }

    @Test
    void deleteRemovesKey() {
        store.set("k", "v");
        assertTrue(store.delete("k"));
        assertFalse(store.delete("k"));
        assertEquals(Optional.empty(), store.get("k"));
    }

    @Test
    void evictsLeastRecentlyUsedWhenFull() {
        store.set("a", "1");
        store.set("b", "2");
        store.set("c", "3");
        store.get("a");          // a is now most recent; b is least recent
        store.set("d", "4");     // capacity 3 -> evict b
        assertEquals(Optional.empty(), store.get("b"));
        assertEquals(Optional.of("1"), store.get("a"));
        assertEquals(3, store.size());
    }

    @Test
    void overwritingAtCapacityDoesNotEvict() {
        store.set("a", "1");
        store.set("b", "2");
        store.set("c", "3");
        store.set("a", "updated");
        assertEquals(3, store.size());
        assertEquals(Optional.of("2"), store.get("b"));
    }

    @Test
    void keyExpiresAfterTtl() {
        store.set("session", "xyz");
        assertTrue(store.expire("session", 5_000));
        now.addAndGet(4_999);
        assertEquals(Optional.of("xyz"), store.get("session"));
        now.addAndGet(1);
        assertEquals(Optional.empty(), store.get("session"));
    }

    @Test
    void expireOnMissingKeyReturnsFalse() {
        assertFalse(store.expire("nope", 1_000));
    }

    @Test
    void ttlCodes() {
        assertEquals(-2, store.ttl("missing"));
        store.set("k", "v");
        assertEquals(-1, store.ttl("k"));
        store.expire("k", 10_000);
        now.addAndGet(3_000);
        assertEquals(7_000, store.ttl("k"));
    }

    @Test
    void setClearsExistingTtl() {
        store.set("k", "v");
        store.expire("k", 1_000);
        store.set("k", "v2");
        now.addAndGet(5_000);
        assertEquals(Optional.of("v2"), store.get("k"));
    }

    @Test
    void removeExpiredSweepsOnlyExpiredKeys() {
        store.set("a", "1");
        store.set("b", "2");
        store.expire("a", 100);
        now.addAndGet(200);
        assertEquals(1, store.removeExpired());
        assertEquals(1, store.size());
    }

    @Test
    void concurrentWritesAreSafe() throws Exception {
        InMemoryStore big = new InMemoryStore(100_000, new LRUEvictionPolicy());
        int threads = 16;
        int perThread = 2_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            futures.add(pool.submit(() -> {
                go.await();
                for (int i = 0; i < perThread; i++) {
                    big.set("t" + id + ":" + i, "v");
                    big.get("t" + id + ":" + (i / 2));
                }
                return null;
            }));
        }
        go.countDown(); // release all threads at the same moment to maximise contention
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        assertEquals(threads * perThread, big.size());
    }
}
