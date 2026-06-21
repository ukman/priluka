package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AsciiTextLexer implements Lexer {
    private final List<TerminalSymbol> wordCarriers = new ArrayList<TerminalSymbol>();
    private final List<TerminalSymbol> numberCarriers = new ArrayList<TerminalSymbol>();
    private final List<TerminalSymbol> symbolCarriers = new ArrayList<TerminalSymbol>();
    private final Map<String, List<TerminalSymbol>> exactKeywords =
        new LinkedHashMap<String, List<TerminalSymbol>>();
    private final Map<String, List<TerminalSymbol>> caseInsensitiveKeywords =
        new LinkedHashMap<String, List<TerminalSymbol>>();

    public AsciiTextLexer(LexerSpec spec) {
        for (TerminalSymbol terminal : spec.getTerminals()) {
            if (!terminal.getKeywordTexts().isEmpty()) {
                addKeywordTerminal(terminal);
            } else {
                addCarrierTerminal(terminal);
            }
        }
    }

    @Override
    public List<Lexeme> tokenize(String input) {
        List<Lexeme> lexemes = new ArrayList<Lexeme>();
        int position = 0;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isWhitespace(c)) {
                position++;
                continue;
            }

            int start = position;
            if (isAsciiLetter(c)) {
                position++;
                while (position < input.length() && isAsciiLetter(input.charAt(position))) {
                    position++;
                }
            } else if (isAsciiDigit(c)) {
                position++;
                while (position < input.length() && isAsciiDigit(input.charAt(position))) {
                    position++;
                }
            } else {
                position++;
            }

            String text = input.substring(start, position);
            lexemes.add(new Lexeme(start, position - start, text, terminalTypes(text, c), false));
        }
        return lexemes;
    }

    @Override
    public int countTokens(String input) {
        int tokens = 0;
        int position = 0;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isWhitespace(c)) {
                position++;
                continue;
            }
            tokens++;
            if (isAsciiLetter(c)) {
                position++;
                while (position < input.length() && isAsciiLetter(input.charAt(position))) {
                    position++;
                }
            } else if (isAsciiDigit(c)) {
                position++;
                while (position < input.length() && isAsciiDigit(input.charAt(position))) {
                    position++;
                }
            } else {
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

    private void addCarrierTerminal(TerminalSymbol terminal) {
        if (terminal.getKind() != TerminalSymbol.Kind.REGEXP) {
            return;
        }

        boolean word = matches(terminal, "abc") && !matches(terminal, "123") && !matches(terminal, "@");
        boolean number = matches(terminal, "123") && !matches(terminal, "abc") && !matches(terminal, "@");
        boolean symbol = matches(terminal, "@") && !matches(terminal, "abc") && !matches(terminal, "123");

        if (word) {
            wordCarriers.add(terminal);
        }
        if (number) {
            numberCarriers.add(terminal);
        }
        if (symbol) {
            symbolCarriers.add(terminal);
        }
    }

    private boolean matches(TerminalSymbol terminal, String text) {
        return Pattern.compile(terminal.getPattern()).matcher(text).matches();
    }

    private List<TerminalSymbol> terminalTypes(String text, char firstChar) {
        List<TerminalSymbol> terminalTypes = new ArrayList<TerminalSymbol>();
        if (isAsciiLetter(firstChar)) {
            terminalTypes.addAll(wordCarriers);
        } else if (isAsciiDigit(firstChar)) {
            terminalTypes.addAll(numberCarriers);
        } else {
            terminalTypes.addAll(symbolCarriers);
        }

        List<TerminalSymbol> exact = exactKeywords.get(text);
        if (exact != null) {
            addAllAbsent(terminalTypes, exact);
        }

        List<TerminalSymbol> caseInsensitive = caseInsensitiveKeywords.get(normalize(text));
        if (caseInsensitive != null) {
            addAllAbsent(terminalTypes, caseInsensitive);
        }
        return terminalTypes;
    }

    private void addAllAbsent(List<TerminalSymbol> terminalTypes, List<TerminalSymbol> additions) {
        for (int i = 0; i < additions.size(); i++) {
            TerminalSymbol terminal = additions.get(i);
            if (!terminalTypes.contains(terminal)) {
                terminalTypes.add(terminal);
            }
        }
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
