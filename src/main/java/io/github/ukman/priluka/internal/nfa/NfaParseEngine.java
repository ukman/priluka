package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseException;
import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.internal.lexer.LexerConfig;
import io.github.ukman.priluka.internal.parser.ParseEngine;

public final class NfaParseEngine implements ParseEngine {
    private final NfaRecognizer recognizer;

    public NfaParseEngine(GrammarModel model) {
        this(model, LexerConfig.DEFAULT);
    }

    public NfaParseEngine(GrammarModel model, LexerConfig lexerConfig) {
        this.recognizer = new NfaRecognizer(model, lexerConfig);
    }

    @Override
    public ParseTrace parseTrace(Class<?> start, String input) {
        ParseTrace trace = recognizer.parseTrace(input);
        if (trace == null) {
            throw new ParseException("Input does not match start symbol: " + start.getName());
        }
        return trace;
    }
}
