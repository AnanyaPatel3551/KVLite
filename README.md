# KVLite

A Redis-inspired, thread-safe in-memory key-value store written in Java, with:

- **O(1) LRU eviction**, built from scratch with a HashMap and a doubly linked list
- **TTL expiry**, both lazy (checked on read) and active (background sweeper)
- **Durability** through an append-only log that is replayed on restart
- **A multithreaded TCP server** speaking a simple text protocol, plus a CLI client
- **A load-testing tool** that reports throughput and p50 / p99 latency

**Benchmark:** `33,308` ops/sec with 50 concurrent clients, p99 latency `33.695` ms (on `Intel Core i7-12650H (12th Gen)`).

## Architecture

```
        KVClient / Benchmark / any TCP client
                     │  "SET a 1\n"
                     ▼
┌──────────────────────────────────────────┐
│ KVServer   (ServerSocket + thread pool)  │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│ RequestHandler                           │
│   CommandParser ──► Command (pattern)    │──► PersistenceManager (append-only log)
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│ Store (interface)                        │
│   └─ InMemoryStore  (ReadWriteLock)      │◄── TTLManager (scheduled sweeper)
│        └─ EvictionPolicy (Strategy)      │
│             └─ LRUEvictionPolicy         │
└──────────────────────────────────────────┘
```

## Design decisions

| Decision | Why |
|---|---|
| `Store` and `EvictionPolicy` are interfaces | Commands and the server never depend on concrete classes, so a new eviction policy needs no other changes (Open/Closed). |
| Command pattern for GET/SET/... | Each command is a small class; new commands don't touch the server, and write commands serialize themselves to the log. |
| Hand-written LRU (no `LinkedHashMap`) | O(1) get, put and evict using a HashMap plus a doubly linked list with sentinel nodes. |
| `ReentrantReadWriteLock` | Read-only operations can run in parallel. `GET` updates LRU order, so it takes the write lock. |
| Lazy + active expiry | Lazy alone leaks memory for keys nobody reads; active alone could return stale values between sweeps. |
| Append-only log; corrupt lines skipped on replay | Sequential writes are fast, and a crash mid-write only damages the last line. |
| Injected clock in `InMemoryStore` | TTL tests run instantly and deterministically, with no `Thread.sleep`. |
| Fixed thread pool | Caps resource use under load instead of creating one thread per client. |

## Running it

Requires Java 17+ and Maven.

```bash
mvn test                                   # 23 unit tests
mvn -q package -DskipTests
java -jar target/kvlite-1.0-SNAPSHOT.jar   # server on port 6380
```

In a second terminal, start the client:

```
$ java -cp target/classes com.ananya.kvlite.client.KVClient
Connected to KVLite at localhost:6380. Type QUIT to exit.
kvlite> SET city Bengaluru
OK
kvlite> GET city
"Bengaluru"
kvlite> EXPIRE city 60
(integer) 1
kvlite> TTL city
(integer) 60
kvlite> QUIT
BYE
```

Benchmark (with the server running):

```bash
java -cp target/classes com.ananya.kvlite.benchmark.Benchmark 50 10000
```

## Commands

| Command | Reply |
|---|---|
| `PING` | `PONG` |
| `SET key value` | `OK` (value may contain spaces) |
| `GET key` | `"value"` or `(nil)` |
| `DEL key` | `(integer) 1` or `(integer) 0` |
| `EXPIRE key seconds` | `(integer) 1` or `(integer) 0` |
| `TTL key` | seconds left, `-1` (no TTL) or `-2` (missing) |
| `QUIT` | closes the connection |

## Possible next steps

- Event-loop server with `java.nio` selectors (like real Redis) and a benchmark comparison
- LFU / FIFO eviction policies selectable by flag
- Log compaction (rewrite the log as one `SET` per live key)
- Absolute expiry timestamps in the log so TTLs survive restarts accurately
