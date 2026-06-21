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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class DfaFindRecognizer {
    private final NfaGraph graph;
    private final Lexer lexer;
    private final NfaRecognizer traceRecognizer;
    private final List<List<NfaTransition>> outgoing;
    private final List<TerminalKey> alphabet;
    private final Map<TerminalKey, Integer> alphabetIds = new LinkedHashMap<TerminalKey, Integer>();
    private final List<DfaState> states = new ArrayList<DfaState>();
    private final Map<BitSetKey, Integer> stateIds = new LinkedHashMap<BitSetKey, Integer>();
    private final List<int[]> transitions = new ArrayList<int[]>();
    private final Map<TerminalKey, Integer> runtimeAlphabetIds = new LinkedHashMap<TerminalKey, Integer>();
    private final Map<List<TerminalSymbol>, Integer> runtimeTerminalListIds =
        new LinkedHashMap<List<TerminalSymbol>, Integer>();

    public DfaFindRecognizer(NfaGraph graph, Lexer lexer) {
        this(graph, lexer, terminalSymbols(graph));
    }

    public DfaFindRecognizer(NfaGraph graph, Lexer lexer, List<TerminalSymbol> terminals) {
        this.graph = graph;
        this.lexer = lexer;
        this.traceRecognizer = new NfaRecognizer(graph, lexer);
        this.outgoing = outgoing(graph);
        this.alphabet = alphabet(terminals, terminalTypes(graph));
        for (int i = 0; i < alphabet.size(); i++) {
            alphabetIds.put(alphabet.get(i), Integer.valueOf(i));
        }
        compile();
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

    private void compile() {
        state(closure(singleton(graph.getStart().getId())));
        for (int stateId = 0; stateId < states.size(); stateId++) {
            int[] row = transitions.get(stateId);
            DfaState state = states.get(stateId);
            for (int i = 0; i < alphabet.size(); i++) {
                BitSet move = move(state.nfaStates, alphabet.get(i));
                if (!move.isEmpty()) {
                    int target = state(closure(move));
                    row[i] = target;
                }
            }
        }
    }

    private FindSpan findSpan(List<Lexeme> lexemes, int startTokenIndex) {
        List<ActiveState> active = new ArrayList<ActiveState>();
        FindSpan best = null;
        for (int i = startTokenIndex; i < lexemes.size(); i++) {
            Lexeme lexeme = lexemes.get(i);
            int terminalId = terminalId(lexeme);
            if (terminalId < 0) {
                active.clear();
                if (best != null) {
                    return best;
                }
                continue;
            }

            int startTransition = best == null ? transition(0, terminalId) : -1;
            if (best == null && active.isEmpty() && startTransition < 0) {
                continue;
            }

            if (best == null && startTransition >= 0) {
                active.add(new ActiveState(0, lexeme.getStart(), i));
            }

            List<ActiveState> next = new ArrayList<ActiveState>();
            for (int j = 0; j < active.size(); j++) {
                ActiveState current = active.get(j);
                int target = transition(current.stateId, terminalId);
                if (target >= 0) {
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

    private int transition(int stateId, int terminalId) {
        return transitions.get(stateId)[terminalId];
    }

    private int terminalId(Lexeme lexeme) {
        List<TerminalSymbol> terminals = lexeme.getTerminalTypes();
        Integer cachedList = runtimeTerminalListIds.get(terminals);
        if (cachedList != null) {
            return cachedList.intValue();
        }
        TerminalKey key = terminalKey(terminals);
        Integer cached = runtimeAlphabetIds.get(key);
        if (cached != null) {
            int value = cached.intValue();
            runtimeTerminalListIds.put(terminals, Integer.valueOf(value));
            return value;
        }
        Integer id = alphabetIds.get(key);
        int value = id == null ? -1 : id.intValue();
        runtimeAlphabetIds.put(key, Integer.valueOf(value));
        runtimeTerminalListIds.put(terminals, Integer.valueOf(value));
        return value;
    }

    private int state(BitSet nfaStates) {
        BitSetKey key = new BitSetKey(nfaStates);
        Integer existing = stateIds.get(key);
        if (existing != null) {
            return existing.intValue();
        }
        int id = states.size();
        states.add(new DfaState(id, nfaStates, nfaStates.get(graph.getAccept().getId())));
        stateIds.put(key, Integer.valueOf(id));
        transitions.add(emptyRow(alphabet.size()));
        return id;
    }

    private BitSet move(BitSet nfaStates, TerminalKey terminalKey) {
        BitSet result = new BitSet(graph.getStates().size());
        for (int nfaState = nfaStates.nextSetBit(0); nfaState >= 0; nfaState = nfaStates.nextSetBit(nfaState + 1)) {
            List<NfaTransition> stateTransitions = outgoing.get(nfaState);
            for (int i = 0; i < stateTransitions.size(); i++) {
                NfaTransition transition = stateTransitions.get(i);
                if (
                    transition.getKind() == NfaTransition.Kind.TERMINAL
                        && terminalKey.contains(transition.getSymbolType())
                ) {
                    result.set(transition.getTo().getId());
                }
            }
        }
        return result;
    }

    private BitSet closure(BitSet seed) {
        BitSet result = (BitSet) seed.clone();
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        for (int state = result.nextSetBit(0); state >= 0; state = result.nextSetBit(state + 1)) {
            queue.addLast(Integer.valueOf(state));
        }
        while (!queue.isEmpty()) {
            int state = queue.removeFirst().intValue();
            List<NfaTransition> stateTransitions = outgoing.get(state);
            for (int i = 0; i < stateTransitions.size(); i++) {
                NfaTransition transition = stateTransitions.get(i);
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

    private static int[] emptyRow(int size) {
        int[] row = new int[size];
        for (int i = 0; i < row.length; i++) {
            row[i] = -1;
        }
        return row;
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
        return terminalKey(lexeme.getTerminalTypes());
    }

    private static TerminalKey terminalKey(List<TerminalSymbol> terminals) {
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (TerminalSymbol terminal : terminals) {
            types.add(terminal.getType());
        }
        return new TerminalKey(types);
    }

    private FindSpan firstAcceptedSpan(List<ActiveState> active) {
        for (int i = 0; i < active.size(); i++) {
            ActiveState state = active.get(i);
            if (states.get(state.stateId).accepting && state.end >= state.start) {
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

    private static List<TerminalKey> alphabet(List<TerminalSymbol> terminals, Set<Class<?>> usedTypes) {
        List<TerminalSymbol> wordCarriers = carriers(terminals, "abc");
        List<TerminalSymbol> numberCarriers = carriers(terminals, "123");
        List<TerminalSymbol> symbolCarriers = carriers(terminals, "@");
        Map<String, List<TerminalSymbol>> keywordGroups = new LinkedHashMap<String, List<TerminalSymbol>>();
        for (TerminalSymbol terminal : terminals) {
            if (!usedTypes.contains(terminal.getType()) || terminal.getKeywordTexts().isEmpty()) {
                continue;
            }
            for (String text : terminal.getKeywordTexts()) {
                String key = terminal.isCaseSensitive() ? text : text.toLowerCase(Locale.ROOT);
                List<TerminalSymbol> group = keywordGroups.get(key);
                if (group == null) {
                    group = new ArrayList<TerminalSymbol>();
                    keywordGroups.put(key, group);
                }
                group.add(terminal);
            }
        }

        Set<TerminalKey> result = new LinkedHashSet<TerminalKey>();
        addCarrierKey(result, wordCarriers);
        addCarrierKey(result, numberCarriers);
        addCarrierKey(result, symbolCarriers);
        for (Map.Entry<String, List<TerminalSymbol>> entry : keywordGroups.entrySet()) {
            List<TerminalSymbol> group = new ArrayList<TerminalSymbol>();
            String text = entry.getKey();
            if (isAsciiWord(text)) {
                group.addAll(wordCarriers);
            } else if (isAsciiNumber(text)) {
                group.addAll(numberCarriers);
            } else {
                group.addAll(symbolCarriers);
            }
            group.addAll(entry.getValue());
            result.add(new TerminalKey(types(group)));
        }
        for (TerminalSymbol terminal : terminals) {
            if (usedTypes.contains(terminal.getType())) {
                List<Class<?>> singleton = new ArrayList<Class<?>>();
                singleton.add(terminal.getType());
                result.add(new TerminalKey(singleton));
            }
        }
        return new ArrayList<TerminalKey>(result);
    }

    private static void addCarrierKey(Set<TerminalKey> target, List<TerminalSymbol> carriers) {
        if (!carriers.isEmpty()) {
            target.add(new TerminalKey(types(carriers)));
        }
    }

    private static List<TerminalSymbol> carriers(List<TerminalSymbol> terminals, String sample) {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>();
        for (TerminalSymbol terminal : terminals) {
            if (
                terminal.getKind() == TerminalSymbol.Kind.REGEXP
                    && matches(terminal, sample)
            ) {
                result.add(terminal);
            }
        }
        return result;
    }

    private static boolean matches(TerminalSymbol terminal, String sample) {
        return Pattern.compile(terminal.getPattern()).matcher(sample).matches();
    }

    private static boolean isAsciiWord(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiNumber(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    private static List<Class<?>> types(List<TerminalSymbol> terminals) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        for (int i = 0; i < terminals.size(); i++) {
            result.add(terminals.get(i).getType());
        }
        return result;
    }

    private static Set<Class<?>> terminalTypes(NfaGraph graph) {
        Set<Class<?>> result = new LinkedHashSet<Class<?>>();
        for (NfaTransition transition : graph.getTransitions()) {
            if (transition.getKind() == NfaTransition.Kind.TERMINAL) {
                result.add(transition.getSymbolType());
            }
        }
        return result;
    }

    private static List<TerminalSymbol> terminalSymbols(NfaGraph graph) {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>();
        for (Class<?> type : terminalTypes(graph)) {
            result.add(new TerminalSymbol(type, TerminalSymbol.Kind.REGEXP, "\\Q" + type.getName() + "\\E", false, 0));
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
        private final int stateId;
        private final int start;
        private final int end;
        private final int startTokenIndex;
        private final int endTokenIndex;

        private ActiveState(int stateId, int start, int startTokenIndex) {
            this(stateId, start, start, startTokenIndex, startTokenIndex);
        }

        private ActiveState(int stateId, int start, int end, int startTokenIndex, int endTokenIndex) {
            this.stateId = stateId;
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
            List<Class<?>> sorted = new ArrayList<Class<?>>(types);
            Collections.sort(sorted, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> left, Class<?> right) {
                    return left.getName().compareTo(right.getName());
                }
            });
            this.types = Collections.unmodifiableList(sorted);
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
}
