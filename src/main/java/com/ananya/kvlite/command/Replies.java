package com.ananya.kvlite.command;

/**
 * Reply formats of the KVLite text protocol (a simplified version of Redis' RESP).
 *
 *   OK            success with no value
 *   "value"       a string value
 *   (nil)         key not found
 *   (integer) 1   a number
 *   ERR message   an error
 */
public final class Replies {

    private Replies() {
    }

    public static final String OK = "OK";
    public static final String NIL = "(nil)";

    public static String value(String v) {
        return "\"" + v + "\"";
    }

    public static String integer(long n) {
        return "(integer) " + n;
    }

    public static String error(String message) {
        return "ERR " + message;
    }
}
