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
    private final Map<Class<?>, Map<String, List<TerminalSymbol>>> keywordsByCarrier;
    private final List<TerminalSymbol> coveredKeywords;

    private KeywordCarrierIndex(
        List<TerminalSymbol> masterTerminals,
        Map<Class<?>, Map<String, List<TerminalSymbol>>> keywordsByCarrier,
        List<TerminalSymbol> coveredKeywords
    ) {
        this.masterTerminals = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(masterTerminals));
        this.keywordsByCarrier = keywordsByCarrier;
        this.coveredKeywords = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(coveredKeywords));
    }

    static KeywordCarrierIndex build(List<TerminalSymbol> terminals) {
        List<TerminalSymbol> masterTerminals = new ArrayList<TerminalSymbol>();
        List<TerminalSymbol> coveredKeywords = new ArrayList<TerminalSymbol>();
        Map<Class<?>, Map<String, List<TerminalSymbol>>> keywordsByCarrier =
            new LinkedHashMap<Class<?>, Map<String, List<TerminalSymbol>>>();

        for (TerminalSymbol terminal : terminals) {
            if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD && attachToCarrier(terminal, terminals, keywordsByCarrier)) {
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
            new LinkedHashMap<Class<?>, Map<String, List<TerminalSymbol>>>(),
            new ArrayList<TerminalSymbol>()
        );
    }

    List<TerminalSymbol> getMasterTerminals() {
        return masterTerminals;
    }

    void addKeywordMatches(String text, List<TerminalSymbol> terminalTypes) {
        List<TerminalSymbol> snapshot = new ArrayList<TerminalSymbol>(terminalTypes);
        for (TerminalSymbol terminal : snapshot) {
            Map<String, List<TerminalSymbol>> keywordsByText = keywordsByCarrier.get(terminal.getType());
            if (keywordsByText == null) {
                continue;
            }
            List<TerminalSymbol> keywords = keywordsByText.get(normalize(text));
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
        Map<Class<?>, Map<String, List<TerminalSymbol>>> keywordsByCarrier
    ) {
        for (TerminalSymbol carrier : terminals) {
            if (carrier.getKind() == TerminalSymbol.Kind.KEYWORD) {
                continue;
            }
            if (matches(carrier, keyword.getPattern())) {
                Map<String, List<TerminalSymbol>> keywordsByText = keywordsByCarrier.get(carrier.getType());
                if (keywordsByText == null) {
                    keywordsByText = new LinkedHashMap<String, List<TerminalSymbol>>();
                    keywordsByCarrier.put(carrier.getType(), keywordsByText);
                }
                String key = normalize(keyword.getPattern());
                List<TerminalSymbol> keywords = keywordsByText.get(key);
                if (keywords == null) {
                    keywords = new ArrayList<TerminalSymbol>();
                    keywordsByText.put(key, keywords);
                }
                keywords.add(keyword);
                return true;
            }
        }
        return false;
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
}
