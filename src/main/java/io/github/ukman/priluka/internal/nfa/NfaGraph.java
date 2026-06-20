package io.github.ukman.priluka.internal.nfa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NfaGraph {
    private final NfaState start;
    private final NfaState accept;
    private final List<NfaState> states;
    private final List<NfaTransition> transitions;

    NfaGraph(
        NfaState start,
        NfaState accept,
        List<NfaState> states,
        List<NfaTransition> transitions
    ) {
        this.start = start;
        this.accept = accept;
        this.states = Collections.unmodifiableList(new ArrayList<NfaState>(states));
        this.transitions = Collections.unmodifiableList(new ArrayList<NfaTransition>(transitions));
    }

    public NfaState getStart() {
        return start;
    }

    public NfaState getAccept() {
        return accept;
    }

    public List<NfaState> getStates() {
        return states;
    }

    public List<NfaTransition> getTransitions() {
        return transitions;
    }
}
