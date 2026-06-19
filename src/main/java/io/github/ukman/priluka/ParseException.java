package io.github.ukman.priluka;

/**
 * Thrown when input cannot be parsed by a valid grammar.
 */
public class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
