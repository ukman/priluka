package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.GrammarException;
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

public final class NfaCompiler {
    private final GrammarModel model;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Set<Class<?>> terminals = new LinkedHashSet<Class<?>>();
    private final List<NfaState> states = new ArrayList<NfaState>();
    private final List<NfaTransition> transitions = new ArrayList<NfaTransition>();

    public NfaCompiler(GrammarModel model) {
        this.model = model;
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            nonterminals.put(nonterminal.getType(), nonterminal);
        }
        for (TerminalSymbol terminal : model.getTerminals()) {
            terminals.add(terminal.getType());
        }
    }

    public NfaGraph compile() {
        NfaCompatibility compatibility = model.checkNfaCompatibility();
        if (!compatibility.isSupported()) {
            throw new GrammarException(compatibility.toString());
        }

        NfaState start = newState();
        NfaState accept = newState();
        compileNonterminal(model.getStart(), start, accept);
        return new NfaGraph(start, accept, states, transitions);
    }

    private void compileNonterminal(NonterminalSymbol nonterminal, NfaState from, NfaState to) {
        for (Production production : nonterminal.getProductions()) {
            NfaState productionStart = newState();
            NfaState productionEnd = newState();
            add(NfaTransition.beginProduction(from, productionStart, production));
            compileSequence(production.getParts(), productionStart, productionEnd);
            add(NfaTransition.endProduction(productionEnd, to, production));
        }
    }

    private void compileSequence(List<ProductionPart> parts, NfaState from, NfaState to) {
        if (parts.isEmpty()) {
            add(NfaTransition.epsilon(from, to));
            return;
        }

        NfaState current = from;
        for (int i = 0; i < parts.size(); i++) {
            NfaState next = i == parts.size() - 1 ? to : newState();
            compilePart(parts.get(i), current, next);
            current = next;
        }
    }

    private void compilePart(ProductionPart part, NfaState from, NfaState to) {
        if (part.getSeparatorType() != null) {
            compileSeparatedPart(part, from, to);
            return;
        }

        if (part.getQuantifier() == ProductionPart.Quantifier.ONE) {
            compileSymbol(part.getSymbolType(), part, from, to);
        } else if (part.getQuantifier() == ProductionPart.Quantifier.OPTIONAL) {
            add(NfaTransition.epsilon(from, to));
            compileSymbol(part.getSymbolType(), part, from, to);
        } else if (part.getQuantifier() == ProductionPart.Quantifier.ZERO_OR_MORE) {
            add(NfaTransition.epsilon(from, to));
            compileRepeatingSymbol(part, from, to);
        } else if (part.getQuantifier() == ProductionPart.Quantifier.ONE_OR_MORE) {
            compileRepeatingSymbol(part, from, to);
        }
    }

    private void compileRepeatingSymbol(ProductionPart part, NfaState from, NfaState to) {
        NfaState itemEnd = newState();
        compileSymbol(part.getSymbolType(), part, from, itemEnd);
        add(NfaTransition.epsilon(itemEnd, from));
        add(NfaTransition.epsilon(itemEnd, to));
    }

    private void compileSeparatedPart(ProductionPart part, NfaState from, NfaState to) {
        if (part.getQuantifier() == ProductionPart.Quantifier.ZERO_OR_MORE) {
            add(NfaTransition.epsilon(from, to));
        }
        compileSeparatedNonEmpty(part, from, to);
    }

    private void compileSeparatedNonEmpty(ProductionPart part, NfaState from, NfaState to) {
        NfaState afterItem = newState();
        compileSymbol(part.getSymbolType(), part, from, afterItem);
        add(NfaTransition.epsilon(afterItem, to));

        NfaState afterSeparator = newState();
        compileSymbol(part.getSeparatorType(), part, afterItem, afterSeparator);
        if (part.isTrailingSeparator()) {
            add(NfaTransition.epsilon(afterSeparator, to));
        }

        NfaState afterNextItem = newState();
        compileSymbol(part.getSymbolType(), part, afterSeparator, afterNextItem);
        add(NfaTransition.epsilon(afterNextItem, afterItem));
    }

    private void compileSymbol(Class<?> symbolType, ProductionPart part, NfaState from, NfaState to) {
        if (terminals.contains(symbolType)) {
            add(NfaTransition.terminal(from, to, symbolType, part));
            return;
        }

        NonterminalSymbol nonterminal = nonterminals.get(symbolType);
        if (nonterminal != null) {
            compileNonterminal(nonterminal, from, to);
            return;
        }

        throw new GrammarException("Unknown NFA symbol: " + symbolType.getName());
    }

    private NfaState newState() {
        NfaState state = new NfaState(states.size());
        states.add(state);
        return state;
    }

    private void add(NfaTransition transition) {
        transitions.add(transition);
    }
}
