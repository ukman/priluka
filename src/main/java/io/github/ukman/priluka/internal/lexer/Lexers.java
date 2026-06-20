package io.github.ukman.priluka.internal.lexer;

public final class Lexers {
    private Lexers() {
    }

    public static Lexer defaultLexer(LexerSpec spec) {
        return brics(spec);
    }

    public static Lexer defaultLexer(LexerSpec spec, LexerOptions options) {
        return brics(spec, options);
    }

    public static Lexer brics(LexerSpec spec) {
        return new BricsLexer(spec);
    }

    public static Lexer brics(LexerSpec spec, LexerOptions options) {
        return new BricsLexer(spec, options);
    }

    public static Lexer javaRegex(LexerSpec spec) {
        return new JavaRegexLexer(spec);
    }

    public static Lexer javaRegex(LexerSpec spec, LexerOptions options) {
        return new JavaRegexLexer(spec, options);
    }

    public static Lexer asciiWord(LexerSpec spec, Class<?> wordTerminalType) {
        return new AsciiWordLexer(spec, wordTerminalType);
    }
}
