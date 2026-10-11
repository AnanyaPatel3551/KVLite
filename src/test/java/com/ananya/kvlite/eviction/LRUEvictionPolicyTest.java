package com.ananya.kvlite.eviction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the O(1) LRU eviction policy. */
class LRUEvictionPolicyTest {

    private LRUEvictionPolicy lru;

    @BeforeEach
    void setUp() {
        lru = new LRUEvictionPolicy();
    }

    @Test
    void emptyPolicyEvictsNothing() {
        assertEquals(Optional.empty(), lru.evict());
    }

    @Test
    void evictsLeastRecentlyInserted() {
        lru.recordAccess("a");
        lru.recordAccess("b");
        lru.recordAccess("c");
        assertEquals(Optional.of("a"), lru.evict());
        assertEquals(Optional.of("b"), lru.evict());
        assertEquals(Optional.of("c"), lru.evict());
        assertEquals(Optional.empty(), lru.evict());
    }

    @Test
    void accessMovesKeyToMostRecent() {
        lru.recordAccess("a");
        lru.recordAccess("b");
        lru.recordAccess("a"); // a is now newest
        assertEquals(Optional.of("b"), lru.evict());
        assertEquals(Optional.of("a"), lru.evict());
    }

    @Test
    void repeatedAccessDoesNotDuplicate() {
        lru.recordAccess("a");
        lru.recordAccess("a");
        lru.recordAccess("a");
        assertEquals(1, lru.size());
    }

    @Test
    void removedKeyIsNeverEvicted() {
        lru.recordAccess("a");
        lru.recordAccess("b");
        lru.remove("a");
        assertEquals(1, lru.size());
        assertEquals(Optional.of("b"), lru.evict());
    }

    @Test
    void removingUnknownKeyIsHarmless() {
        lru.remove("ghost");
        assertEquals(0, lru.size());
    }
}
