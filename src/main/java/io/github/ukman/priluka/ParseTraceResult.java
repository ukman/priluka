package io.github.ukman.priluka;

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
