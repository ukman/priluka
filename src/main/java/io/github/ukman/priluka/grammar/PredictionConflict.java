package io.github.ukman.priluka.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PredictionConflict {
    private final NonterminalSymbol nonterminal;
    private final Class<?> lookaheadType;
    private final String lookaheadName;
    private final List<Production> productions;

    PredictionConflict(
        NonterminalSymbol nonterminal,
        Class<?> lookaheadType,
        String lookaheadName,
        List<Production> productions
    ) {
        this.nonterminal = nonterminal;
        this.lookaheadType = lookaheadType;
        this.lookaheadName = lookaheadName;
        this.productions = Collections.unmodifiableList(new ArrayList<Production>(productions));
    }

    public NonterminalSymbol getNonterminal() {
        return nonterminal;
    }

    public Class<?> getLookaheadType() {
        return lookaheadType;
    }

    public String getLookaheadName() {
        return lookaheadName;
    }

    public List<Production> getProductions() {
        return productions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(nonterminal.getName())
            .append(" conflicts on ")
            .append(lookaheadName)
            .append(": ");
        for (int i = 0; i < productions.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(productions.get(i).toBnf());
        }
        return builder.toString();
    }
}
