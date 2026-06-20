package io.github.ukman.priluka.internal.lexer;

public final class LexerOptions {
    public static final LexerOptions DEFAULT = new LexerOptions(true, true);
    public static final LexerOptions STRICT = new LexerOptions(false, false);

    private final boolean collectAmbiguousTerminalTypes;
    private final boolean keywordCarrierOptimization;

    public LexerOptions(boolean collectAmbiguousTerminalTypes, boolean keywordCarrierOptimization) {
        this.collectAmbiguousTerminalTypes = collectAmbiguousTerminalTypes;
        this.keywordCarrierOptimization = keywordCarrierOptimization;
    }

    public boolean isCollectAmbiguousTerminalTypes() {
        return collectAmbiguousTerminalTypes;
    }

    public boolean isKeywordCarrierOptimization() {
        return keywordCarrierOptimization;
    }
}
