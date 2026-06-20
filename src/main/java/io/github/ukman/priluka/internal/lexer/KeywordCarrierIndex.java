package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class KeywordCarrierIndex {
    private final List<TerminalSymbol> masterTerminals;
    private final Map<Class<?>, KeywordLookup> keywordsByCarrier;
    private final List<TerminalSymbol> coveredKeywords;

    private KeywordCarrierIndex(
        List<TerminalSymbol> masterTerminals,
        Map<Class<?>, KeywordLookup> keywordsByCarrier,
        List<TerminalSymbol> coveredKeywords
    ) {
        this.masterTerminals = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(masterTerminals));
        this.keywordsByCarrier = keywordsByCarrier;
        this.coveredKeywords = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(coveredKeywords));
    }

    static KeywordCarrierIndex build(List<TerminalSymbol> terminals) {
        List<TerminalSymbol> masterTerminals = new ArrayList<TerminalSymbol>();
        List<TerminalSymbol> coveredKeywords = new ArrayList<TerminalSymbol>();
        Map<Class<?>, KeywordLookup> keywordsByCarrier = new LinkedHashMap<Class<?>, KeywordLookup>();

        for (TerminalSymbol terminal : terminals) {
            if (!terminal.getKeywordTexts().isEmpty() && attachToCarrier(terminal, terminals, keywordsByCarrier)) {
                coveredKeywords.add(terminal);
            } else {
                masterTerminals.add(terminal);
            }
        }
        return new KeywordCarrierIndex(masterTerminals, keywordsByCarrier, coveredKeywords);
    }

    static KeywordCarrierIndex empty(List<TerminalSymbol> terminals) {
        return new KeywordCarrierIndex(
            terminals,
            new LinkedHashMap<Class<?>, KeywordLookup>(),
            new ArrayList<TerminalSymbol>()
        );
    }

    List<TerminalSymbol> getMasterTerminals() {
        return masterTerminals;
    }

    void addKeywordMatches(String text, List<TerminalSymbol> terminalTypes) {
        List<TerminalSymbol> snapshot = new ArrayList<TerminalSymbol>(terminalTypes);
        for (TerminalSymbol terminal : snapshot) {
            KeywordLookup keywordLookup = keywordsByCarrier.get(terminal.getType());
            if (keywordLookup == null) {
                continue;
            }
            List<TerminalSymbol> keywords = keywordLookup.find(text);
            if (keywords == null) {
                continue;
            }
            for (TerminalSymbol keyword : keywords) {
                if (!terminalTypes.contains(keyword)) {
                    terminalTypes.add(keyword);
                }
            }
        }
    }

    boolean isCoveredKeyword(TerminalSymbol terminal) {
        return coveredKeywords.contains(terminal);
    }

    private static boolean attachToCarrier(
        TerminalSymbol keyword,
        List<TerminalSymbol> terminals,
        Map<Class<?>, KeywordLookup> keywordsByCarrier
    ) {
        for (TerminalSymbol carrier : terminals) {
            if (!carrier.getKeywordTexts().isEmpty()) {
                continue;
            }
            if (matchesAll(carrier, keyword.getKeywordTexts())) {
                KeywordLookup keywordLookup = keywordsByCarrier.get(carrier.getType());
                if (keywordLookup == null) {
                    keywordLookup = new KeywordLookup();
                    keywordsByCarrier.put(carrier.getType(), keywordLookup);
                }
                keywordLookup.add(keyword);
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAll(TerminalSymbol terminal, List<String> texts) {
        for (String text : texts) {
            if (!matches(terminal, text)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(TerminalSymbol terminal, String text) {
        return Pattern.compile(regexFor(terminal)).matcher(text).matches();
    }

    private static String regexFor(TerminalSymbol terminal) {
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            String quoted = Pattern.quote(terminal.getPattern());
            if (!terminal.isCaseSensitive()) {
                return "(?iu:" + quoted + ")";
            }
            return quoted;
        }
        return terminal.getPattern();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    private static final class KeywordLookup {
        private final Map<String, List<TerminalSymbol>> exactKeywords =
            new LinkedHashMap<String, List<TerminalSymbol>>();
        private final Map<String, List<TerminalSymbol>> caseInsensitiveKeywords =
            new LinkedHashMap<String, List<TerminalSymbol>>();

        void add(TerminalSymbol keyword) {
            Map<String, List<TerminalSymbol>> target = keyword.isCaseSensitive()
                ? exactKeywords
                : caseInsensitiveKeywords;
            for (String text : keyword.getKeywordTexts()) {
                String key = keyword.isCaseSensitive()
                    ? text
                    : normalize(text);
                List<TerminalSymbol> keywords = target.get(key);
                if (keywords == null) {
                    keywords = new ArrayList<TerminalSymbol>();
                    target.put(key, keywords);
                }
                keywords.add(keyword);
            }
        }

        List<TerminalSymbol> find(String text) {
            List<TerminalSymbol> found = null;
            List<TerminalSymbol> exact = exactKeywords.get(text);
            if (exact != null) {
                found = new ArrayList<TerminalSymbol>(exact);
            }

            List<TerminalSymbol> caseInsensitive = caseInsensitiveKeywords.get(normalize(text));
            if (caseInsensitive != null) {
                if (found == null) {
                    found = new ArrayList<TerminalSymbol>();
                }
                found.addAll(caseInsensitive);
            }
            return found;
        }
    }
}
