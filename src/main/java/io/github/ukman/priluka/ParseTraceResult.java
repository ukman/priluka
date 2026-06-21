package io.github.ukman.priluka;

/**
 * Result of parsing a complete input together with the trace used to build it.
 *
 * @param <S> parsed value type for the requested start symbol
 */
public final class ParseTraceResult<S> {
    private final S value;
    private final ParseTrace trace;

    public ParseTraceResult(S value, ParseTrace trace) {
        this.value = value;
        this.trace = trace;
    }

    public S getValue() {
        return value;
    }

    public ParseTrace getTrace() {
        return trace;
    }
}
