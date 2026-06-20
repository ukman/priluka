package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.ParseTraceEvent;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerException;
import io.github.ukman.priluka.internal.lexer.LexerOptions;
import io.github.ukman.priluka.internal.lexer.LexerSpec;
import io.github.ukman.priluka.internal.lexer.Lexers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NfaRecognizer {
    private final NfaGraph graph;
    private final Lexer lexer;
    private final Map<NfaState, List<NfaTransition>> outgoing = new LinkedHashMap<NfaState, List<NfaTransition>>();
    private final Map<NfaState, List<EpsilonPath>> epsilonClosures = new LinkedHashMap<NfaState, List<EpsilonPath>>();
    private final Set<Class<?>> startTerminalTypes = new LinkedHashSet<Class<?>>();

    public NfaRecognizer(GrammarModel model) {
        this(new NfaCompiler(model).compile(), lexerFor(model));
    }

    public NfaRecognizer(GrammarModel model, Class<?>... lexerTerminalTypes) {
        this(new NfaCompiler(model).compile(), lexerFor(model, lexerTerminalTypes));
    }

    public NfaRecognizer(NfaGraph graph, Lexer lexer) {
        this.graph = graph;
        this.lexer = lexer;
        for (NfaState state : graph.getStates()) {
            outgoing.put(state, new ArrayList<NfaTransition>());
        }
        for (NfaTransition transition : graph.getTransitions()) {
            outgoing.get(transition.getFrom()).add(transition);
        }
        for (NfaState state : graph.getStates()) {
            epsilonClosures.put(state, collectEpsilonPaths(state));
        }
        startTerminalTypes.addAll(collectStartTerminalTypes());
    }

    public boolean recognizes(String input) {
        return parseTrace(input) != null;
    }

    public boolean recognizes(List<Lexeme> lexemes) {
        return parseTrace(lexemes) != null;
    }

    public ParseTrace parseTrace(String input) {
        try {
            return parseTrace(lexer.tokenize(input));
        } catch (LexerException e) {
            return null;
        }
    }

    public ParseTrace parseTrace(List<Lexeme> lexemes) {
        List<Configuration> active = epsilonClosure(singleton(new Configuration(graph.getStart())), true);
        for (int i = 0; i < lexemes.size(); i++) {
            Lexeme lexeme = lexemes.get(i);
            List<Configuration> next = new ArrayList<Configuration>();
            for (Configuration configuration : active) {
                List<NfaTransition> transitions = outgoing.get(configuration.state);
                for (NfaTransition transition : transitions) {
                    if (
                        transition.getKind() == NfaTransition.Kind.TERMINAL
                            && lexeme.hasTerminal(transition.getSymbolType())
                    ) {
                        next.add(configuration.advance(transition, lexeme, i, true));
                    }
                }
            }
            if (next.isEmpty()) {
                return null;
            }
            active = epsilonClosure(next, true);
        }
        for (Configuration configuration : active) {
            if (configuration.state.equals(graph.getAccept())) {
                return new ParseTrace(traceEvents(configuration.trace));
            }
        }
        return null;
    }

    public NfaFindResult find(String input) {
        try {
            return find(lexer.tokenize(input));
        } catch (LexerException e) {
            return null;
        }
    }

    public NfaFindResult find(List<Lexeme> lexemes) {
        return find(lexemes, 0);
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
            ParseTrace trace = parseTrace(lexemes.subList(span.startTokenIndex, span.endTokenIndex));
            NfaFindResult result = new NfaFindResult(span.start, span.end, trace);
            results.add(result);
            int nextTokenIndex = span.endTokenIndex;
            tokenIndex = nextTokenIndex > tokenIndex ? nextTokenIndex : tokenIndex + 1;
        }
        return results;
    }

    private NfaFindResult find(List<Lexeme> lexemes, int startTokenIndex) {
        FindSpan span = findSpan(lexemes, startTokenIndex);
        if (span == null) {
            return null;
        }
        ParseTrace trace = parseTrace(lexemes.subList(span.startTokenIndex, span.endTokenIndex));
        return new NfaFindResult(span.start, span.end, trace);
    }

    private FindSpan findSpan(List<Lexeme> lexemes, int startTokenIndex) {
        List<Configuration> active = new ArrayList<Configuration>();
        FindSpan best = null;
        for (int i = startTokenIndex; i < lexemes.size(); i++) {
            Lexeme lexeme = lexemes.get(i);
            if (best == null && canStartAt(lexeme)) {
                active.addAll(epsilonClosure(singleton(new Configuration(graph.getStart(), lexeme.getStart(), i)), false));
            }

            List<Configuration> next = new ArrayList<Configuration>();
            for (Configuration configuration : active) {
                List<NfaTransition> transitions = outgoing.get(configuration.state);
                for (NfaTransition transition : transitions) {
                    if (
                        transition.getKind() == NfaTransition.Kind.TERMINAL
                            && lexeme.hasTerminal(transition.getSymbolType())
                    ) {
                        next.add(configuration.advance(transition, lexeme, i, false));
                    }
                }
            }

            active = epsilonClosure(next, false);
            FindSpan accepted = firstAcceptedSpan(active);
            if (accepted != null) {
                best = betterFindSpan(best, accepted);
            }
            if (best != null) {
                active = configurationsStartingAtOrBefore(active, best.start);
                if (active.isEmpty()) {
                    return best;
                }
            }
        }
        return best;
    }

    private List<Configuration> configurationsStartingAtOrBefore(List<Configuration> active, int start) {
        List<Configuration> result = new ArrayList<Configuration>();
        for (Configuration configuration : active) {
            if (configuration.start <= start) {
                result.add(configuration);
            }
        }
        return result;
    }

    private boolean canStartAt(Lexeme lexeme) {
        for (Class<?> type : startTerminalTypes) {
            if (lexeme.hasTerminal(type)) {
                return true;
            }
        }
        return false;
    }

    private Set<Class<?>> collectStartTerminalTypes() {
        Set<Class<?>> result = new LinkedHashSet<Class<?>>();
        List<Configuration> startConfigurations = epsilonClosure(singleton(new Configuration(graph.getStart())), false);
        for (Configuration configuration : startConfigurations) {
            List<NfaTransition> transitions = outgoing.get(configuration.state);
            for (NfaTransition transition : transitions) {
                if (transition.getKind() == NfaTransition.Kind.TERMINAL) {
                    result.add(transition.getSymbolType());
                }
            }
        }
        return result;
    }

    private List<Configuration> epsilonClosure(List<Configuration> seed, boolean captureTrace) {
        List<Configuration> closed = new ArrayList<Configuration>();
        for (Configuration configuration : seed) {
            List<EpsilonPath> paths = epsilonClosures.get(configuration.state);
            for (EpsilonPath path : paths) {
                closed.add(configuration.advance(path, captureTrace));
            }
        }
        return closed;
    }

    private List<EpsilonPath> collectEpsilonPaths(NfaState start) {
        List<EpsilonPath> paths = new ArrayList<EpsilonPath>();
        Set<NfaState> closedStates = new LinkedHashSet<NfaState>();
        Deque<EpsilonPath> queue = new ArrayDeque<EpsilonPath>();
        EpsilonPath startPath = new EpsilonPath(start, new ArrayList<NfaTransition>());
        if (isObservableState(start)) {
            paths.add(startPath);
        }
        queue.addLast(startPath);
        closedStates.add(start);

        while (!queue.isEmpty()) {
            EpsilonPath path = queue.removeFirst();
            List<NfaTransition> transitions = outgoing.get(path.state);
            for (NfaTransition transition : transitions) {
                if (transition.getKind() == NfaTransition.Kind.TERMINAL) {
                    continue;
                }
                if (closedStates.add(transition.getTo())) {
                    EpsilonPath next = path.append(transition);
                    if (isObservableState(next.state)) {
                        paths.add(next);
                    }
                    queue.addLast(next);
                }
            }
        }
        return paths;
    }

    private boolean isObservableState(NfaState state) {
        if (state.equals(graph.getAccept())) {
            return true;
        }
        List<NfaTransition> transitions = outgoing.get(state);
        for (NfaTransition transition : transitions) {
            if (transition.getKind() == NfaTransition.Kind.TERMINAL) {
                return true;
            }
        }
        return false;
    }

    private List<Configuration> singleton(Configuration configuration) {
        List<Configuration> result = new ArrayList<Configuration>();
        result.add(configuration);
        return result;
    }

    private FindSpan firstAcceptedSpan(List<Configuration> active) {
        for (Configuration configuration : active) {
            if (configuration.state.equals(graph.getAccept()) && configuration.end >= configuration.start) {
                return new FindSpan(
                    configuration.start,
                    configuration.end,
                    configuration.startTokenIndex,
                    configuration.endTokenIndex
                );
            }
        }
        return null;
    }

    private FindSpan betterFindSpan(FindSpan current, FindSpan candidate) {
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

    private List<ParseTraceEvent> traceEvents(TraceNode trace) {
        List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>();
        Deque<RepeatContext> repeats = new ArrayDeque<RepeatContext>();
        List<TraceNode> nodes = traceNodes(trace);
        for (TraceNode node : nodes) {
            NfaTransition transition = node.transition;
            switch (transition.getKind()) {
                case BEGIN_PRODUCTION:
                    events.add(ParseTraceEvent.beginProduction(transition.getProduction()));
                    break;
                case END_PRODUCTION:
                    events.add(ParseTraceEvent.endProduction(transition.getProduction()));
                    break;
                case TERMINAL:
                    events.add(ParseTraceEvent.consumeTerminal(
                        transition.getSymbolType(),
                        node.lexeme.getText(),
                        node.lexeme.getStart(),
                        node.lexeme.getLen()
                    ));
                    break;
                case BEGIN_REPEAT:
                    repeats.push(new RepeatContext(transition.getPart().getSymbolName()));
                    events.add(ParseTraceEvent.beginRepeat(transition.getPart().getSymbolName()));
                    break;
                case APPEND_REPEAT_ELEMENT:
                    repeats.peek().count++;
                    events.add(ParseTraceEvent.appendRepeatElement(transition.getPart().getSymbolName()));
                    break;
                case END_REPEAT:
                    events.add(ParseTraceEvent.endRepeat(transition.getPart().getSymbolName(), repeats.pop().count));
                    break;
                case BEGIN_OPTIONAL:
                    events.add(ParseTraceEvent.beginOptional(transition.getPart().getSymbolName()));
                    break;
                case END_OPTIONAL_PRESENT:
                    events.add(ParseTraceEvent.endOptional(transition.getPart().getSymbolName(), true));
                    break;
                case END_OPTIONAL_ABSENT:
                    events.add(ParseTraceEvent.endOptional(transition.getPart().getSymbolName(), false));
                    break;
                case EPSILON:
                    break;
                default:
                    throw new IllegalStateException("Unsupported NFA transition kind: " + transition.getKind());
            }
        }
        return events;
    }

    private List<TraceNode> traceNodes(TraceNode trace) {
        Deque<TraceNode> stack = new ArrayDeque<TraceNode>();
        TraceNode current = trace;
        while (current != null) {
            stack.push(current);
            current = current.previous;
        }

        List<TraceNode> result = new ArrayList<TraceNode>(stack.size());
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }

    private static Lexer lexerFor(GrammarModel model, Class<?>... lexerTerminalTypes) {
        return Lexers.defaultLexer(
            new LexerSpec(terminalsWithImplicitWhitespace(model, lexerTerminalTypes)),
            LexerOptions.DEFAULT
        );
    }

    private static List<TerminalSymbol> terminalsWithImplicitWhitespace(GrammarModel model, Class<?>... lexerTerminalTypes) {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>(model.getTerminals());
        for (int i = 0; i < lexerTerminalTypes.length; i++) {
            addIfAbsent(result, GrammarModelBuilder.terminalSymbol(lexerTerminalTypes[i]));
        }
        result.add(new TerminalSymbol(ImplicitWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return result;
    }

    private static void addIfAbsent(List<TerminalSymbol> terminals, TerminalSymbol terminal) {
        for (TerminalSymbol existing : terminals) {
            if (existing.getType().equals(terminal.getType())) {
                return;
            }
        }
        terminals.add(terminal);
    }

    private static final class ImplicitWhitespace {
    }

    private static final class Configuration {
        private final NfaState state;
        private final TraceNode trace;
        private final int start;
        private final int end;
        private final int startTokenIndex;
        private final int endTokenIndex;

        private Configuration(NfaState state) {
            this(state, null, -1, -1, 0, 0);
        }

        private Configuration(NfaState state, int start, int startTokenIndex) {
            this(state, null, start, start, startTokenIndex, startTokenIndex);
        }

        private Configuration(
            NfaState state,
            TraceNode trace,
            int start,
            int end,
            int startTokenIndex,
            int endTokenIndex
        ) {
            this.state = state;
            this.trace = trace;
            this.start = start;
            this.end = end;
            this.startTokenIndex = startTokenIndex;
            this.endTokenIndex = endTokenIndex;
        }

        private Configuration advance(NfaTransition transition, Lexeme lexeme, int tokenIndex, boolean captureTrace) {
            int nextEnd = end;
            int nextEndTokenIndex = endTokenIndex;
            if (lexeme != null) {
                nextEnd = lexeme.getStart() + lexeme.getLen();
                nextEndTokenIndex = tokenIndex + 1;
            }
            TraceNode nextTrace = captureTrace ? new TraceNode(trace, transition, lexeme) : null;
            return new Configuration(
                transition.getTo(),
                nextTrace,
                start,
                nextEnd,
                startTokenIndex,
                nextEndTokenIndex
            );
        }

        private Configuration advance(EpsilonPath path, boolean captureTrace) {
            TraceNode nextTrace = trace;
            if (captureTrace) {
                for (NfaTransition transition : path.transitions) {
                    nextTrace = new TraceNode(nextTrace, transition, null);
                }
            }
            return new Configuration(
                path.state,
                nextTrace,
                start,
                end,
                startTokenIndex,
                endTokenIndex
            );
        }
    }

    private static final class EpsilonPath {
        private final NfaState state;
        private final List<NfaTransition> transitions;

        private EpsilonPath(NfaState state, List<NfaTransition> transitions) {
            this.state = state;
            this.transitions = transitions;
        }

        private EpsilonPath append(NfaTransition transition) {
            List<NfaTransition> next = new ArrayList<NfaTransition>(transitions.size() + 1);
            next.addAll(transitions);
            next.add(transition);
            return new EpsilonPath(transition.getTo(), next);
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

    private static final class TraceNode {
        private final TraceNode previous;
        private final NfaTransition transition;
        private final Lexeme lexeme;

        private TraceNode(TraceNode previous, NfaTransition transition, Lexeme lexeme) {
            this.previous = previous;
            this.transition = transition;
            this.lexeme = lexeme;
        }
    }

    private static final class RepeatContext {
        private final String symbolName;
        private int count;

        private RepeatContext(String symbolName) {
            this.symbolName = symbolName;
        }
    }
}
