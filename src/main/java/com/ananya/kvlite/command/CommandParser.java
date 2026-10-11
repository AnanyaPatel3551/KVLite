package com.ananya.kvlite.command;

/**
 * Parses one line of client input into a {@link Command}.
 *
 *   "SET name Ananya Patel" -> SetCommand("name", "Ananya Patel")  (value = rest of the line)
 *   "get name"              -> GetCommand("name")                  (command names are case-insensitive)
 *
 * Malformed input throws {@link ParseException}, which the server turns into an "ERR ..." reply.
 */
public class CommandParser {

    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            throw new ParseException("empty command");
        }
        // Split into at most 3 parts so a SET value can contain spaces.
        String[] parts = line.trim().split("\\s+", 3);
        String name = parts[0].toUpperCase();

        switch (name) {
            case "PING":
                return new PingCommand();
            case "GET":
                requireArgs(parts, 2, "GET key");
                return new GetCommand(parts[1]);
            case "SET":
                requireArgs(parts, 3, "SET key value");
                return new SetCommand(parts[1], parts[2]);
            case "DEL":
                requireArgs(parts, 2, "DEL key");
                return new DelCommand(parts[1]);
            case "TTL":
                requireArgs(parts, 2, "TTL key");
                return new TtlCommand(parts[1]);
            case "EXPIRE":
                requireArgs(parts, 3, "EXPIRE key seconds");
                try {
                    return new ExpireCommand(parts[1], Long.parseLong(parts[2]));
                } catch (NumberFormatException e) {
                    throw new ParseException("EXPIRE seconds must be a number");
                }
            default:
                throw new ParseException("unknown command '" + parts[0] + "'");
        }
    }

    private static void requireArgs(String[] parts, int expected, String usage) {
        if (parts.length < expected) {
            throw new ParseException("wrong number of arguments. Usage: " + usage);
        }
    }

    /** Thrown for any malformed command. */
    public static class ParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ParseException(String message) {
            super(message);
        }
    }
}
