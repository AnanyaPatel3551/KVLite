package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/** DEL key -> (integer) 1 if deleted, (integer) 0 if it did not exist */
public final class DelCommand implements Command {

    private final String key;

    public DelCommand(String key) {
        this.key = key;
    }

    @Override
    public String execute(Store store) {
        return Replies.integer(store.delete(key) ? 1 : 0);
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String toLogLine() {
        return "DEL " + key;
    }
}
