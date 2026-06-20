package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Lexeme {
    private final int start;
    private final int len;
    private final String text;
    private final List<TerminalSymbol> terminalTypes;
    private final boolean skipped;

    public Lexeme(int start, int len, String text, List<TerminalSymbol> terminalTypes, boolean skipped) {
        this.start = start;
        this.len = len;
        this.text = text;
        this.terminalTypes = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(terminalTypes));
        this.skipped = skipped;
    }

    public int getStart() {
        return start;
    }

    public int getLen() {
        return len;
    }

    public String getText() {
        return text;
    }

    public List<TerminalSymbol> getTerminalTypes() {
        return terminalTypes;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean hasTerminal(Class<?> terminalType) {
        for (TerminalSymbol terminal : terminalTypes) {
            if (terminal.getType().equals(terminalType)) {
                return true;
            }
        }
        return false;
    }
}
