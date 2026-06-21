package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DfaFindRecognizer {
    private final NfaGraph graph;
    private final Lexer lexer;
    private final NfaRecognizer traceRecognizer;
    private final List<List<NfaTransition>> outgoing;
    private final Map<BitSetKey, DfaState> states = new LinkedHashMap<BitSetKey, DfaState>();
    private final Map<TransitionKey, DfaState> transitionCache = new LinkedHashMap<TransitionKey, DfaState>();
    private final DfaState startState;

    public DfaFindRecognizer(NfaGraph graph, Lexer lexer) {
        this.graph = graph;
        this.lexer = lexer;
        this.traceRecognizer = new NfaRecognizer(graph, lexer);
        this.outgoing = outgoing(graph);
        this.startState = state(closure(singleton(graph.getStart().getId())));
    }

    public List<NfaFindResult> findAll(String input) {
        try {
            return findAll(lexer.tokenize(input));
        } catch (LexerException e) {
            return new ArrayList<NfaFindResult>();
        }
    }

    public List<NfaFindResult> findAll(List<Lexeme> lexemes) {
        List<NfaFindResult> results = new ArrayList<NfaFindResult>();
        int tokenIndex = 0;
        while (tokenIndex < lexemes.size()) {
            FindSpan span = findSpan(lexemes, tokenIndex);
            if (span == null) {
                break;
            }
            ParseTrace trace = traceRecognizer.parseTrace(lexemes.subList(span.startTokenIndex, span.endTokenIndex));
            results.add(new NfaFindResult(span.start, span.end, trace));
            int nextTokenIndex = span.endTokenIndex;
            tokenIndex = nextTokenIndex > tokenIndex ? nextTokenIndex : tokenIndex + 1;
        }
        return results;
    }

    public List<NfaFindSpan> findSpans(String input) {
        try {
            return findSpans(lexer.tokenize(input));
        } catch (LexerException e) {
            return new ArrayList<NfaFindSpan>();
        }
    }

    public List<NfaFindSpan> findSpans(List<Lexeme> lexemes) {
        List<NfaFindSpan> results = new ArrayList<NfaFindSpan>();
        int tokenIndex = 0;
        while (tokenIndex < lexemes.size()) {
            FindSpan span = findSpan(lexemes, tokenIndex);
            if (span == null) {
                break;
            }
            results.add(new NfaFindSpan(span.start, span.end));
            int nextTokenIndex = span.endTokenIndex;
            tokenIndex = nextTokenIndex > tokenIndex ? nextTokenIndex : tokenIndex + 1;
        }
        return results;
    }

    private FindSpan findSpan(List<Lexeme> lexemes, int startTokenIndex) {
        List<ActiveState> active = new ArrayList<ActiveState>();
        FindSpan best = null;
        for (int i = startTokenIndex; i < lexemes.size(); i++) {
            Lexeme lexeme = lexemes.get(i);
            TerminalKey terminalKey = terminalKey(lexeme);
            if (best == null && transition(startState, terminalKey) != null) {
                active.add(new ActiveState(startState, lexeme.getStart(), i));
            }

            List<ActiveState> next = new ArrayList<ActiveState>();
            for (int j = 0; j < active.size(); j++) {
                ActiveState current = active.get(j);
                DfaState target = transition(current.state, terminalKey);
                if (target != null) {
                    next.add(new ActiveState(
                        target,
                        current.start,
                        lexeme.getStart() + lexeme.getLen(),
                        current.startTokenIndex,
                        i + 1
                    ));
                }
            }
            active = next;

            FindSpan accepted = firstAcceptedSpan(active);
            if (accepted != null) {
                best = betterFindSpan(best, accepted);
            }
            if (best != null) {
                active = activeStartingAtOrBefore(active, best.start);
                if (active.isEmpty()) {
                    return best;
                }
            }
        }
        return best;
    }

    private DfaState transition(DfaState state, TerminalKey terminalKey) {
        TransitionKey key = new TransitionKey(state.id, terminalKey);
        DfaState cached = transitionCache.get(key);
        if (cached != null || transitionCache.containsKey(key)) {
            return cached;
        }

        BitSet move = new BitSet(graph.getStates().size());
        for (int nfaState = state.nfaStates.nextSetBit(0); nfaState >= 0; nfaState = state.nfaStates.nextSetBit(nfaState + 1)) {
            List<NfaTransition> transitions = outgoing.get(nfaState);
            for (int i = 0; i < transitions.size(); i++) {
                NfaTransition transition = transitions.get(i);
                if (
                    transition.getKind() == NfaTransition.Kind.TERMINAL
                        && terminalKey.contains(transition.getSymbolType())
                ) {
                    move.set(transition.getTo().getId());
                }
            }
        }
        DfaState target = move.isEmpty() ? null : state(closure(move));
        transitionCache.put(key, target);
        return target;
    }

    private DfaState state(BitSet nfaStates) {
        BitSetKey key = new BitSetKey(nfaStates);
        DfaState existing = states.get(key);
        if (existing != null) {
            return existing;
        }
        DfaState created = new DfaState(states.size(), nfaStates, nfaStates.get(graph.getAccept().getId()));
        states.put(key, created);
        return created;
    }

    private BitSet closure(BitSet seed) {
        BitSet result = (BitSet) seed.clone();
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        for (int state = result.nextSetBit(0); state >= 0; state = result.nextSetBit(state + 1)) {
            queue.addLast(Integer.valueOf(state));
        }
        while (!queue.isEmpty()) {
            int state = queue.removeFirst().intValue();
            List<NfaTransition> transitions = outgoing.get(state);
            for (int i = 0; i < transitions.size(); i++) {
                NfaTransition transition = transitions.get(i);
                if (transition.getKind() != NfaTransition.Kind.TERMINAL) {
                    int to = transition.getTo().getId();
                    if (!result.get(to)) {
                        result.set(to);
                        queue.addLast(Integer.valueOf(to));
                    }
                }
            }
        }
        return result;
    }

    private static List<List<NfaTransition>> outgoing(NfaGraph graph) {
        List<List<NfaTransition>> result = new ArrayList<List<NfaTransition>>();
        for (int i = 0; i < graph.getStates().size(); i++) {
            result.add(new ArrayList<NfaTransition>());
        }
        for (NfaTransition transition : graph.getTransitions()) {
            result.get(transition.getFrom().getId()).add(transition);
        }
        return result;
    }

    private static BitSet singleton(int state) {
        BitSet result = new BitSet();
        result.set(state);
        return result;
    }

    private static TerminalKey terminalKey(Lexeme lexeme) {
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (TerminalSymbol terminal : lexeme.getTerminalTypes()) {
            types.add(terminal.getType());
        }
        Collections.sort(types, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> left, Class<?> right) {
                return left.getName().compareTo(right.getName());
            }
        });
        return new TerminalKey(types);
    }

    private static FindSpan firstAcceptedSpan(List<ActiveState> active) {
        for (int i = 0; i < active.size(); i++) {
            ActiveState state = active.get(i);
            if (state.state.accepting && state.end >= state.start) {
                return new FindSpan(state.start, state.end, state.startTokenIndex, state.endTokenIndex);
            }
        }
        return null;
    }

    private static FindSpan betterFindSpan(FindSpan current, FindSpan candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate.start < current.start) {
            return candidate;
        }
        if (candidate.start == current.start && candidate.end > current.end) {
            return candidate;
        }
        return current;
    }

    private static List<ActiveState> activeStartingAtOrBefore(List<ActiveState> active, int start) {
        List<ActiveState> result = new ArrayList<ActiveState>();
        for (int i = 0; i < active.size(); i++) {
            ActiveState state = active.get(i);
            if (state.start <= start) {
                result.add(state);
            }
        }
        return result;
    }

    private static final class DfaState {
        private final int id;
        private final BitSet nfaStates;
        private final boolean accepting;

        private DfaState(int id, BitSet nfaStates, boolean accepting) {
            this.id = id;
            this.nfaStates = (BitSet) nfaStates.clone();
            this.accepting = accepting;
        }
    }

    private static final class ActiveState {
        private final DfaState state;
        private final int start;
        private final int end;
        private final int startTokenIndex;
        private final int endTokenIndex;

        private ActiveState(DfaState state, int start, int startTokenIndex) {
            this(state, start, start, startTokenIndex, startTokenIndex);
        }

        private ActiveState(DfaState state, int start, int end, int startTokenIndex, int endTokenIndex) {
            this.state = state;
            this.start = start;
            this.end = end;
            this.startTokenIndex = startTokenIndex;
            this.endTokenIndex = endTokenIndex;
        }
    }

    private static final class FindSpan {
        private final int start;
        private final int end;
        private final int startTokenIndex;
        private final int endTokenIndex;

        private FindSpan(int start, int end, int startTokenIndex, int endTokenIndex) {
            this.start = start;
            this.end = end;
            this.startTokenIndex = startTokenIndex;
            this.endTokenIndex = endTokenIndex;
        }
    }

    private static final class BitSetKey {
        private final BitSet bits;

        private BitSetKey(BitSet bits) {
            this.bits = (BitSet) bits.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof BitSetKey && bits.equals(((BitSetKey) other).bits);
        }

        @Override
        public int hashCode() {
            return bits.hashCode();
        }
    }

    private static final class TerminalKey {
        private final List<Class<?>> types;

        private TerminalKey(List<Class<?>> types) {
            this.types = Collections.unmodifiableList(new ArrayList<Class<?>>(types));
        }

        private boolean contains(Class<?> type) {
            return types.contains(type);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TerminalKey && types.equals(((TerminalKey) other).types);
        }

        @Override
        public int hashCode() {
            return types.hashCode();
        }
    }

    private static final class TransitionKey {
        private final int stateId;
        private final TerminalKey terminalKey;

        private TransitionKey(int stateId, TerminalKey terminalKey) {
            this.stateId = stateId;
            this.terminalKey = terminalKey;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof TransitionKey)) {
                return false;
            }
            TransitionKey that = (TransitionKey) other;
            return stateId == that.stateId && terminalKey.equals(that.terminalKey);
        }

        @Override
        public int hashCode() {
            return 31 * stateId + terminalKey.hashCode();
        }
    }
}
