package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Lexer {
    private final LexerSpec spec;
    private final MasterPattern masterPattern;

    public Lexer(LexerSpec spec) {
        this.spec = spec;
        this.masterPattern = new MasterPatternBuilder().build(spec);
    }

    public List<Lexeme> tokenize(String input) {
        List<Lexeme> lexemes = new ArrayList<Lexeme>();
        int position = 0;
        while (position < input.length()) {
            Matcher matcher = masterPattern.getPattern().matcher(input);
            matcher.region(position, input.length());
            if (!matcher.lookingAt()) {
                throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
            }

            String text = matcher.group();
            if (text.length() == 0) {
                throw new LexerException("Lexer pattern matched empty text at offset " + position);
            }

            List<TerminalSymbol> terminalTypes = matchingTerminals(text);
            boolean skipped = allSkipped(terminalTypes);
            Lexeme lexeme = new Lexeme(position, text.length(), text, terminalTypes, skipped);
            if (!skipped) {
                lexemes.add(lexeme);
            }
            position += text.length();
        }
        return lexemes;
    }

    private List<TerminalSymbol> matchingTerminals(String text) {
        List<TerminalSymbol> matches = new ArrayList<TerminalSymbol>();
        for (TerminalSymbol terminal : spec.getTerminals()) {
            if (matches(terminal, text)) {
                matches.add(terminal);
            }
        }
        return matches;
    }

    private boolean matches(TerminalSymbol terminal, String text) {
        return Pattern.compile(regexFor(terminal)).matcher(text).matches();
    }

    private String regexFor(TerminalSymbol terminal) {
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            return Pattern.quote(terminal.getPattern());
        }
        return terminal.getPattern();
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
}
