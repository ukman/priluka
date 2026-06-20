package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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

    @Test
    void removesEnumKeywordSetFromMasterTerminalsWhenCoveredByCarrier() {
        KeywordCarrierIndex index = KeywordCarrierIndex.build(Arrays.asList(
            keywordSet(Verb3Form.class, "started|finished", false, "started", "finished"),
            regexp(Word.class, "[A-Za-z]+")
        ));

        assertEquals(1, index.getMasterTerminals().size());
        assertEquals(Word.class, index.getMasterTerminals().get(0).getType());
    }

    private TerminalSymbol regexp(Class<?> type, String pattern) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.REGEXP, pattern, false, 0);
    }

    private TerminalSymbol keyword(Class<?> type, String text, boolean caseSensitive) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.KEYWORD, text, false, 0, caseSensitive);
    }

    private TerminalSymbol keywordSet(Class<?> type, String pattern, boolean caseSensitive, String... texts) {
        return new TerminalSymbol(
            type,
            TerminalSymbol.Kind.REGEXP,
            pattern,
            false,
            0,
            caseSensitive,
            Collections.unmodifiableList(Arrays.asList(texts))
        );
    }

    static class If {
    }

    static class Id {
    }

    static class Word {
    }

    static class Verb3Form {
    }
}
