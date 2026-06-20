package io.github.ukman.priluka.internal.lexer;

import java.util.List;

public interface Lexer {
    List<Lexeme> tokenize(String input);

    int countTokens(String input);
}
