package io.github.ukman.priluka.internal.nfa;

public final class NfaState {
    private final int id;

    NfaState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "q" + id;
    }
}
