package io.github.ukman.priluka.internal.lexer;

public interface StreamingLexer extends Lexer {
    LexemeCursor cursor(String input);
}
