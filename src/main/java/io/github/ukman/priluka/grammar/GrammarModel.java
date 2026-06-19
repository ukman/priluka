package io.github.ukman.priluka.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal grammar representation exposed read-only for diagnostics.
 */
public final class GrammarModel {
    private final NonterminalSymbol start;
    private final List<NonterminalSymbol> nonterminals;
    private final List<TerminalSymbol> terminals;

    public GrammarModel(
        NonterminalSymbol start,
        List<NonterminalSymbol> nonterminals,
        List<TerminalSymbol> terminals
    ) {
        this.start = start;
        this.nonterminals = Collections.unmodifiableList(new ArrayList<NonterminalSymbol>(nonterminals));
        this.terminals = Collections.unmodifiableList(new ArrayList<TerminalSymbol>(terminals));
    }

    public NonterminalSymbol getStart() {
        return start;
    }

    public List<NonterminalSymbol> getNonterminals() {
        return nonterminals;
    }

    public List<TerminalSymbol> getTerminals() {
        return terminals;
    }

    public String toBnf() {
        StringBuilder builder = new StringBuilder();
        for (NonterminalSymbol nonterminal : nonterminals) {
            for (Production production : nonterminal.getProductions()) {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(production.toBnf());
            }
        }
        return builder.toString();
    }
}
