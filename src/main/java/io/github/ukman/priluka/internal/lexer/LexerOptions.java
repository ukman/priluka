package io.github.ukman.priluka.internal.lexer;

public final class LexerOptions {
    public static final LexerOptions DEFAULT = new LexerOptions(true, true, true);
    public static final LexerOptions STRICT = new LexerOptions(false, false, true);

    private final boolean collectAmbiguousTerminalTypes;
    private final boolean keywordCarrierOptimization;
    private final boolean regexpCaseSensitive;

    public LexerOptions(boolean collectAmbiguousTerminalTypes, boolean keywordCarrierOptimization) {
        this(collectAmbiguousTerminalTypes, keywordCarrierOptimization, true);
    }

    public LexerOptions(
        boolean collectAmbiguousTerminalTypes,
        boolean keywordCarrierOptimization,
        boolean regexpCaseSensitive
    ) {
        this.collectAmbiguousTerminalTypes = collectAmbiguousTerminalTypes;
        this.keywordCarrierOptimization = keywordCarrierOptimization;
        this.regexpCaseSensitive = regexpCaseSensitive;
    }

    public boolean isCollectAmbiguousTerminalTypes() {
        return collectAmbiguousTerminalTypes;
    }

    public boolean isKeywordCarrierOptimization() {
        return keywordCarrierOptimization;
    }

    public boolean isRegexpCaseSensitive() {
        return regexpCaseSensitive;
    }

    public LexerOptions withRegexpCaseSensitive(boolean regexpCaseSensitive) {
        return new LexerOptions(collectAmbiguousTerminalTypes, keywordCarrierOptimization, regexpCaseSensitive);
    }
}
