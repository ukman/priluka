package io.github.ukman.priluka.internal.lexer;

public interface LexemeCursor {
    boolean next();

    int getStart();

    int getLen();

    String getText();

    boolean hasTerminal(Class<?> terminalType);
}
