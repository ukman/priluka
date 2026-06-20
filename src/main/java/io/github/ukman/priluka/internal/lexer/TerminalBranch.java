package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

public final class TerminalBranch {
    private final String groupName;
    private final int groupIndex;
    private final TerminalSymbol terminal;

    TerminalBranch(String groupName, int groupIndex, TerminalSymbol terminal) {
        this.groupName = groupName;
        this.groupIndex = groupIndex;
        this.terminal = terminal;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getGroupIndex() {
        return groupIndex;
    }

    public TerminalSymbol getTerminal() {
        return terminal;
    }
}
