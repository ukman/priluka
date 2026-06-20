package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordCarrierIndexTest {

    @Test
    void removesKeywordFromMasterTerminalsWhenCoveredByCarrier() {
        KeywordCarrierIndex index = KeywordCarrierIndex.build(Arrays.asList(
            keyword(If.class, "if", true),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*")
        ));

        assertEquals(1, index.getMasterTerminals().size());
        assertEquals(Id.class, index.getMasterTerminals().get(0).getType());
    }

    @Test
    void keepsKeywordInMasterTerminalsWhenNoCarrierMatchesIt() {
        KeywordCarrierIndex index = KeywordCarrierIndex.build(Arrays.asList(
            keyword(If.class, "if", true),
            regexp(Integer.class, "[0-9]+")
        ));

        assertEquals(2, index.getMasterTerminals().size());
    }

    private TerminalSymbol regexp(Class<?> type, String pattern) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.REGEXP, pattern, false, 0);
    }

    private TerminalSymbol keyword(Class<?> type, String text, boolean caseSensitive) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.KEYWORD, text, false, 0, caseSensitive);
    }

    static class If {
    }

    static class Id {
    }
}
