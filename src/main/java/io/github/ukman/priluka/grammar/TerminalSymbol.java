package io.github.ukman.priluka.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TerminalSymbol {
    public enum Kind {
        REGEXP,
        KEYWORD,
        BUILT_IN
    }

    private final Class<?> type;
    private final Kind kind;
    private final String pattern;
    private final boolean skip;
    private final int priority;
    private final boolean caseSensitive;
    private final List<String> keywordTexts;

    public TerminalSymbol(Class<?> type, Kind kind, String pattern, boolean skip, int priority) {
        this(type, kind, pattern, skip, priority, true);
    }

    public TerminalSymbol(Class<?> type, Kind kind, String pattern, boolean skip, int priority, boolean caseSensitive) {
        this(
            type,
            kind,
            pattern,
            skip,
            priority,
            caseSensitive,
            kind == Kind.KEYWORD ? Collections.singletonList(pattern) : Collections.<String>emptyList()
        );
    }

    public TerminalSymbol(
        Class<?> type,
        Kind kind,
        String pattern,
        boolean skip,
        int priority,
        boolean caseSensitive,
        List<String> keywordTexts
    ) {
        this.type = type;
        this.kind = kind;
        this.pattern = pattern;
        this.skip = skip;
        this.priority = priority;
        this.caseSensitive = caseSensitive;
        this.keywordTexts = Collections.unmodifiableList(new ArrayList<String>(keywordTexts));
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return type.getSimpleName();
    }

    public Kind getKind() {
        return kind;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isSkip() {
        return skip;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public List<String> getKeywordTexts() {
        return keywordTexts;
    }

    public TerminalSymbol withSkip(boolean skip) {
        return new TerminalSymbol(type, kind, pattern, skip, priority, caseSensitive, keywordTexts);
    }
}
