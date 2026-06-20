package io.github.ukman.priluka.internal.grammar;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NfaCompatibility;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NfaCompatibilityChecker {
    private final GrammarModel model;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Set<Class<?>> terminals = new LinkedHashSet<Class<?>>();
    private final Map<Class<?>, Set<Class<?>>> edges = new LinkedHashMap<Class<?>, Set<Class<?>>>();
    private final List<String> reasons = new ArrayList<String>();
    private final Set<Class<?>> visiting = new LinkedHashSet<Class<?>>();
    private final Set<Class<?>> visited = new LinkedHashSet<Class<?>>();

    public NfaCompatibilityChecker(GrammarModel model) {
        this.model = model;
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            nonterminals.put(nonterminal.getType(), nonterminal);
            edges.put(nonterminal.getType(), new LinkedHashSet<Class<?>>());
        }
        for (TerminalSymbol terminal : model.getTerminals()) {
            terminals.add(terminal.getType());
        }
    }

    public NfaCompatibility check() {
        buildGraph();
        detectCycles();
        return new NfaCompatibility(reasons);
    }

    private void buildGraph() {
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            Set<Class<?>> outgoing = edges.get(nonterminal.getType());
            for (Production production : nonterminal.getProductions()) {
                for (ProductionPart part : production.getParts()) {
                    addEdge(outgoing, nonterminal, production, part.getSymbolType());
                    if (part.getSeparatorType() != null) {
                        addEdge(outgoing, nonterminal, production, part.getSeparatorType());
                    }
                }
            }
        }
    }

    private void addEdge(
        Set<Class<?>> outgoing,
        NonterminalSymbol nonterminal,
        Production production,
        Class<?> symbolType
    ) {
        if (terminals.contains(symbolType)) {
            return;
        }
        if (nonterminals.containsKey(symbolType)) {
            outgoing.add(symbolType);
            return;
        }
        reasons.add(
            "Production "
                + production.toBnf()
                + " references symbol outside NFA model: "
                + symbolType.getName()
                + " from "
                + nonterminal.getName()
        );
    }

    private void detectCycles() {
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            if (!visited.contains(nonterminal.getType())) {
                visit(nonterminal.getType(), new ArrayList<Class<?>>());
            }
        }
    }

    private void visit(Class<?> type, List<Class<?>> path) {
        if (visiting.contains(type)) {
            reasons.add("Recursive nonterminal cycle is outside NFA v1 subset: " + cycleText(path, type));
            return;
        }
        if (visited.contains(type)) {
            return;
        }

        visiting.add(type);
        path.add(type);
        Set<Class<?>> outgoing = edges.get(type);
        if (outgoing != null) {
            for (Class<?> target : outgoing) {
                visit(target, path);
            }
        }
        path.remove(path.size() - 1);
        visiting.remove(type);
        visited.add(type);
    }

    private String cycleText(List<Class<?>> path, Class<?> repeated) {
        StringBuilder builder = new StringBuilder();
        boolean inCycle = false;
        for (Class<?> type : path) {
            if (type.equals(repeated)) {
                inCycle = true;
            }
            if (inCycle) {
                if (builder.length() > 0) {
                    builder.append(" -> ");
                }
                builder.append(type.getSimpleName());
            }
        }
        if (builder.length() > 0) {
            builder.append(" -> ");
        }
        builder.append(repeated.getSimpleName());
        return builder.toString();
    }
}
