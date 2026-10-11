package com.ananya.kvlite.server;

import com.ananya.kvlite.command.Command;
import com.ananya.kvlite.command.CommandParser;
import com.ananya.kvlite.command.Replies;
import com.ananya.kvlite.persistence.PersistenceManager;
import com.ananya.kvlite.store.Store;

/**
 * Turns one request line into one reply line: parse -> execute -> persist (if it was a write).
 *
 * Kept separate from the socket code (Single Responsibility), so all request logic
 * can be unit-tested without opening a network connection.
 * `persistence` may be null when persistence is disabled (e.g. in tests).
 */
public class RequestHandler {

    private final Store store;
    private final CommandParser parser;
    private final PersistenceManager persistence;

    public RequestHandler(Store store, CommandParser parser, PersistenceManager persistence) {
        this.store = store;
        this.parser = parser;
        this.persistence = persistence;
    }

    /** Never throws: every failure becomes an "ERR ..." reply, so one bad request can't kill a connection. */
    public String handle(String line) {
        try {
            Command command = parser.parse(line);
            String reply = command.execute(store);
            // Logged only after a successful execute, so failed commands never reach the log.
            if (persistence != null && command.isWrite()) {
                persistence.append(command);
            }
            return reply;
        } catch (CommandParser.ParseException e) {
            return Replies.error(e.getMessage());
        } catch (Exception e) {
            return Replies.error("internal error: " + e.getMessage());
        }
    }
}
