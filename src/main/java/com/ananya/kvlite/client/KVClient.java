package com.ananya.kvlite.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Interactive command-line client for KVLite (like redis-cli).
 *
 * Usage: java -cp target/classes com.ananya.kvlite.client.KVClient [host] [port]
 */
public final class KVClient {

    private KVClient() {
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 6380;

        try (Socket socket = new Socket(host, port);
             BufferedReader server = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("Connected to KVLite at " + host + ":" + port + ". Type QUIT to exit.");
            while (true) {
                System.out.print("kvlite> ");
                System.out.flush();
                String line = keyboard.readLine();
                if (line == null) {
                    break; // end of input
                }
                if (line.isBlank()) {
                    continue;
                }
                out.println(line);
                String reply = server.readLine();
                if (reply == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }
                System.out.println(reply);
                if (line.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        }
    }
}
