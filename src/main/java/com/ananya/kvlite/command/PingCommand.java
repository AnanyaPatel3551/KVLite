package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/** PING -> PONG. Used to check that the server is alive. */
public final class PingCommand implements Command {

    @Override
    public String execute(Store store) {
        return "PONG";
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public String toLogLine() {
        return "PING";
    }
}
