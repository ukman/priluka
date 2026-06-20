package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class MasterPatternBuilder {
    public MasterPattern build(LexerSpec spec) {
        List<TerminalSymbol> terminals = new ArrayList<TerminalSymbol>(spec.getTerminals());
        terminals.sort(new TerminalOrder());

        List<TerminalBranch> branches = new ArrayList<TerminalBranch>();
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < terminals.size(); i++) {
            TerminalSymbol terminal = terminals.get(i);
            String groupName = "T" + i;
            if (pattern.length() > 0) {
                pattern.append('|');
            }
            pattern.append("(?<").append(groupName).append(">(?:").append(regexFor(terminal)).append("))");
            branches.add(new TerminalBranch(groupName, terminal));
        }

        return new MasterPattern(Pattern.compile(pattern.toString()), branches);
    }

    private String regexFor(TerminalSymbol terminal) {
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            return Pattern.quote(terminal.getPattern());
        }
        return terminal.getPattern();
    }

    static final class TerminalOrder implements Comparator<TerminalSymbol> {
        @Override
        public int compare(TerminalSymbol left, TerminalSymbol right) {
            int priority = Integer.compare(right.getPriority(), left.getPriority());
            if (priority != 0) {
                return priority;
            }

            int category = Integer.compare(categoryRank(left), categoryRank(right));
            if (category != 0) {
                return category;
            }

            int fixedLength = Integer.compare(fixedTextLength(right), fixedTextLength(left));
            if (fixedLength != 0) {
                return fixedLength;
            }

            return left.getType().getName().compareTo(right.getType().getName());
        }

        private int categoryRank(TerminalSymbol terminal) {
            if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
                return 2;
            }
            return 1;
        }

        private int fixedTextLength(TerminalSymbol terminal) {
            if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
                return terminal.getPattern().length();
            }
            return 0;
        }
    }
}
