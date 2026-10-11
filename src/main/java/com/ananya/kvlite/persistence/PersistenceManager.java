package com.ananya.kvlite.persistence;

import com.ananya.kvlite.command.Command;
import com.ananya.kvlite.command.CommandParser;
import com.ananya.kvlite.store.Store;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * Durability through an append-only file (AOF), the same idea Redis uses.
 *
 *  - Every successful write command is appended to the log as one line.
 *  - On startup, replay() re-runs every line, rebuilding the same state.
 *
 * Append-only writes are sequential (fast), and a crash can only damage the last line.
 * Each append is flushed immediately: safest, at some cost to throughput.
 */
public class PersistenceManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(PersistenceManager.class.getName());

    private final Path logFile;
    private final CommandParser parser;
    private BufferedWriter writer;

    public PersistenceManager(Path logFile, CommandParser parser) {
        this.logFile = logFile;
        this.parser = parser;
    }

    /**
     * Re-applies every command in the log to the store. Call before open() and before the server starts.
     * @return number of commands replayed
     */
    public int replay(Store store) throws IOException {
        if (!Files.exists(logFile)) {
            return 0;
        }
        int count = 0;
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            try {
                parser.parse(line).execute(store);
                count++;
            } catch (RuntimeException e) {
                // A half-written last line after a crash must not stop the database from starting.
                LOG.warning("Skipping corrupt log line: " + line);
            }
        }
        return count;
    }

    /** Opens the log for appending. */
    public void open() throws IOException {
        if (logFile.getParent() != null) {
            Files.createDirectories(logFile.getParent());
        }
        writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Appends one write command. Synchronized so lines from concurrent clients never interleave. */
    public synchronized void append(Command command) throws IOException {
        if (!command.isWrite()) {
            return;
        }
        writer.write(command.toLogLine());
        writer.newLine();
        writer.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
