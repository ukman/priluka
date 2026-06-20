package io.github.ukman.priluka.grammar;

import io.github.ukman.priluka.internal.grammar.PredictionTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PredictionConflictAnalyzer {
    private final GrammarModel model;
    private final PredictionTable predictionTable;

    PredictionConflictAnalyzer(GrammarModel model) {
        this.model = model;
        this.predictionTable = new PredictionTable(model);
    }

    List<PredictionConflict> findConflicts() {
        List<PredictionConflict> conflicts = new ArrayList<PredictionConflict>();
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            Map<Class<?>, List<Production>> table = predictionTable.decisionsFor(nonterminal);
            for (Map.Entry<Class<?>, List<Production>> entry : table.entrySet()) {
                if (entry.getValue().size() > 1) {
                    conflicts.add(new PredictionConflict(
                        nonterminal,
                        entry.getKey(),
                        predictionTable.lookaheadName(entry.getKey()),
                        entry.getValue()
                    ));
                }
            }
        }
        return conflicts;
    }
}
