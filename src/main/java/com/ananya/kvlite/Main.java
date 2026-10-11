package com.ananya.kvlite;

import com.ananya.kvlite.command.CommandParser;
import com.ananya.kvlite.eviction.LRUEvictionPolicy;
import com.ananya.kvlite.persistence.PersistenceManager;
import com.ananya.kvlite.server.KVServer;
import com.ananya.kvlite.server.RequestHandler;
import com.ananya.kvlite.store.InMemoryStore;
import com.ananya.kvlite.store.Store;
import com.ananya.kvlite.ttl.TTLManager;

import java.nio.file.Path;

/**
 * Entry point. The only place that knows the concrete classes; everything else depends on
 * interfaces (manual dependency injection / composition root).
 *
 * Usage: java -jar target/kvlite-1.0-SNAPSHOT.jar [port] [capacity]
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6380;
        int capacity = args.length > 1 ? Integer.parseInt(args[1]) : 10_000;

        Store store = new InMemoryStore(capacity, new LRUEvictionPolicy());
        CommandParser parser = new CommandParser();

        PersistenceManager persistence = new PersistenceManager(Path.of("data", "kvlite.aof"), parser);
        int replayed = persistence.replay(store);
        System.out.println("Replayed " + replayed + " commands from the log");
        persistence.open();

        TTLManager ttl = new TTLManager(store, 1000);
        ttl.start();

        KVServer server = new KVServer(port, 64, new RequestHandler(store, parser, persistence));

        // Clean shutdown on Ctrl+C: stop accepting clients, stop the sweeper, close the log.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
                ttl.close();
                persistence.close();
                System.out.println("KVLite stopped cleanly");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        System.out.println("KVLite ready on port " + port + " (capacity " + capacity + " keys)");
        server.start();
    }
}
