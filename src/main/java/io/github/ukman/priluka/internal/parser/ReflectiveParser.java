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
import java.util.ArrayList;
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
    private int parseDepth;

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
            parseDepth = 0;
        }
        Lexer lexer = Lexers.defaultLexer(new LexerSpec(terminalsWithImplicitWhitespace()), LexerOptions.DEFAULT);
        List<Lexeme> lexemes = lexer.tokenize(input);
        List<ParseResult> results = parseNonterminal(start, lexemes, 0);
        if (debug) {
            debugStats.topLevelResults = results.size();
        }
        ParseResult partial = null;
        ParseResult full = null;
        for (ParseResult result : results) {
            if (result.position == lexemes.size()) {
                if (debug) {
                    debugStats.fullResults++;
                }
                if (full == null) {
                    full = result;
                }
            } else {
                if (debug) {
                    debugStats.rejectedPartialResults++;
                }
                if (partial == null || result.position > partial.position) {
                    partial = result;
                }
            }
        }
        if (full != null) {
            if (debug) {
                debugStats.print(start, input.length(), lexemes.size());
            }
            return start.cast(full.value);
        }

        if (partial != null && partial.position < lexemes.size()) {
            if (debug) {
                debugStats.print(start, input.length(), lexemes.size());
            }
            Lexeme unexpected = lexemes.get(partial.position);
            throw new ParseException("Unexpected token at offset " + unexpected.getStart() + ": " + unexpected.getText());
        }
        if (debug) {
            debugStats.print(start, input.length(), lexemes.size());
        }
        throw new ParseException("Input does not match start symbol: " + start.getName());
    }

    private List<ParseResult> parseNonterminal(Class<?> type, List<Lexeme> lexemes, int position) {
        if (debug) {
            debugStats.nonterminalCalls++;
            parseDepth++;
            if (parseDepth > debugStats.maxParseDepth) {
                debugStats.maxParseDepth = parseDepth;
            }
        }
        try {
            NonterminalSymbol nonterminal = nonterminals.get(type);
            if (nonterminal == null) {
                return new ArrayList<ParseResult>();
            }

            List<ParseResult> results = new ArrayList<ParseResult>();
            for (Production production : nonterminal.getProductions()) {
                results.addAll(parseProduction(production, lexemes, position));
            }
            return results;
        } finally {
            if (debug) {
                parseDepth--;
            }
        }
    }

    private List<ParseResult> parseProduction(Production production, List<Lexeme> lexemes, int position) {
        if (debug) {
            debugStats.productionAttempts++;
        }
        List<ParseState> states = new ArrayList<ParseState>();
        states.add(new ParseState(position, new ArrayList<Object>()));

        for (ProductionPart part : production.getParts()) {
            if (part.getQuantifier() != ProductionPart.Quantifier.ONE) {
                throw new ParseException("Parser v1 supports only single production parts: " + part.toBnf());
            }
            List<ParseState> nextStates = new ArrayList<ParseState>();
            for (ParseState state : states) {
                List<ParseResult> partResults = parseSymbol(part.getSymbolType(), lexemes, state.position);
                for (ParseResult partResult : partResults) {
                    List<Object> values = new ArrayList<Object>(state.values);
                    values.add(partResult.value);
                    nextStates.add(new ParseState(partResult.position, values));
                }
            }
            if (nextStates.isEmpty()) {
                if (debug) {
                    debugStats.productionDeadEnds++;
                }
                return new ArrayList<ParseResult>();
            }
            states = nextStates;
        }

        List<ParseResult> results = new ArrayList<ParseResult>();
        for (ParseState state : states) {
            results.add(new ParseResult(instantiateProduction(production, state.values), state.position));
        }
        if (debug) {
            debugStats.productionMatches++;
            debugStats.parseResultsProduced += results.size();
        }
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

    private List<ParseResult> parseSymbol(Class<?> type, List<Lexeme> lexemes, int position) {
        if (terminals.containsKey(type)) {
            return parseTerminal(type, lexemes, position);
        }
        return parseNonterminal(type, lexemes, position);
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
