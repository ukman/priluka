package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.ParseException;
import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.ParseTraceEvent;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReflectiveParser implements ParseEngine {
    private final GrammarModel model;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Map<Class<?>, TerminalSymbol> terminals = new LinkedHashMap<Class<?>, TerminalSymbol>();
    private final GrammarPredictor predictor;
    private final LexerConfig lexerConfig;
    private final boolean debug;
    private final ParseDebugStats debugStats = new ParseDebugStats();

    public ReflectiveParser(GrammarModel model) {
        this(model, LexerConfig.DEFAULT);
    }

    public ReflectiveParser(GrammarModel model, LexerConfig lexerConfig) {
        this.model = model;
        this.lexerConfig = lexerConfig;
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            nonterminals.put(nonterminal.getType(), nonterminal);
        }
        for (TerminalSymbol terminal : model.getTerminals()) {
            terminals.put(terminal.getType(), terminal);
        }
        this.predictor = new GrammarPredictor(model);
        this.debug = Boolean.getBoolean("priluka.parser.debug");
    }

    @Override
    public ParseTrace parseTrace(Class<?> start, String input) {
        if (debug) {
            debugStats.reset();
        }
        Lexer lexer = lexerConfig.createLexer(model.getTerminals());
        List<Lexeme> lexemes = lexer.tokenize(input);
        ParseSearch search = parseStart(start, lexemes);
        if (search.full != null) {
            if (debug) {
                debugStats.print(start, input.length(), lexemes.size());
            }
            return new ParseTrace(search.full.events);
        }

        if (search.partial != null && search.partial.position < lexemes.size()) {
            if (debug) {
                debugStats.print(start, input.length(), lexemes.size());
            }
            Lexeme unexpected = lexemes.get(search.partial.position);
            throw new ParseException("Unexpected token at offset " + unexpected.getStart() + ": " + unexpected.getText());
        }
        if (debug) {
            debugStats.print(start, input.length(), lexemes.size());
        }
        throw new ParseException("Input does not match start symbol: " + start.getName());
    }

    private ParseSearch parseStart(Class<?> type, List<Lexeme> lexemes) {
        final ParseSearch search = new ParseSearch(lexemes.size());
        Deque<ParseFrame> frames = new ArrayDeque<ParseFrame>();
        frames.push(new NonterminalFrame(type, lexemes, 0, new ResultSink() {
            @Override
            public void accept(ParseResult result) {
                search.accept(result);
            }

            @Override
            public boolean isDone() {
                return search.full != null;
            }

            @Override
            public int preferredPosition() {
                return search.endPosition;
            }
        }, 1));
        runFrames(frames, search);
        return search;
    }

    private List<ParseResult> parseNonterminal(Class<?> type, List<Lexeme> lexemes, int position) {
        final List<ParseResult> results = new ArrayList<ParseResult>();
        Deque<ParseFrame> frames = new ArrayDeque<ParseFrame>();
        frames.push(new NonterminalFrame(type, lexemes, position, new ResultSink() {
            @Override
            public void accept(ParseResult result) {
                results.add(result);
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public int preferredPosition() {
                return -1;
            }
        }, 1));
        runFrames(frames, null);
        return results;
    }

    private void runFrames(Deque<ParseFrame> frames, ParseSearch search) {
        while (!frames.isEmpty() && (search == null || search.full == null)) {
            frames.pop().run(frames);
        }
    }

    private final class NonterminalFrame implements ParseFrame {
        private final Class<?> type;
        private final List<Lexeme> lexemes;
        private final int position;
        private final ResultSink sink;
        private final int depth;

        private NonterminalFrame(Class<?> type, List<Lexeme> lexemes, int position, ResultSink sink, int depth) {
            this.type = type;
            this.lexemes = lexemes;
            this.position = position;
            this.sink = sink;
            this.depth = depth;
        }

        @Override
        public void run(Deque<ParseFrame> frames) {
            if (debug) {
                debugStats.nonterminalCalls++;
                if (depth > debugStats.maxParseDepth) {
                    debugStats.maxParseDepth = depth;
                }
            }

            NonterminalSymbol nonterminal = nonterminals.get(type);
            if (nonterminal == null) {
                return;
            }

            List<Production> productions = predictor.predict(nonterminal, lexemes, position);
            if (debug) {
                debugStats.predictionAttempts++;
                debugStats.predictionCandidates += productions.size();
                debugStats.predictionRejected += nonterminal.getProductions().size() - productions.size();
            }
            for (int i = productions.size() - 1; i >= 0; i--) {
                List<ParseState> states = new ArrayList<ParseState>();
                states.add(new ParseState(position, new ArrayList<ParseTraceEvent>()));
                frames.push(new ProductionFrame(productions.get(i), lexemes, 0, states, sink, depth));
            }
        }
    }

    private final class ProductionFrame implements ParseFrame {
        private final Production production;
        private final List<Lexeme> lexemes;
        private final int partIndex;
        private final List<ParseState> states;
        private final ResultSink sink;
        private final int depth;

        private ProductionFrame(
            Production production,
            List<Lexeme> lexemes,
            int partIndex,
            List<ParseState> states,
            ResultSink sink,
            int depth
        ) {
            this.production = production;
            this.lexemes = lexemes;
            this.partIndex = partIndex;
            this.states = states;
            this.sink = sink;
            this.depth = depth;
        }

        @Override
        public void run(Deque<ParseFrame> frames) {
            if (partIndex == 0 && debug) {
                debugStats.productionAttempts++;
            }

            if (partIndex == production.getParts().size()) {
                int emitted = emitProductionResults(production, states, sink);
                if (debug) {
                    debugStats.productionMatches++;
                    debugStats.parseResultsProduced += emitted;
                }
                return;
            }

            ProductionPart part = production.getParts().get(partIndex);
            List<ParseState> nextStates = new ArrayList<ParseState>();
            frames.push(new AfterProductionPartFrame(production, lexemes, partIndex, nextStates, sink, depth));
            if (part.getQuantifier() != ProductionPart.Quantifier.ONE) {
                for (int i = 0; i < states.size(); i++) {
                    ParseState state = states.get(i);
                    List<ParseResult> partResults = parseVariablePart(part, lexemes, state.position);
                    for (ParseResult partResult : partResults) {
                        addNextState(nextStates, state, partResult);
                    }
                }
                return;
            }

            Class<?> symbolType = part.getSymbolType();
            if (terminals.containsKey(symbolType)) {
                for (int i = 0; i < states.size(); i++) {
                    ParseState state = states.get(i);
                    List<ParseResult> partResults = parseTerminal(symbolType, lexemes, state.position);
                    for (ParseResult partResult : partResults) {
                        addNextState(nextStates, state, partResult);
                    }
                }
                return;
            }

            for (int i = states.size() - 1; i >= 0; i--) {
                final ParseState state = states.get(i);
                frames.push(new NonterminalFrame(symbolType, lexemes, state.position, new ResultSink() {
                    @Override
                    public void accept(ParseResult partResult) {
                        addNextState(nextStates, state, partResult);
                    }

                    @Override
                    public boolean isDone() {
                        return false;
                    }

                    @Override
                    public int preferredPosition() {
                        return -1;
                    }
                }, depth + 1));
            }
        }

        private void addNextState(List<ParseState> nextStates, ParseState state, ParseResult partResult) {
            List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>(state.events);
            events.addAll(partResult.events);
            nextStates.add(new ParseState(partResult.position, events));
        }
    }

    private List<ParseResult> parseVariablePart(
        ProductionPart part,
        List<Lexeme> lexemes,
        int position
    ) {
        if (part.getQuantifier() == ProductionPart.Quantifier.OPTIONAL) {
            return parseOptionalPart(part, lexemes, position);
        }
        return parseRepeatedPart(part, lexemes, position);
    }

    private List<ParseResult> parseOptionalPart(
        ProductionPart part,
        List<Lexeme> lexemes,
        int position
    ) {
        List<ParseResult> results = new ArrayList<ParseResult>();
        List<ParseTraceEvent> absentEvents = new ArrayList<ParseTraceEvent>();
        absentEvents.add(ParseTraceEvent.beginOptional(part.getSymbolName()));
        absentEvents.add(ParseTraceEvent.endOptional(part.getSymbolName(), false));
        results.add(new ParseResult(position, absentEvents));
        List<ParseResult> presentResults = parseSymbol(part.getSymbolType(), lexemes, position);
        for (ParseResult presentResult : presentResults) {
            if (presentResult.position == position) {
                continue;
            }
            List<ParseTraceEvent> presentEvents = new ArrayList<ParseTraceEvent>();
            presentEvents.add(ParseTraceEvent.beginOptional(part.getSymbolName()));
            presentEvents.addAll(presentResult.events);
            presentEvents.add(ParseTraceEvent.endOptional(part.getSymbolName(), true));
            results.add(new ParseResult(presentResult.position, presentEvents));
        }
        return results;
    }

    private List<ParseResult> parseRepeatedPart(
        ProductionPart part,
        List<Lexeme> lexemes,
        int position
    ) {
        if (part.getSeparatorType() != null) {
            return parseSeparatedRepeatedPart(part, lexemes, position);
        }
        return parsePlainRepeatedPart(part, lexemes, position);
    }

    private List<ParseResult> parsePlainRepeatedPart(
        ProductionPart part,
        List<Lexeme> lexemes,
        int position
    ) {
        List<RepeatedState> active = new ArrayList<RepeatedState>();
        active.add(new RepeatedState(position, 0, new ArrayList<ParseTraceEvent>()));

        List<ParseResult> results = new ArrayList<ParseResult>();
        if (part.getQuantifier() == ProductionPart.Quantifier.ZERO_OR_MORE) {
            List<ParseTraceEvent> emptyEvents = new ArrayList<ParseTraceEvent>();
            emptyEvents.add(ParseTraceEvent.beginRepeat(part.getSymbolName()));
            emptyEvents.add(ParseTraceEvent.endRepeat(part.getSymbolName(), 0));
            results.add(new ParseResult(position, emptyEvents));
        }

        while (!active.isEmpty()) {
            List<RepeatedState> next = new ArrayList<RepeatedState>();
            for (RepeatedState state : active) {
                List<ParseResult> itemResults = parseSymbol(part.getSymbolType(), lexemes, state.position);
                for (ParseResult itemResult : itemResults) {
                    if (itemResult.position == state.position) {
                        continue;
                    }
                    int count = state.count + 1;
                    List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>(state.events);
                    events.addAll(itemResult.events);
                    events.add(ParseTraceEvent.appendRepeatElement(part.getSymbolName()));
                    results.add(new ParseResult(itemResult.position, repeatEvents(part.getSymbolName(), events, count)));
                    next.add(new RepeatedState(itemResult.position, count, events));
                }
            }
            active = next;
        }

        return results;
    }

    private List<ParseResult> parseSeparatedRepeatedPart(
        ProductionPart part,
        List<Lexeme> lexemes,
        int position
    ) {
        List<ParseResult> results = new ArrayList<ParseResult>();
        if (part.getQuantifier() == ProductionPart.Quantifier.ZERO_OR_MORE) {
            List<ParseTraceEvent> emptyEvents = new ArrayList<ParseTraceEvent>();
            emptyEvents.add(ParseTraceEvent.beginRepeat(part.getSymbolName()));
            emptyEvents.add(ParseTraceEvent.endRepeat(part.getSymbolName(), 0));
            results.add(new ParseResult(position, emptyEvents));
        }

        List<RepeatedState> active = new ArrayList<RepeatedState>();
        List<ParseResult> firstItems = parseSymbol(part.getSymbolType(), lexemes, position);
        for (ParseResult firstItem : firstItems) {
            if (firstItem.position == position) {
                continue;
            }
            List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>();
            events.addAll(firstItem.events);
            events.add(ParseTraceEvent.appendRepeatElement(part.getSymbolName()));
            results.add(new ParseResult(firstItem.position, repeatEvents(part.getSymbolName(), events, 1)));
            active.add(new RepeatedState(firstItem.position, 1, events));
        }

        while (!active.isEmpty()) {
            List<RepeatedState> next = new ArrayList<RepeatedState>();
            for (RepeatedState state : active) {
                List<ParseResult> separators = parseSymbol(part.getSeparatorType(), lexemes, state.position);
                for (ParseResult separator : separators) {
                    if (separator.position == state.position) {
                        continue;
                    }
                    List<ParseResult> itemResults = parseSymbol(part.getSymbolType(), lexemes, separator.position);
                    if (itemResults.isEmpty() && part.isTrailingSeparator()) {
                        List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>(state.events);
                        events.addAll(separator.events);
                        results.add(new ParseResult(
                            separator.position,
                            repeatEvents(part.getSymbolName(), events, state.count)
                        ));
                    }
                    for (ParseResult itemResult : itemResults) {
                        if (itemResult.position == separator.position) {
                            continue;
                        }
                        int count = state.count + 1;
                        List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>(state.events);
                        events.addAll(separator.events);
                        events.addAll(itemResult.events);
                        events.add(ParseTraceEvent.appendRepeatElement(part.getSymbolName()));
                        results.add(new ParseResult(itemResult.position, repeatEvents(part.getSymbolName(), events, count)));
                        next.add(new RepeatedState(itemResult.position, count, events));
                    }
                }
            }
            active = next;
        }

        return results;
    }

    private List<ParseResult> parseSymbol(Class<?> type, List<Lexeme> lexemes, int position) {
        if (terminals.containsKey(type)) {
            return parseTerminal(type, lexemes, position);
        }
        return parseNonterminal(type, lexemes, position);
    }

    private List<ParseTraceEvent> repeatEvents(String symbolName, List<ParseTraceEvent> bodyEvents, int count) {
        List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>();
        events.add(ParseTraceEvent.beginRepeat(symbolName));
        events.addAll(bodyEvents);
        events.add(ParseTraceEvent.endRepeat(symbolName, count));
        return events;
    }

    private int emitProductionResults(Production production, List<ParseState> states, ResultSink sink) {
        int preferredPosition = sink.preferredPosition();
        if (preferredPosition >= 0) {
            for (ParseState state : states) {
                if (state.position == preferredPosition) {
                    sink.accept(new ParseResult(state.position, productionEvents(production, state.events)));
                    return 1;
                }
            }
        }
        int emitted = 0;
        for (ParseState state : states) {
            sink.accept(new ParseResult(state.position, productionEvents(production, state.events)));
            emitted++;
            if (sink.isDone()) {
                break;
            }
        }
        return emitted;
    }

    private List<ParseTraceEvent> productionEvents(Production production, List<ParseTraceEvent> childEvents) {
        List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>();
        events.add(ParseTraceEvent.beginProduction(production));
        events.addAll(childEvents);
        events.add(ParseTraceEvent.endProduction(production));
        return events;
    }

    private final class AfterProductionPartFrame implements ParseFrame {
        private final Production production;
        private final List<Lexeme> lexemes;
        private final int partIndex;
        private final List<ParseState> nextStates;
        private final ResultSink sink;
        private final int depth;

        private AfterProductionPartFrame(
            Production production,
            List<Lexeme> lexemes,
            int partIndex,
            List<ParseState> nextStates,
            ResultSink sink,
            int depth
        ) {
            this.production = production;
            this.lexemes = lexemes;
            this.partIndex = partIndex;
            this.nextStates = nextStates;
            this.sink = sink;
            this.depth = depth;
        }

        @Override
        public void run(Deque<ParseFrame> frames) {
            if (nextStates.isEmpty()) {
                if (debug) {
                    debugStats.productionDeadEnds++;
                }
                return;
            }
            frames.push(new ProductionFrame(production, lexemes, partIndex + 1, nextStates, sink, depth));
        }
    }

    private interface ParseFrame {
        void run(Deque<ParseFrame> frames);
    }

    private interface ResultSink {
        void accept(ParseResult result);

        boolean isDone();

        int preferredPosition();
    }

    private List<ParseResult> parseTerminal(Class<?> type, List<Lexeme> lexemes, int position) {
        if (debug) {
            debugStats.terminalAttempts++;
        }
        List<ParseResult> results = new ArrayList<ParseResult>();
        if (position >= lexemes.size()) {
            if (debug) {
                debugStats.terminalMisses++;
            }
            return results;
        }
        Lexeme lexeme = lexemes.get(position);
        if (!lexeme.hasTerminal(type)) {
            if (debug) {
                debugStats.terminalMisses++;
            }
            return results;
        }
        if (debug) {
            debugStats.terminalMatches++;
        }
        List<ParseTraceEvent> events = new ArrayList<ParseTraceEvent>();
        events.add(ParseTraceEvent.consumeTerminal(type, lexeme.getText(), lexeme.getStart(), lexeme.getLen()));
        results.add(new ParseResult(position + 1, events));
        return results;
    }

    private static final class ParseState {
        private final int position;
        private final List<ParseTraceEvent> events;

        private ParseState(int position, List<ParseTraceEvent> events) {
            this.position = position;
            this.events = events;
        }
    }

    private static final class RepeatedState {
        private final int position;
        private final int count;
        private final List<ParseTraceEvent> events;

        private RepeatedState(int position, int count, List<ParseTraceEvent> events) {
            this.position = position;
            this.count = count;
            this.events = events;
        }
    }

    private static final class ParseResult {
        private final int position;
        private final List<ParseTraceEvent> events;

        private ParseResult(int position, List<ParseTraceEvent> events) {
            this.position = position;
            this.events = events;
        }
    }

    private final class ParseSearch {
        private final int endPosition;
        private ParseResult full;
        private ParseResult partial;

        private ParseSearch(int endPosition) {
            this.endPosition = endPosition;
        }

        private void accept(ParseResult result) {
            if (debug) {
                debugStats.topLevelResults++;
            }
            if (result.position == endPosition) {
                if (debug) {
                    debugStats.fullResults++;
                }
                if (full == null) {
                    full = result;
                }
                return;
            }

            if (debug) {
                debugStats.rejectedPartialResults++;
            }
            if (partial == null || result.position > partial.position) {
                partial = result;
            }
        }
    }

    private static final class ParseDebugStats {
        private long nonterminalCalls;
        private long productionAttempts;
        private long productionMatches;
        private long productionDeadEnds;
        private long terminalAttempts;
        private long terminalMatches;
        private long terminalMisses;
        private long parseResultsProduced;
        private long predictionAttempts;
        private long predictionCandidates;
        private long predictionRejected;
        private int topLevelResults;
        private int fullResults;
        private int rejectedPartialResults;
        private int maxParseDepth;

        private void reset() {
            nonterminalCalls = 0;
            productionAttempts = 0;
            productionMatches = 0;
            productionDeadEnds = 0;
            terminalAttempts = 0;
            terminalMatches = 0;
            terminalMisses = 0;
            parseResultsProduced = 0;
            predictionAttempts = 0;
            predictionCandidates = 0;
            predictionRejected = 0;
            topLevelResults = 0;
            fullResults = 0;
            rejectedPartialResults = 0;
            maxParseDepth = 0;
        }

        private void print(Class<?> start, int inputChars, int lexemes) {
            System.out.println(
                "parser-debug start=" + start.getSimpleName()
                    + " chars=" + inputChars
                    + " lexemes=" + lexemes
                    + " nonterminalCalls=" + nonterminalCalls
                    + " productionAttempts=" + productionAttempts
                    + " productionMatches=" + productionMatches
                    + " productionDeadEnds=" + productionDeadEnds
                    + " productionDeadEndRate=" + percent(productionDeadEnds, productionAttempts)
                    + " terminalAttempts=" + terminalAttempts
                    + " terminalMatches=" + terminalMatches
                    + " terminalMisses=" + terminalMisses
                    + " terminalMissRate=" + percent(terminalMisses, terminalAttempts)
                    + " parseResultsProduced=" + parseResultsProduced
                    + " predictionAttempts=" + predictionAttempts
                    + " predictionCandidates=" + predictionCandidates
                    + " predictionRejected=" + predictionRejected
                    + " topLevelResults=" + topLevelResults
                    + " fullResults=" + fullResults
                    + " rejectedPartialResults=" + rejectedPartialResults
                    + " partialTopLevelRate=" + percent(rejectedPartialResults, topLevelResults)
                    + " maxParseDepth=" + maxParseDepth
            );
        }

        private String percent(long numerator, long denominator) {
            if (denominator == 0) {
                return "0.00%";
            }
            return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0 / denominator);
        }
    }
}
