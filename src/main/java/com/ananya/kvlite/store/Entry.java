package com.ananya.kvlite.store;

/**
 * An immutable stored value plus its optional expiry time.
 * Immutability makes entries safe to share between threads without extra locking.
 */
public final class Entry {

    /** Sentinel meaning "this key never expires". */
    public static final long NO_EXPIRY = -1L;

    private final String value;
    private final long expiresAtMillis;

    public Entry(String value) {
        this(value, NO_EXPIRY);
    }

    public Entry(String value, long expiresAtMillis) {
        this.value = value;
        this.expiresAtMillis = expiresAtMillis;
    }

    public String getValue() {
        return value;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean hasExpiry() {
        return expiresAtMillis != NO_EXPIRY;
    }

    /** True if this entry has a TTL and that time has passed. */
    public boolean isExpired(long nowMillis) {
        return hasExpiry() && nowMillis >= expiresAtMillis;
    }

    /** Returns a copy of this entry with a new expiry time. */
    public Entry withExpiry(long newExpiresAtMillis) {
        return new Entry(value, newExpiresAtMillis);
    }
}
