package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AsciiWordLexer implements Lexer {
    private final TerminalSymbol wordTerminal;
    private final Map<String, List<TerminalSymbol>> exactKeywords =
        new LinkedHashMap<String, List<TerminalSymbol>>();
    private final Map<String, List<TerminalSymbol>> caseInsensitiveKeywords =
        new LinkedHashMap<String, List<TerminalSymbol>>();

    public AsciiWordLexer(LexerSpec spec, Class<?> wordTerminalType) {
        TerminalSymbol foundWordTerminal = null;
        for (TerminalSymbol terminal : spec.getTerminals()) {
            if (terminal.getType().equals(wordTerminalType)) {
                foundWordTerminal = terminal;
            } else if (!terminal.getKeywordTexts().isEmpty()) {
                addKeywordTerminal(terminal);
            }
        }
        if (foundWordTerminal == null) {
            throw new LexerException("ASCII word lexer carrier terminal is absent: " + wordTerminalType.getName());
        }
        this.wordTerminal = foundWordTerminal;
    }

    @Override
    public List<Lexeme> tokenize(String input) {
        List<Lexeme> lexemes = new ArrayList<Lexeme>();
        int position = 0;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (!isAsciiLetter(c)) {
                position++;
                continue;
            }

            int start = position;
            position++;
            while (position < input.length() && isAsciiLetter(input.charAt(position))) {
                position++;
            }

            String text = input.substring(start, position);
            lexemes.add(new Lexeme(start, position - start, text, terminalTypes(text), false));
        }
        return lexemes;
    }

    @Override
    public int countTokens(String input) {
        int tokens = 0;
        int position = 0;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (!isAsciiLetter(c)) {
                position++;
                continue;
            }
            tokens++;
            position++;
            while (position < input.length() && isAsciiLetter(input.charAt(position))) {
                position++;
            }
        }
        return tokens;
    }

    private void addKeywordTerminal(TerminalSymbol terminal) {
        Map<String, List<TerminalSymbol>> target = terminal.isCaseSensitive()
            ? exactKeywords
            : caseInsensitiveKeywords;
        for (String text : terminal.getKeywordTexts()) {
            String key = terminal.isCaseSensitive() ? text : normalize(text);
            List<TerminalSymbol> terminals = target.get(key);
            if (terminals == null) {
                terminals = new ArrayList<TerminalSymbol>();
                target.put(key, terminals);
            }
            terminals.add(terminal);
        }
    }

    private List<TerminalSymbol> terminalTypes(String text) {
        List<TerminalSymbol> terminalTypes = new ArrayList<TerminalSymbol>();
        terminalTypes.add(wordTerminal);

        List<TerminalSymbol> exact = exactKeywords.get(text);
        if (exact != null) {
            terminalTypes.addAll(exact);
        }

        List<TerminalSymbol> caseInsensitive = caseInsensitiveKeywords.get(normalize(text));
        if (caseInsensitive != null) {
            terminalTypes.addAll(caseInsensitive);
        }
        return terminalTypes;
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
