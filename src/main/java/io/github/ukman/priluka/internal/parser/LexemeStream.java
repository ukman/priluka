package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.internal.lexer.Lexeme;

import java.util.List;

final class LexemeStream {
    private final List<Lexeme> lexemes;
    private int position;

    LexemeStream(List<Lexeme> lexemes) {
        this.lexemes = lexemes;
    }

    int mark() {
        return position;
    }

    void reset(int mark) {
        this.position = mark;
    }

    boolean isEnd() {
        return position >= lexemes.size();
    }

    Lexeme peek() {
        if (isEnd()) {
            return null;
        }
        return lexemes.get(position);
    }

    Lexeme consume() {
        Lexeme lexeme = peek();
        if (lexeme != null) {
            position++;
        }
        return lexeme;
    }

    int position() {
        return position;
    }
}
