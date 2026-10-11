package com.ananya.kvlite.benchmark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Load test: N concurrent clients each send M commands (alternating SET/GET) to a running server,
 * and the tool reports throughput plus p50 / p99 request latency.
 *
 * Usage (with the server already running):
 *   java -cp target/classes com.ananya.kvlite.benchmark.Benchmark [clients] [opsPerClient] [port]
 */
public final class Benchmark {

    private Benchmark() {
    }

    public static void main(String[] args) throws Exception {
        int clients = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        int opsPerClient = args.length > 1 ? Integer.parseInt(args[1]) : 10_000;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 6380;

        ExecutorService pool = Executors.newFixedThreadPool(clients);
        List<Future<long[]>> results = new ArrayList<>();

        long start = System.nanoTime();
        for (int c = 0; c < clients; c++) {
            final int clientId = c;
            results.add(pool.submit(() -> runClient(clientId, opsPerClient, port)));
        }

        long[] allLatencies = new long[clients * opsPerClient];
        int offset = 0;
        for (Future<long[]> f : results) {
            long[] latencies = f.get();
            System.arraycopy(latencies, 0, allLatencies, offset, latencies.length);
            offset += latencies.length;
        }
        long elapsedNanos = System.nanoTime() - start;
        pool.shutdown();

        Arrays.sort(allLatencies);
        double seconds = elapsedNanos / 1_000_000_000.0;
        System.out.printf("Clients: %d | Ops: %d | Time: %.2fs%n", clients, allLatencies.length, seconds);
        System.out.printf("Throughput: %.0f ops/sec%n", allLatencies.length / seconds);
        System.out.printf("Latency p50: %.3f ms | p99: %.3f ms%n",
                percentileMillis(allLatencies, 50), percentileMillis(allLatencies, 99));
    }

    /** Sends ops alternating SET/GET and returns the latency of each request in nanoseconds. */
    private static long[] runClient(int clientId, int ops, int port) throws Exception {
        long[] latencies = new long[ops];
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            for (int i = 0; i < ops; i++) {
                String key = "k:" + clientId + ":" + (i % 1000);
                long t0 = System.nanoTime();
                out.println(i % 2 == 0 ? "SET " + key + " v" + i : "GET " + key);
                in.readLine();
                latencies[i] = System.nanoTime() - t0;
            }
        }
        return latencies;
    }

    /** Value at the given percentile of a sorted array, in milliseconds. */
    private static double percentileMillis(long[] sortedNanos, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedNanos.length) - 1;
        index = Math.max(0, Math.min(index, sortedNanos.length - 1));
        return sortedNanos[index] / 1_000_000.0;
    }
}
