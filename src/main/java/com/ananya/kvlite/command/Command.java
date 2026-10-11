package com.ananya.kvlite.command;

import com.ananya.kvlite.store.Store;

/**
 * A client command (Command pattern): each request becomes an object that executes itself.
 * Adding a command means adding a class; the server never changes (Open/Closed principle).
 * Write commands serialize themselves for the append-only log via {@link #toLogLine()}.
 */
public interface Command {

    /** Runs the command against the store and returns the reply sent to the client. */
    String execute(Store store);

    /** True if this command changes data and must be written to the persistence log. */
    boolean isWrite();

    /** The command as a single protocol line, e.g. "SET name ananya". Replayed on startup. */
    String toLogLine();
}
