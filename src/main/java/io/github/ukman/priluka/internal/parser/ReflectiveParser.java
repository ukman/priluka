package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.ParseException;
import io.github.ukman.priluka.Token;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerOptions;
import io.github.ukman.priluka.internal.lexer.LexerSpec;
import io.github.ukman.priluka.internal.lexer.Lexers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReflectiveParser {
    private final GrammarModel model;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Map<Class<?>, TerminalSymbol> terminals = new LinkedHashMap<Class<?>, TerminalSymbol>();
    private final boolean debug;
    private final ParseDebugStats debugStats = new ParseDebugStats();

    public ReflectiveParser(GrammarModel model) {
        this.model = model;
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            nonterminals.put(nonterminal.getType(), nonterminal);
        }
        for (TerminalSymbol terminal : model.getTerminals()) {
            terminals.put(terminal.getType(), terminal);
        }
        this.debug = Boolean.getBoolean("priluka.parser.debug");
    }

    public <S> S parse(Class<S> start, String input) {
        if (debug) {
            debugStats.reset();
        }
        Lexer lexer = Lexers.defaultLexer(new LexerSpec(terminalsWithImplicitWhitespace()), LexerOptions.DEFAULT);
        List<Lexeme> lexemes = lexer.tokenize(input);
        ParseSearch search = parseStart(start, lexemes);
        if (search.full != null) {
            if (debug) {
                debugStats.print(start, input.length(), lexemes.size());
            }
            return start.cast(search.full.value);
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

            List<Production> productions = nonterminal.getProductions();
            for (int i = productions.size() - 1; i >= 0; i--) {
                List<ParseState> states = new ArrayList<ParseState>();
                states.add(new ParseState(position, new ArrayList<Object>()));
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
            if (part.getQuantifier() != ProductionPart.Quantifier.ONE) {
                throw new ParseException("Parser v1 supports only single production parts: " + part.toBnf());
            }

            List<ParseState> nextStates = new ArrayList<ParseState>();
            frames.push(new AfterProductionPartFrame(production, lexemes, partIndex, nextStates, sink, depth));
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
            List<Object> values = new ArrayList<Object>(state.values);
            values.add(partResult.value);
            nextStates.add(new ParseState(partResult.position, values));
        }
    }

    private int emitProductionResults(Production production, List<ParseState> states, ResultSink sink) {
        int preferredPosition = sink.preferredPosition();
        if (preferredPosition >= 0) {
            for (ParseState state : states) {
                if (state.position == preferredPosition) {
                    sink.accept(new ParseResult(instantiateProduction(production, state.values), state.position));
                    return 1;
                }
            }
        }
        int emitted = 0;
        for (ParseState state : states) {
            sink.accept(new ParseResult(instantiateProduction(production, state.values), state.position));
            emitted++;
            if (sink.isDone()) {
                break;
            }
        }
        return emitted;
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
        results.add(new ParseResult(terminalValue(type, lexeme), position + 1));
        return results;
    }

    private Object instantiateProduction(Production production, List<Object> values) {
        Constructor<?> constructor = production.getConstructor();
        if (constructor == null) {
            if (values.size() != 1) {
                throw new ParseException("Interface production must produce exactly one value: " + production.toBnf());
            }
            return values.get(0);
        }
        return instantiate(constructor, values.toArray());
    }

    private Object terminalValue(Class<?> type, Lexeme lexeme) {
        String text = lexeme.getText();
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return Integer.valueOf(text);
        }
        if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return Double.valueOf(text);
        }
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return Boolean.valueOf(text);
        }
        return instantiateTerminal(type, lexeme);
    }

    private Object instantiateTerminal(Class<?> type, Lexeme lexeme) {
        try {
            Constructor<?> textConstructor = findConstructor(type, String.class);
            if (textConstructor != null) {
                return instantiate(textConstructor, new Object[] {lexeme.getText()});
            }

            Constructor<?> emptyConstructor = findConstructor(type);
            if (emptyConstructor == null) {
                throw new ParseException("Terminal has no supported constructor: " + type.getName());
            }
            Object value = instantiate(emptyConstructor, new Object[0]);
            if (value instanceof Token) {
                fillToken((Token) value, lexeme);
            }
            return value;
        } catch (SecurityException e) {
            throw new ParseException("Cannot instantiate terminal: " + type.getName(), e);
        }
    }

    private void fillToken(Token token, Lexeme lexeme) {
        token.setStart(lexeme.getStart());
        token.setLen(lexeme.getLen());
        token.setText(lexeme.getText());
    }

    private Object instantiate(Constructor<?> constructor, Object[] args) {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InstantiationException e) {
            throw new ParseException("Cannot instantiate " + constructor.getDeclaringClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new ParseException("Cannot access constructor " + constructor, e);
        } catch (InvocationTargetException e) {
            throw new ParseException("Constructor failed: " + constructor, e.getCause());
        }
    }

    private Constructor<?> findConstructor(Class<?> type, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private List<TerminalSymbol> terminalsWithImplicitWhitespace() {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>(model.getTerminals());
        result.add(new TerminalSymbol(ImplicitWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return result;
    }

    private static final class ImplicitWhitespace {
    }

    private static final class ParseState {
        private final int position;
        private final List<Object> values;

        private ParseState(int position, List<Object> values) {
            this.position = position;
            this.values = values;
        }
    }

    private static final class ParseResult {
        private final Object value;
        private final int position;

        private ParseResult(Object value, int position) {
            this.value = value;
            this.position = position;
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
