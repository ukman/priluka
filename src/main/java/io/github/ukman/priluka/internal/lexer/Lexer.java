package io.github.ukman.priluka.internal.lexer;

import dk.brics.automaton.RunAutomaton;
import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Lexer {
    private final LexerOptions options;
    private final KeywordCarrierIndex keywordCarrierIndex;
    private final List<TerminalAutomaton> masterAutomata;
    private final List<TerminalAutomaton> allAutomata;

    public Lexer(LexerSpec spec) {
        this(spec, LexerOptions.DEFAULT);
    }

    public Lexer(LexerSpec spec, LexerOptions options) {
        this.options = options;
        this.keywordCarrierIndex = options.isKeywordCarrierOptimization()
            ? KeywordCarrierIndex.build(spec.getTerminals())
            : KeywordCarrierIndex.empty(spec.getTerminals());
        this.masterAutomata = compileAutomata(keywordCarrierIndex.getMasterTerminals());
        this.allAutomata = compileAutomata(spec.getTerminals());
    }

    public List<Lexeme> tokenize(String input) {
        List<Lexeme> lexemes = new ArrayList<Lexeme>();
        int position = 0;
        while (position < input.length()) {
            TerminalMatch match = longestMatch(masterAutomata, input, position);
            if (match.length == 0) {
                throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
            }

            String text = input.substring(position, position + match.length);
            List<TerminalSymbol> terminalTypes = terminalTypes(match.automaton, text);
            boolean skipped = allSkipped(terminalTypes);
            Lexeme lexeme = new Lexeme(position, text.length(), text, terminalTypes, skipped);
            if (!skipped) {
                lexemes.add(lexeme);
            }
            position += match.length;
        }
        return lexemes;
    }

    public int countTokens(String input) {
        int position = 0;
        int tokenCount = 0;
        while (position < input.length()) {
            TerminalMatch match = longestMatch(masterAutomata, input, position);
            if (match.length == 0) {
                throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
            }

            if (!match.automaton.terminal.isSkip()) {
                tokenCount++;
            }
            position += match.length;
        }
        return tokenCount;
    }

    private List<TerminalSymbol> matchingTerminals(String text) {
        List<TerminalSymbol> matches = new ArrayList<TerminalSymbol>();
        for (TerminalAutomaton automaton : allAutomata) {
            TerminalSymbol terminal = automaton.terminal;
            if (!keywordCarrierIndex.isCoveredKeyword(terminal) && automaton.automaton.run(text)) {
                matches.add(terminal);
            }
        }
        return matches;
    }

    private List<TerminalSymbol> terminalTypes(TerminalAutomaton branch, String text) {
        List<TerminalSymbol> terminalTypes;
        if (options.isCollectAmbiguousTerminalTypes()) {
            terminalTypes = matchingTerminals(text);
        } else {
            if (options.isKeywordCarrierOptimization()) {
                terminalTypes = new ArrayList<TerminalSymbol>();
                terminalTypes.add(branch.terminal);
            } else {
                terminalTypes = Collections.singletonList(branch.terminal);
            }
        }
        if (options.isKeywordCarrierOptimization()) {
            keywordCarrierIndex.addKeywordMatches(text, terminalTypes);
        }
        return terminalTypes;
    }

    private boolean allSkipped(List<TerminalSymbol> terminalTypes) {
        if (terminalTypes.isEmpty()) {
            return false;
        }
        for (TerminalSymbol terminal : terminalTypes) {
            if (!terminal.isSkip()) {
                return false;
            }
        }
        return true;
    }

    private List<TerminalAutomaton> compileAutomata(List<TerminalSymbol> terminals) {
        List<TerminalSymbol> ordered = new ArrayList<TerminalSymbol>(terminals);
        ordered.sort(new MasterPatternBuilder.TerminalOrder());

        List<TerminalAutomaton> automata = new ArrayList<TerminalAutomaton>();
        for (TerminalSymbol terminal : ordered) {
            automata.add(new TerminalAutomaton(terminal, BricsTerminalAutomata.compile(terminal)));
        }
        return Collections.unmodifiableList(automata);
    }

    private TerminalMatch longestMatch(List<TerminalAutomaton> automata, String input, int position) {
        TerminalAutomaton bestAutomaton = null;
        int bestLength = 0;
        for (TerminalAutomaton automaton : automata) {
            int length = matchLength(automaton.automaton, input, position);
            if (length > bestLength) {
                bestAutomaton = automaton;
                bestLength = length;
            }
        }
        return new TerminalMatch(bestAutomaton, bestLength);
    }

    private int matchLength(RunAutomaton automaton, String input, int position) {
        int state = automaton.getInitialState();
        int best = 0;
        for (int i = position; i < input.length(); i++) {
            state = automaton.step(state, input.charAt(i));
            if (state == -1) {
                break;
            }
            if (automaton.isAccept(state)) {
                best = i - position + 1;
            }
        }
        return best;
    }

    private static final class TerminalAutomaton {
        private final TerminalSymbol terminal;
        private final RunAutomaton automaton;

        private TerminalAutomaton(TerminalSymbol terminal, RunAutomaton automaton) {
            this.terminal = terminal;
            this.automaton = automaton;
        }
    }

    private static final class TerminalMatch {
        private final TerminalAutomaton automaton;
        private final int length;

        private TerminalMatch(TerminalAutomaton automaton, int length) {
            this.automaton = automaton;
            this.length = length;
        }
    }
}
