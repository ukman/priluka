package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.LexerEngine;

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

    public static Lexer asciiText(LexerSpec spec) {
        return new AsciiTextLexer(spec);
    }

    public static Lexer create(LexerEngine engine, LexerSpec spec, LexerOptions options) {
        if (engine == LexerEngine.ASCII_TEXT) {
            return asciiText(spec);
        }
        if (engine == LexerEngine.JAVA_REGEX) {
            return javaRegex(spec, options);
        }
        return brics(spec, options);
    }
}
