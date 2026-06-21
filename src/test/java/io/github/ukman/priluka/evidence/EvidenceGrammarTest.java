package io.github.ukman.priluka.evidence;

import io.github.ukman.priluka.FindEngine;
import io.github.ukman.priluka.LexerEngine;
import io.github.ukman.priluka.ParseFindResult;
import io.github.ukman.priluka.Parser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvidenceGrammarTest {

    @Test
    void dateGrammarFindsDatesAndRanges() {
        Parser.InitializedParser parser = Parser
            .builder()
            .classes(grammarClasses(DateGrammar.class))
            .terminals(DateGrammar.Word.class, DateGrammar.Other.class)
            .caseInsensitive()
            .engine(LexerEngine.ASCII_TEXT)
            .findEngine(FindEngine.NFA)
            .build();

        List<ParseFindResult<DateGrammar.DateEvidence>> results = parser.findAll(
            DateGrammar.DateEvidence.class,
            "Initial period 1 April 2026 to 31 March 2027, then Oct. 25."
        );

        assertEquals(2, results.size());
        assertEquals("date-range", results.get(0).getValue().kind());
        assertEquals("month-year", results.get(1).getValue().kind());
    }

    @Test
    void moneyGrammarFindsAmountsRangesAndPercentages() {
        Parser.InitializedParser parser = Parser
            .builder()
            .classes(grammarClasses(MoneyGrammar.class))
            .terminals(MoneyGrammar.Word.class, MoneyGrammar.NumberToken.class, MoneyGrammar.Symbol.class)
            .caseInsensitive()
            .engine(LexerEngine.ASCII_TEXT)
            .findEngine(FindEngine.NFA)
            .build();

        List<ParseFindResult<MoneyGrammar.Money>> results = parser.findAll(
            MoneyGrammar.Money.class,
            "Public Liability Insurance £10,000,000; Lot 1 £8m to £18m; Quality 50%."
        );

        assertEquals(3, results.size());
        assertEquals("insurance-limit", results.get(0).getValue().kind());
        assertEquals("money-range", results.get(1).getValue().kind());
        assertEquals("percentage", results.get(2).getValue().kind());
    }

    @Test
    void moneyGrammarFindsAdditionalCurrencies() {
        Parser.InitializedParser parser = Parser
            .builder()
            .classes(grammarClasses(MoneyGrammar.class))
            .terminals(MoneyGrammar.Word.class, MoneyGrammar.NumberToken.class, MoneyGrammar.Symbol.class)
            .caseInsensitive()
            .engine(LexerEngine.ASCII_TEXT)
            .findEngine(FindEngine.NFA)
            .build();

        List<ParseFindResult<MoneyGrammar.Money>> results = parser.findAll(
            MoneyGrammar.Money.class,
            "CAD 100; 200 Canadian dollars; AUD 300; 400 Australian dollars; "
                + "JPY 5000; ¥6000; 700 Swiss francs; CHF 800; SAR 900; 1000 Saudi riyals; "
                + "1200 DKK; 1300 Norwegian kroner; 1400 PLN; 1500 zloty; 1600 CZK; "
                + "1700 koruna; 1800 HUF; 1900 forint; 2000 RON; 2100 lei; 2200 BGN; 2300 leva"
        );

        assertEquals(22, results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals("money-amount", results.get(i).getValue().kind());
        }
    }

    private static Class<?>[] grammarClasses(Class<?> root) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        collect(root, classes);
        return classes.toArray(new Class<?>[0]);
    }

    private static void collect(Class<?> type, List<Class<?>> classes) {
        classes.add(type);
        Class<?>[] nested = type.getDeclaredClasses();
        for (int i = 0; i < nested.length; i++) {
            collect(nested[i], classes);
        }
    }
}
