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
        int nextGroupIndex = 1;
        for (int i = 0; i < terminals.size(); i++) {
            TerminalSymbol terminal = terminals.get(i);
            String groupName = "T" + i;
            int groupIndex = nextGroupIndex;
            String regex = regexFor(terminal);
            if (pattern.length() > 0) {
                pattern.append('|');
            }
            pattern.append("((?:").append(regex).append("))");
            branches.add(new TerminalBranch(groupName, groupIndex, terminal));
            nextGroupIndex += 1 + countCapturingGroups(regex);
        }

        return new MasterPattern(Pattern.compile(pattern.toString()), branches);
    }

    private String regexFor(TerminalSymbol terminal) {
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            String quoted = Pattern.quote(terminal.getPattern());
            if (!terminal.isCaseSensitive()) {
                return "(?iu:" + quoted + ")";
            }
            return quoted;
        }
        return terminal.getPattern();
    }

    private int countCapturingGroups(String regex) {
        int count = 0;
        boolean escaped = false;
        boolean inCharacterClass = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '[') {
                inCharacterClass = true;
                continue;
            }
            if (c == ']') {
                inCharacterClass = false;
                continue;
            }
            if (inCharacterClass || c != '(') {
                continue;
            }
            if (i + 1 < regex.length() && regex.charAt(i + 1) == '?') {
                if (isNamedCapturingGroup(regex, i)) {
                    count++;
                }
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean isNamedCapturingGroup(String regex, int groupStart) {
        if (groupStart + 3 >= regex.length()) {
            return false;
        }
        if (regex.charAt(groupStart + 1) != '?' || regex.charAt(groupStart + 2) != '<') {
            return false;
        }
        char firstNameChar = regex.charAt(groupStart + 3);
        return firstNameChar != '=' && firstNameChar != '!';
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
