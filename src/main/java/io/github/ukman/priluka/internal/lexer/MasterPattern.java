package io.github.ukman.priluka.internal.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class MasterPattern {
    private final Pattern pattern;
    private final List<TerminalBranch> branches;

    MasterPattern(Pattern pattern, List<TerminalBranch> branches) {
        this.pattern = pattern;
        this.branches = Collections.unmodifiableList(new ArrayList<TerminalBranch>(branches));
    }

    public Pattern getPattern() {
        return pattern;
    }

    public List<TerminalBranch> getBranches() {
        return branches;
    }

    public TerminalBranch getBranchByGroupName(String groupName) {
        for (TerminalBranch branch : branches) {
            if (branch.getGroupName().equals(groupName)) {
                return branch;
            }
        }
        return null;
    }

    public TerminalBranch getMatchedBranch(java.util.regex.Matcher matcher) {
        for (TerminalBranch branch : branches) {
            if (matcher.group(branch.getGroupName()) != null) {
                return branch;
            }
        }
        return null;
    }
}
