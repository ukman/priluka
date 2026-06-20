package io.github.ukman.priluka;

public final class ParseFindResult<S> {
    private final S value;
    private final ParseTrace trace;
    private final int start;
    private final int end;

    public ParseFindResult(S value, ParseTrace trace, int start, int end) {
        this.value = value;
        this.trace = trace;
        this.start = start;
        this.end = end;
    }

    public S getValue() {
        return value;
    }

    public ParseTrace getTrace() {
        return trace;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getLen() {
        return end - start;
    }
}
