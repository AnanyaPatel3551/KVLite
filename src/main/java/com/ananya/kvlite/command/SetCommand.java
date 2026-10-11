package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/** SET key value -> OK */
public final class SetCommand implements Command {

    private final String key;
    private final String value;

    public SetCommand(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String execute(Store store) {
        store.set(key, value);
        return Replies.OK;
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String toLogLine() {
        // The parser treats everything after the key as the value, so spaces survive a replay.
        return "SET " + key + " " + value;
    }
}
