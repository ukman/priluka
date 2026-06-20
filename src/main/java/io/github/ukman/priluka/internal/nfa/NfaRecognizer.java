package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.ParseTraceEvent;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
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

    public NfaRecognizer(GrammarModel model) {
        this(new NfaCompiler(model).compile(), lexerFor(model));
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
        List<Configuration> active = epsilonClosure(singleton(new Configuration(graph.getStart())));
        for (Lexeme lexeme : lexemes) {
            List<Configuration> next = new ArrayList<Configuration>();
            for (Configuration configuration : active) {
                List<NfaTransition> transitions = outgoing.get(configuration.state);
                for (NfaTransition transition : transitions) {
                    if (
                        transition.getKind() == NfaTransition.Kind.TERMINAL
                            && lexeme.hasTerminal(transition.getSymbolType())
                    ) {
                        next.add(configuration.advance(transition, lexeme));
                    }
                }
            }
            if (next.isEmpty()) {
                return null;
            }
            active = epsilonClosure(next);
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
            NfaFindResult result = find(lexemes, tokenIndex);
            if (result == null) {
                break;
            }
            results.add(result);
            int nextTokenIndex = firstTokenAtOrAfter(lexemes, result.getEnd());
            tokenIndex = nextTokenIndex > tokenIndex ? nextTokenIndex : tokenIndex + 1;
        }
        return results;
    }

    private NfaFindResult find(List<Lexeme> lexemes, int startTokenIndex) {
        List<Configuration> active = new ArrayList<Configuration>();
        NfaFindResult best = null;
        for (int i = startTokenIndex; i < lexemes.size(); i++) {
            Lexeme lexeme = lexemes.get(i);
            active.addAll(epsilonClosure(singleton(new Configuration(graph.getStart(), lexeme.getStart()))));

            List<Configuration> next = new ArrayList<Configuration>();
            for (Configuration configuration : active) {
                List<NfaTransition> transitions = outgoing.get(configuration.state);
                for (NfaTransition transition : transitions) {
                    if (
                        transition.getKind() == NfaTransition.Kind.TERMINAL
                            && lexeme.hasTerminal(transition.getSymbolType())
                    ) {
                        next.add(configuration.advance(transition, lexeme));
                    }
                }
            }

            active = epsilonClosure(next);
            NfaFindResult accepted = firstAccepted(active);
            if (accepted != null) {
                best = betterFindResult(best, accepted);
            }
        }
        return best;
    }

    private int firstTokenAtOrAfter(List<Lexeme> lexemes, int offset) {
        for (int i = 0; i < lexemes.size(); i++) {
            if (lexemes.get(i).getStart() >= offset) {
                return i;
            }
        }
        return lexemes.size();
    }

    private List<Configuration> epsilonClosure(List<Configuration> seed) {
        List<Configuration> closed = new ArrayList<Configuration>(seed);
        Set<NfaState> closedStates = new LinkedHashSet<NfaState>();
        Deque<Configuration> queue = new ArrayDeque<Configuration>(seed);
        for (Configuration configuration : seed) {
            closedStates.add(configuration.state);
        }
        while (!queue.isEmpty()) {
            Configuration configuration = queue.removeFirst();
            List<NfaTransition> transitions = outgoing.get(configuration.state);
            for (NfaTransition transition : transitions) {
                if (transition.getKind() == NfaTransition.Kind.TERMINAL) {
                    continue;
                }
                if (closedStates.add(transition.getTo())) {
                    Configuration next = configuration.advance(transition, null);
                    closed.add(next);
                    queue.addLast(next);
                }
            }
        }
        return closed;
    }

    private List<Configuration> singleton(Configuration configuration) {
        List<Configuration> result = new ArrayList<Configuration>();
        result.add(configuration);
        return result;
    }

    private NfaFindResult firstAccepted(List<Configuration> active) {
        for (Configuration configuration : active) {
            if (configuration.state.equals(graph.getAccept()) && configuration.end >= configuration.start) {
                return new NfaFindResult(
                    configuration.start,
                    configuration.end,
                    new ParseTrace(traceEvents(configuration.trace))
                );
            }
        }
        return null;
    }

    private NfaFindResult betterFindResult(NfaFindResult current, NfaFindResult candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate.getStart() < current.getStart()) {
            return candidate;
        }
        if (candidate.getStart() == current.getStart() && candidate.getEnd() > current.getEnd()) {
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

    private static Lexer lexerFor(GrammarModel model) {
        return Lexers.defaultLexer(new LexerSpec(terminalsWithImplicitWhitespace(model)), LexerOptions.DEFAULT);
    }

    private static List<TerminalSymbol> terminalsWithImplicitWhitespace(GrammarModel model) {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>(model.getTerminals());
        result.add(new TerminalSymbol(ImplicitWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return result;
    }

    private static final class ImplicitWhitespace {
    }

    private static final class Configuration {
        private final NfaState state;
        private final TraceNode trace;
        private final int start;
        private final int end;

        private Configuration(NfaState state) {
            this(state, null, -1, -1);
        }

        private Configuration(NfaState state, int start) {
            this(state, null, start, start);
        }

        private Configuration(NfaState state, TraceNode trace, int start, int end) {
            this.state = state;
            this.trace = trace;
            this.start = start;
            this.end = end;
        }

        private Configuration advance(NfaTransition transition, Lexeme lexeme) {
            int nextEnd = end;
            if (lexeme != null) {
                nextEnd = lexeme.getStart() + lexeme.getLen();
            }
            return new Configuration(transition.getTo(), new TraceNode(trace, transition, lexeme), start, nextEnd);
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
