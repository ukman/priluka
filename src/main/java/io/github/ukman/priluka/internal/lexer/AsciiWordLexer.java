package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AsciiWordLexer implements StreamingLexer {
    private final TerminalSymbol wordTerminal;
    private final Map<String, List<TerminalSymbol>> exactKeywords =
        new LinkedHashMap<String, List<TerminalSymbol>>();
    private final Map<String, List<TerminalSymbol>> caseInsensitiveKeywords =
        new LinkedHashMap<String, List<TerminalSymbol>>();
    private final Map<Class<?>, KeywordTerminal> keywordTerminals =
        new LinkedHashMap<Class<?>, KeywordTerminal>();

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

    @Override
    public LexemeCursor cursor(String input) {
        return new AsciiWordCursor(input);
    }

    private void addKeywordTerminal(TerminalSymbol terminal) {
        Map<String, List<TerminalSymbol>> target = terminal.isCaseSensitive()
            ? exactKeywords
            : caseInsensitiveKeywords;
        List<String> cursorTexts = new ArrayList<String>();
        for (String text : terminal.getKeywordTexts()) {
            String key = terminal.isCaseSensitive() ? text : normalize(text);
            List<TerminalSymbol> terminals = target.get(key);
            if (terminals == null) {
                terminals = new ArrayList<TerminalSymbol>();
                target.put(key, terminals);
            }
            terminals.add(terminal);
            cursorTexts.add(key);
        }
        keywordTerminals.put(terminal.getType(), new KeywordTerminal(terminal.isCaseSensitive(), cursorTexts));
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

    private final class AsciiWordCursor implements LexemeCursor {
        private final String input;
        private int position;
        private int start = -1;
        private int end = -1;
        private String text;
        private boolean hardBoundaryBefore;

        private AsciiWordCursor(String input) {
            this.input = input;
        }

        @Override
        public boolean next() {
            text = null;
            boolean boundary = false;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (isAsciiLetter(c)) {
                    start = position;
                    hardBoundaryBefore = boundary;
                    position++;
                    while (position < input.length() && isAsciiLetter(input.charAt(position))) {
                        position++;
                    }
                    end = position;
                    return true;
                }
                if (isHardBoundary(c)) {
                    boundary = true;
                }
                position++;
            }
            return false;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getLen() {
            return end - start;
        }

        @Override
        public String getText() {
            if (text == null) {
                text = input.substring(start, end);
            }
            return text;
        }

        @Override
        public boolean hasHardBoundaryBefore() {
            return hardBoundaryBefore;
        }

        @Override
        public boolean hasTerminal(Class<?> terminalType) {
            if (wordTerminal.getType().equals(terminalType)) {
                return true;
            }
            KeywordTerminal terminal = keywordTerminals.get(terminalType);
            if (terminal == null) {
                return false;
            }
            for (String keyword : terminal.texts) {
                if (matchesKeyword(keyword, terminal.caseSensitive)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matchesKeyword(String keyword, boolean caseSensitive) {
            int len = end - start;
            if (keyword.length() != len) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                char actual = input.charAt(start + i);
                char expected = keyword.charAt(i);
                if (!caseSensitive) {
                    actual = lowerAscii(actual);
                }
                if (actual != expected) {
                    return false;
                }
            }
            return true;
        }
    }

    private char lowerAscii(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + ('a' - 'A'));
        }
        return c;
    }

    private boolean isHardBoundary(char c) {
        return c == '\n' || c == '\r' || c == '\t' || c == '\f';
    }

    private static final class KeywordTerminal {
        private final boolean caseSensitive;
        private final List<String> texts;

        private KeywordTerminal(boolean caseSensitive, List<String> texts) {
            this.caseSensitive = caseSensitive;
            this.texts = texts;
        }
    }
}
