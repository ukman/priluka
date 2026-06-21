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
    private final List<CheckedTerminal> checkedWordTerminals = new ArrayList<CheckedTerminal>();
    private final List<CheckedTerminal> checkedNumberTerminals = new ArrayList<CheckedTerminal>();
    private final List<CheckedTerminal> checkedSymbolTerminals = new ArrayList<CheckedTerminal>();
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
        boolean hardBoundaryBefore = false;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isWhitespace(c)) {
                if (isHardBoundary(c)) {
                    hardBoundaryBefore = true;
                }
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
            lexemes.add(new Lexeme(start, position - start, text, terminalTypes(text, c), false, hardBoundaryBefore));
            hardBoundaryBefore = false;
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

        Pattern pattern = Pattern.compile(terminal.getPattern());
        boolean word = matches(pattern, "abc") || matches(pattern, "Abc") || matches(pattern, "ABC");
        boolean number = matches(pattern, "123") || matches(pattern, "0") || matches(pattern, "987654");
        boolean symbol = matches(pattern, "@") || matches(pattern, ",") || matches(pattern, ".");

        if (word) {
            if (matches(pattern, "abc") && matches(pattern, "Abc") && matches(pattern, "ABC")) {
                wordCarriers.add(terminal);
            } else {
                checkedWordTerminals.add(new CheckedTerminal(terminal, pattern));
            }
        }
        if (number) {
            if (matches(pattern, "0") && matches(pattern, "123") && matches(pattern, "987654")) {
                numberCarriers.add(terminal);
            } else {
                checkedNumberTerminals.add(new CheckedTerminal(terminal, pattern));
            }
        }
        if (symbol) {
            if (matches(pattern, "@") && matches(pattern, ",") && matches(pattern, ".")) {
                symbolCarriers.add(terminal);
            } else {
                checkedSymbolTerminals.add(new CheckedTerminal(terminal, pattern));
            }
        }
    }

    private boolean matches(TerminalSymbol terminal, String text) {
        return Pattern.compile(terminal.getPattern()).matcher(text).matches();
    }

    private boolean matches(Pattern pattern, String text) {
        return pattern.matcher(text).matches();
    }

    private List<TerminalSymbol> terminalTypes(String text, char firstChar) {
        List<TerminalSymbol> terminalTypes = new ArrayList<TerminalSymbol>();
        if (isAsciiLetter(firstChar)) {
            terminalTypes.addAll(wordCarriers);
            addCheckedMatches(terminalTypes, checkedWordTerminals, text);
        } else if (isAsciiDigit(firstChar)) {
            terminalTypes.addAll(numberCarriers);
            addCheckedMatches(terminalTypes, checkedNumberTerminals, text);
        } else {
            terminalTypes.addAll(symbolCarriers);
            addCheckedMatches(terminalTypes, checkedSymbolTerminals, text);
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

    private void addCheckedMatches(
        List<TerminalSymbol> terminalTypes,
        List<CheckedTerminal> checkedTerminals,
        String text
    ) {
        for (int i = 0; i < checkedTerminals.size(); i++) {
            CheckedTerminal checked = checkedTerminals.get(i);
            if (checked.pattern.matcher(text).matches() && !terminalTypes.contains(checked.terminal)) {
                terminalTypes.add(checked.terminal);
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

    private boolean isHardBoundary(char c) {
        return c == '\n' || c == '\r' || c == '\t' || c == '\f';
    }

    private static final class CheckedTerminal {
        private final TerminalSymbol terminal;
        private final Pattern pattern;

        private CheckedTerminal(TerminalSymbol terminal, Pattern pattern) {
            this.terminal = terminal;
            this.pattern = pattern;
        }
    }
}
