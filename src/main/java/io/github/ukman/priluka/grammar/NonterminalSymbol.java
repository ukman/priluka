package io.github.ukman.priluka.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nonterminal symbol discovered from a Java class, interface, or abstract class.
 */
public final class NonterminalSymbol {
    private final Class<?> type;
    private final List<Production> productions = new ArrayList<Production>();

    public NonterminalSymbol(Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return type.getSimpleName();
    }

    public List<Production> getProductions() {
        return Collections.unmodifiableList(productions);
    }

    public void addProduction(Production production) {
        productions.add(production);
    }
}
