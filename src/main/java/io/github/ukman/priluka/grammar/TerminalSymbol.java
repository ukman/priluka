package io.github.ukman.priluka.grammar;

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

    public TerminalSymbol(Class<?> type, Kind kind, String pattern, boolean skip, int priority) {
        this.type = type;
        this.kind = kind;
        this.pattern = pattern;
        this.skip = skip;
        this.priority = priority;
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
}
