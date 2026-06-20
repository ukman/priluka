package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.grammar.PredictionTable;
import io.github.ukman.priluka.internal.lexer.Lexeme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class GrammarPredictor {
    private final PredictionTable predictionTable;

    GrammarPredictor(GrammarModel model) {
        this.predictionTable = new PredictionTable(model);
    }

    List<Production> predict(NonterminalSymbol nonterminal, List<Lexeme> lexemes, int position) {
        if (position >= lexemes.size()) {
            return predictionTable.lookup(nonterminal, PredictionTable.END);
        }

        List<TerminalSymbol> terminalTypes = lexemes.get(position).getTerminalTypes();
        if (terminalTypes.size() == 1) {
            return predictionTable.lookup(nonterminal, terminalTypes.get(0).getType());
        }

        Set<Production> candidates = new LinkedHashSet<Production>();
        for (TerminalSymbol terminal : terminalTypes) {
            candidates.addAll(predictionTable.lookup(nonterminal, terminal.getType()));
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
}
