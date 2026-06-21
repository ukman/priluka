package io.github.ukman.priluka.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Diagnostic result describing whether a grammar model fits the NFA engine subset.
 */
public final class NfaCompatibility {
    private final List<String> reasons;

    public NfaCompatibility(List<String> reasons) {
        this.reasons = Collections.unmodifiableList(new ArrayList<String>(reasons));
    }

    public boolean isSupported() {
        return reasons.isEmpty();
    }

    public List<String> getReasons() {
        return reasons;
    }

    @Override
    public String toString() {
        if (isSupported()) {
            return "NFA-compatible";
        }
        StringBuilder builder = new StringBuilder("NFA-incompatible:");
        for (String reason : reasons) {
            builder.append(System.lineSeparator()).append("- ").append(reason);
        }
        return builder.toString();
    }
}
