package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/** EXPIRE key seconds -> (integer) 1 if the TTL was set, (integer) 0 if the key does not exist */
public final class ExpireCommand implements Command {

    private final String key;
    private final long seconds;

    public ExpireCommand(String key, long seconds) {
        this.key = key;
        this.seconds = seconds;
    }

    @Override
    public String execute(Store store) {
        return Replies.integer(store.expire(key, seconds * 1000) ? 1 : 0);
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String toLogLine() {
        // Known limitation: replaying a relative TTL after a restart extends the key's life.
        // Redis avoids this by logging an absolute timestamp (PEXPIREAT).
        return "EXPIRE " + key + " " + seconds;
    }
}
