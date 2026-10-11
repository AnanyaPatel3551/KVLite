package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/** TTL key -> remaining seconds, -1 if the key has no TTL, -2 if the key does not exist */
public final class TtlCommand implements Command {

    private final String key;

    public TtlCommand(String key) {
        this.key = key;
    }

    @Override
    public String execute(Store store) {
        long millis = store.ttl(key);
        if (millis < 0) {
            return Replies.integer(millis);   // -1 or -2 status codes
        }
        return Replies.integer((long) Math.ceil(millis / 1000.0));
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public String toLogLine() {
        return "TTL " + key;
    }
}
