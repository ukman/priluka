package io.github.ukman.priluka;

/**
 * Thrown when Java declarations cannot be converted into a valid grammar.
 */
public class GrammarException extends RuntimeException {
    public GrammarException(String message) {
        super(message);
    }

    public GrammarException(String message, Throwable cause) {
        super(message, cause);
    }
}
