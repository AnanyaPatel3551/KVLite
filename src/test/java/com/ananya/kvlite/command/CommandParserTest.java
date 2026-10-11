package com.ananya.kvlite.command;

import com.ananya.kvlite.eviction.LRUEvictionPolicy;
import com.ananya.kvlite.server.RequestHandler;
import com.ananya.kvlite.store.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for CommandParser, the commands, and RequestHandler (end to end, without a socket). */
class CommandParserTest {

    private CommandParser parser;
    private RequestHandler handler;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        handler = new RequestHandler(new InMemoryStore(100, new LRUEvictionPolicy()), parser, null);
    }

    @Test
    void parsesCaseInsensitively() {
        assertInstanceOf(GetCommand.class, parser.parse("get k"));
        assertInstanceOf(SetCommand.class, parser.parse("SeT k v"));
        assertInstanceOf(PingCommand.class, parser.parse("ping"));
    }

    @Test
    void setValueKeepsSpaces() {
        assertEquals("SET name Ananya Patel", parser.parse("SET name Ananya Patel").toLogLine());
    }

    @Test
    void rejectsBadInput() {
        assertThrows(CommandParser.ParseException.class, () -> parser.parse(""));
        assertThrows(CommandParser.ParseException.class, () -> parser.parse("GET"));
        assertThrows(CommandParser.ParseException.class, () -> parser.parse("FLY away"));
        assertThrows(CommandParser.ParseException.class, () -> parser.parse("EXPIRE k soon"));
    }

    @Test
    void endToEndConversation() {
        assertEquals("PONG", handler.handle("PING"));
        assertEquals("OK", handler.handle("SET city Bengaluru"));
        assertEquals("\"Bengaluru\"", handler.handle("GET city"));
        assertEquals("(integer) 1", handler.handle("EXPIRE city 60"));
        assertEquals("(integer) 60", handler.handle("TTL city"));
        assertEquals("(integer) 1", handler.handle("DEL city"));
        assertEquals("(nil)", handler.handle("GET city"));
        assertEquals("(integer) 0", handler.handle("DEL city"));
    }

    @Test
    void errorsBecomeErrReplies() {
        assertTrue(handler.handle("NONSENSE").startsWith("ERR"));
        assertTrue(handler.handle("GET").startsWith("ERR"));
    }
}
