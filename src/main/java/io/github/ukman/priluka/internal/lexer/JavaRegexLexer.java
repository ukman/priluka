package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaRegexLexer implements Lexer {
    private final LexerSpec spec;
    private final LexerOptions options;
    private final MasterPattern masterPattern;
    private final KeywordCarrierIndex keywordCarrierIndex;
    private final Map<TerminalSymbol, Pattern> terminalPatterns;

    public JavaRegexLexer(LexerSpec spec) {
        this(spec, LexerOptions.DEFAULT);
    }

    public JavaRegexLexer(LexerSpec spec, LexerOptions options) {
        this.spec = spec;
        this.options = options;
        this.keywordCarrierIndex = options.isKeywordCarrierOptimization()
            ? KeywordCarrierIndex.build(spec.getTerminals())
            : KeywordCarrierIndex.empty(spec.getTerminals());
        this.masterPattern = new MasterPatternBuilder(options.isRegexpCaseSensitive())
            .build(new LexerSpec(keywordCarrierIndex.getMasterTerminals()));
        this.terminalPatterns = compileTerminalPatterns(spec.getTerminals());
    }

    @Override
    public List<Lexeme> tokenize(String input) {
        List<Lexeme> lexemes = new ArrayList<Lexeme>();
        Matcher matcher = masterPattern.getPattern().matcher(input);
        int position = 0;
        while (matcher.find()) {
            if (matcher.start() != position) {
                throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
            }

            String text = matcher.group();
            if (text.length() == 0) {
                throw new LexerException("Lexer pattern matched empty text at offset " + position);
            }

            TerminalBranch branch = masterPattern.getMatchedBranch(matcher);
            if (branch == null) {
                throw new LexerException("Master pattern matched without a terminal branch at offset " + position);
            }

            List<TerminalSymbol> terminalTypes = terminalTypes(branch, text);
            boolean skipped = allSkipped(terminalTypes);
            Lexeme lexeme = new Lexeme(position, text.length(), text, terminalTypes, skipped);
            if (!skipped) {
                lexemes.add(lexeme);
            }
            position += text.length();
        }
        if (position != input.length()) {
            throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
        }
        return lexemes;
    }

    @Override
    public int countTokens(String input) {
        Matcher matcher = masterPattern.getPattern().matcher(input);
        int position = 0;
        int tokenCount = 0;
        while (matcher.find()) {
            if (matcher.start() != position) {
                throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
            }

            int end = matcher.end();
            if (end == position) {
                throw new LexerException("Lexer pattern matched empty text at offset " + position);
            }

            TerminalBranch branch = masterPattern.getMatchedBranch(matcher);
            if (branch == null) {
                throw new LexerException("Master pattern matched without a terminal branch at offset " + position);
            }

            if (!branch.getTerminal().isSkip()) {
                tokenCount++;
            }
            position = end;
        }
        if (position != input.length()) {
            throw new LexerException("Unexpected input at offset " + position + ": " + input.charAt(position));
        }
        return tokenCount;
    }

    private List<TerminalSymbol> matchingTerminals(String text) {
        List<TerminalSymbol> matches = new ArrayList<TerminalSymbol>();
        for (TerminalSymbol terminal : spec.getTerminals()) {
            if (keywordCarrierIndex.isCoveredKeyword(terminal)) {
                continue;
            }
            if (matches(terminal, text)) {
                matches.add(terminal);
            }
        }
        return matches;
    }

    private List<TerminalSymbol> terminalTypes(TerminalBranch branch, String text) {
        List<TerminalSymbol> terminalTypes;
        if (options.isCollectAmbiguousTerminalTypes()) {
            terminalTypes = matchingTerminals(text);
        } else {
            if (options.isKeywordCarrierOptimization()) {
                terminalTypes = new ArrayList<TerminalSymbol>();
                terminalTypes.add(branch.getTerminal());
            } else {
                terminalTypes = Collections.singletonList(branch.getTerminal());
            }
        }
        if (options.isKeywordCarrierOptimization()) {
            keywordCarrierIndex.addKeywordMatches(text, terminalTypes);
        }
        return terminalTypes;
    }

    private boolean matches(TerminalSymbol terminal, String text) {
        return terminalPatterns.get(terminal).matcher(text).matches();
    }

    private String regexFor(TerminalSymbol terminal) {
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            String quoted = Pattern.quote(terminal.getPattern());
            if (!terminal.isCaseSensitive()) {
                return "(?iu:" + quoted + ")";
            }
            return quoted;
        }
        if (!options.isRegexpCaseSensitive()) {
            return "(?iu:" + terminal.getPattern() + ")";
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

    private Map<TerminalSymbol, Pattern> compileTerminalPatterns(List<TerminalSymbol> terminals) {
        Map<TerminalSymbol, Pattern> patterns = new LinkedHashMap<TerminalSymbol, Pattern>();
        for (TerminalSymbol terminal : terminals) {
            patterns.put(terminal, Pattern.compile(regexFor(terminal)));
        }
        return patterns;
    }
}
