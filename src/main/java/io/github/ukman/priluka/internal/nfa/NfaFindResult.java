package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;

public final class NfaFindResult {
    private final int start;
    private final int end;
    private final ParseTrace trace;

    NfaFindResult(int start, int end, ParseTrace trace) {
        this.start = start;
        this.end = end;
        this.trace = trace;
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

    public ParseTrace getTrace() {
        return trace;
    }
}
