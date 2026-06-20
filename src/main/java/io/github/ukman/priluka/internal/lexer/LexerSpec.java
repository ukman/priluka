package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LexerSpec {
    private final List<TerminalSymbol> terminals;

    public LexerSpec(List<TerminalSymbol> terminals) {
        this.terminals = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(terminals));
    }

    public List<TerminalSymbol> getTerminals() {
        return terminals;
    }
}
