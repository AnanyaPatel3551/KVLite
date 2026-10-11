package com.ananya.kvlite.eviction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Least-Recently-Used eviction policy with O(1) time for every operation.
 *
 * Structure:
 *   HashMap<String, Node>  -> finds a key's node in O(1)
 *   Doubly linked list     -> most recently used at the head, least recently used at the tail
 *
 *   head(sentinel) <-> [newest] <-> ... <-> [oldest] <-> tail(sentinel)
 *
 * Sentinel head/tail nodes remove the need for null checks at the ends of the list.
 * The list is doubly linked so a node can be unlinked in O(1) without walking the list
 * to find its predecessor.
 *
 * Not thread-safe on its own: InMemoryStore calls it only while holding its write lock.
 */
public class LRUEvictionPolicy implements EvictionPolicy {

    private static final class Node {
        final String key;
        Node prev;
        Node next;

        Node(String key) {
            this.key = key;
        }
    }

    private final Map<String, Node> nodes = new HashMap<>();
    private final Node head = new Node(null); // head.next is the most recently used
    private final Node tail = new Node(null); // tail.prev is the least recently used

    public LRUEvictionPolicy() {
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public void recordAccess(String key) {
        Node node = nodes.get(key);
        if (node != null) {
            unlink(node);              // already tracked: detach from its current position
        } else {
            node = new Node(key);      // first time we see this key
            nodes.put(key, node);
        }
        addToFront(node);              // either way, it is now the most recently used
    }

    @Override
    public void remove(String key) {
        Node node = nodes.remove(key);
        if (node != null) {
            unlink(node);
        }
    }

    @Override
    public Optional<String> evict() {
        if (tail.prev == head) {
            return Optional.empty();   // list is empty
        }
        Node victim = tail.prev;       // least recently used
        unlink(victim);
        nodes.remove(victim.key);
        return Optional.of(victim.key);
    }

    @Override
    public int size() {
        return nodes.size();
    }

    /** Inserts the node directly after the head sentinel. */
    private void addToFront(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    /** Detaches the node by connecting its neighbours to each other. */
    private void unlink(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
}
