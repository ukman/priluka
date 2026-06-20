package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.lexer.Lexeme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GrammarPredictor {
    private static final Class<?> END = EndOfInput.class;

    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Set<Class<?>> terminals = new HashSet<Class<?>>();
    private final Map<Class<?>, Boolean> nullable = new LinkedHashMap<Class<?>, Boolean>();
    private final Map<Class<?>, Set<Class<?>>> first = new LinkedHashMap<Class<?>, Set<Class<?>>>();
    private final Map<Class<?>, Set<Class<?>>> follow = new LinkedHashMap<Class<?>, Set<Class<?>>>();
    private final Map<Production, Set<Class<?>>> productionFirst = new LinkedHashMap<Production, Set<Class<?>>>();
    private final Set<Production> nullableProductions = new HashSet<Production>();
    private final Map<Class<?>, Map<Class<?>, List<Production>>> decisionTable =
        new LinkedHashMap<Class<?>, Map<Class<?>, List<Production>>>();

    GrammarPredictor(GrammarModel model) {
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
        buildDecisionTable();
    }

    List<Production> predict(NonterminalSymbol nonterminal, List<Lexeme> lexemes, int position) {
        Map<Class<?>, List<Production>> table = decisionTable.get(nonterminal.getType());
        if (table == null) {
            return Collections.emptyList();
        }

        if (position >= lexemes.size()) {
            return lookup(table, END);
        }

        List<TerminalSymbol> terminalTypes = lexemes.get(position).getTerminalTypes();
        if (terminalTypes.size() == 1) {
            return lookup(table, terminalTypes.get(0).getType());
        }

        Set<Production> candidates = new LinkedHashSet<Production>();
        for (TerminalSymbol terminal : terminalTypes) {
            candidates.addAll(lookup(table, terminal.getType()));
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Production> result = new ArrayList<Production>();
        for (Production production : nonterminal.getProductions()) {
            if (candidates.contains(production)) {
                result.add(production);
            }
        }

        return result;
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
                List<Production> productions = nonterminal.getProductions();
                for (Production production : productions) {
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

    private void buildDecisionTable() {
        for (NonterminalSymbol nonterminal : nonterminals.values()) {
            Map<Class<?>, List<Production>> table = new LinkedHashMap<Class<?>, List<Production>>();
            for (Production production : nonterminal.getProductions()) {
                addDecisionEntries(table, productionFirst.get(production), production);
                if (nullableProductions.contains(production)) {
                    addDecisionEntries(table, follow.get(nonterminal.getType()), production);
                }
            }
            decisionTable.put(nonterminal.getType(), freeze(table));
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

    private List<Production> lookup(Map<Class<?>, List<Production>> table, Class<?> terminalType) {
        List<Production> productions = table.get(terminalType);
        if (productions == null) {
            return Collections.emptyList();
        }
        return productions;
    }

    private static final class EndOfInput {
    }
}
