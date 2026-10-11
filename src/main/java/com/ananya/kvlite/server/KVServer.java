package com.ananya.kvlite.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Multithreaded TCP server speaking a line-based text protocol:
 * one line in = one command, one line out = one reply.
 *
 * The main thread only accepts connections; each client is served by a thread
 * from a fixed-size pool, which caps resource usage under load (extra clients
 * wait in the pool's queue instead of each spawning a new thread).
 */
public class KVServer implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(KVServer.class.getName());

    private final int port;
    private final RequestHandler handler;
    private final ExecutorService clientPool;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public KVServer(int port, int maxClients, RequestHandler handler) {
        this.port = port;
        this.handler = handler;
        this.clientPool = Executors.newFixedThreadPool(maxClients);
    }

    /** Blocks, accepting clients until close() is called. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        LOG.info("KVLite listening on port " + port);
        while (running) {
            try {
                Socket client = serverSocket.accept();
                clientPool.submit(() -> serveClient(client));
            } catch (SocketException e) {
                if (running) {
                    throw e; // a real error; if !running the socket was closed on purpose
                }
            }
        }
    }

    /** Serves one client until it disconnects or sends QUIT. */
    private void serveClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("QUIT")) {
                    out.println("BYE");
                    break;
                }
                out.println(handler.handle(line));
            }
        } catch (IOException e) {
            LOG.fine("Client disconnected: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        clientPool.shutdown();
        try {
            if (!clientPool.awaitTermination(2, TimeUnit.SECONDS)) {
                clientPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
