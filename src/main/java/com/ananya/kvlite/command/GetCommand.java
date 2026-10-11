package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/** GET key -> "value" or (nil) */
public final class GetCommand implements Command {

    private final String key;

    public GetCommand(String key) {
        this.key = key;
    }

    @Override
    public String execute(Store store) {
        return store.get(key).map(Replies::value).orElse(Replies.NIL);
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public String toLogLine() {
        return "GET " + key;
    }
}
