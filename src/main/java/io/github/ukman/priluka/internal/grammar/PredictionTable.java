package io.github.ukman.priluka.internal.grammar;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PredictionTable {
    public static final Class<?> END = EndOfInput.class;

    private final GrammarModel model;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Set<Class<?>> terminals = new HashSet<Class<?>>();
    private final Map<Class<?>, Boolean> nullable = new LinkedHashMap<Class<?>, Boolean>();
    private final Map<Class<?>, Set<Class<?>>> first = new LinkedHashMap<Class<?>, Set<Class<?>>>();
    private final Map<Class<?>, Set<Class<?>>> follow = new LinkedHashMap<Class<?>, Set<Class<?>>>();
    private final Map<Production, Set<Class<?>>> productionFirst = new LinkedHashMap<Production, Set<Class<?>>>();
    private final Set<Production> nullableProductions = new HashSet<Production>();
    private final Map<Class<?>, Map<Class<?>, List<Production>>> decisions =
        new LinkedHashMap<Class<?>, Map<Class<?>, List<Production>>>();

    public PredictionTable(GrammarModel model) {
        this.model = model;
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            nonterminals.put(nonterminal.getType(), nonterminal);
            nullable.put(nonterminal.getType(), Boolean.FALSE);
            first.put(nonterminal.getType(), new LinkedHashSet<Class<?>>());
            follow.put(nonterminal.getType(), new LinkedHashSet<Class<?>>());
        }
        for (TerminalSymbol terminal : model.getTerminals()) {
            terminals.add(terminal.getType());
        }
        follow.get(model.getStart().getType()).add(END);
        computeNullable();
        computeFirst();
        computeProductionFirst();
        computeFollow();
        buildDecisions();
    }

    public Map<Class<?>, List<Production>> decisionsFor(NonterminalSymbol nonterminal) {
        Map<Class<?>, List<Production>> table = decisions.get(nonterminal.getType());
        if (table == null) {
            return Collections.emptyMap();
        }
        return table;
    }

    public List<Production> lookup(NonterminalSymbol nonterminal, Class<?> lookaheadType) {
        List<Production> productions = decisionsFor(nonterminal).get(lookaheadType);
        if (productions == null) {
            return Collections.emptyList();
        }
        return productions;
    }

    public String lookaheadName(Class<?> type) {
        if (END.equals(type)) {
            return "$";
        }
        return type.getSimpleName();
    }

    private void buildDecisions() {
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            Map<Class<?>, List<Production>> table = new LinkedHashMap<Class<?>, List<Production>>();
            for (Production production : nonterminal.getProductions()) {
                addDecisionEntries(table, productionFirst.get(production), production);
                if (nullableProductions.contains(production)) {
                    addDecisionEntries(table, follow.get(nonterminal.getType()), production);
                }
            }
            decisions.put(nonterminal.getType(), freeze(table));
        }
    }

    private void addDecisionEntries(
        Map<Class<?>, List<Production>> table,
        Set<Class<?>> lookaheadTypes,
        Production production
    ) {
        if (lookaheadTypes == null) {
            return;
        }
        for (Class<?> lookaheadType : lookaheadTypes) {
            List<Production> productions = table.get(lookaheadType);
            if (productions == null) {
                productions = new ArrayList<Production>();
                table.put(lookaheadType, productions);
            }
            if (!productions.contains(production)) {
                productions.add(production);
            }
        }
    }

    private Map<Class<?>, List<Production>> freeze(Map<Class<?>, List<Production>> table) {
        Map<Class<?>, List<Production>> result = new LinkedHashMap<Class<?>, List<Production>>();
        for (Map.Entry<Class<?>, List<Production>> entry : table.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<Production>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    private void computeNullable() {
        boolean changed;
        do {
            changed = false;
            for (NonterminalSymbol nonterminal : nonterminals.values()) {
                if (isNullable(nonterminal.getType())) {
                    continue;
                }
                for (Production production : nonterminal.getProductions()) {
                    if (isProductionNullable(production)) {
                        nullable.put(nonterminal.getType(), Boolean.TRUE);
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);
    }

    private void computeFirst() {
        boolean changed;
        do {
            changed = false;
            for (NonterminalSymbol nonterminal : nonterminals.values()) {
                Set<Class<?>> target = first.get(nonterminal.getType());
                for (Production production : nonterminal.getProductions()) {
                    if (addFirstOfParts(target, production.getParts())) {
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    private void computeProductionFirst() {
        for (NonterminalSymbol nonterminal : nonterminals.values()) {
            for (Production production : nonterminal.getProductions()) {
                Set<Class<?>> target = new LinkedHashSet<Class<?>>();
                addFirstOfParts(target, production.getParts());
                productionFirst.put(production, target);
                if (isProductionNullable(production)) {
                    nullableProductions.add(production);
                }
            }
        }
    }

    private void computeFollow() {
        boolean changed;
        do {
            changed = false;
            for (NonterminalSymbol nonterminal : nonterminals.values()) {
                for (Production production : nonterminal.getProductions()) {
                    Set<Class<?>> trailer = new LinkedHashSet<Class<?>>(follow.get(nonterminal.getType()));
                    List<ProductionPart> parts = production.getParts();
                    for (int i = parts.size() - 1; i >= 0; i--) {
                        ProductionPart part = parts.get(i);
                        Class<?> symbolType = part.getSymbolType();
                        if (nonterminals.containsKey(symbolType)) {
                            if (follow.get(symbolType).addAll(trailer)) {
                                changed = true;
                            }
                            if (isNullable(symbolType)) {
                                Set<Class<?>> nextTrailer = new LinkedHashSet<Class<?>>(trailer);
                                nextTrailer.addAll(first.get(symbolType));
                                trailer = nextTrailer;
                            } else {
                                trailer = new LinkedHashSet<Class<?>>(first.get(symbolType));
                            }
                        } else {
                            trailer = firstOfTerminal(symbolType);
                        }
                    }
                }
            }
        } while (changed);
    }

    private boolean addFirstOfParts(Set<Class<?>> target, List<ProductionPart> parts) {
        boolean changed = false;
        for (ProductionPart part : parts) {
            Class<?> symbolType = part.getSymbolType();
            Set<Class<?>> symbolFirst = terminals.contains(symbolType)
                ? firstOfTerminal(symbolType)
                : first.get(symbolType);
            if (symbolFirst != null && target.addAll(symbolFirst)) {
                changed = true;
            }
            if (!isPartNullable(part)) {
                return changed;
            }
        }
        return changed;
    }

    private boolean isProductionNullable(Production production) {
        for (ProductionPart part : production.getParts()) {
            if (!isPartNullable(part)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPartNullable(ProductionPart part) {
        switch (part.getQuantifier()) {
            case OPTIONAL:
            case ZERO_OR_MORE:
                return true;
            case ONE_OR_MORE:
            case ONE:
            default:
                return isNullable(part.getSymbolType());
        }
    }

    private boolean isNullable(Class<?> type) {
        Boolean value = nullable.get(type);
        return value != null && value.booleanValue();
    }

    private Set<Class<?>> firstOfTerminal(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<Class<?>>();
        result.add(type);
        return result;
    }

    private static final class EndOfInput {
    }
}
