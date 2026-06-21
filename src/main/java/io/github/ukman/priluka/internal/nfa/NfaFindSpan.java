package io.github.ukman.priluka.internal.nfa;

public final class NfaFindSpan {
    private final int start;
    private final int end;

    NfaFindSpan(int start, int end) {
        this.start = start;
        this.end = end;
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
