package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

public final class TerminalBranch {
    private final String groupName;
    private final TerminalSymbol terminal;

    TerminalBranch(String groupName, TerminalSymbol terminal) {
        this.groupName = groupName;
        this.terminal = terminal;
    }

    public String getGroupName() {
        return groupName;
    }

    public TerminalSymbol getTerminal() {
        return terminal;
    }
}
