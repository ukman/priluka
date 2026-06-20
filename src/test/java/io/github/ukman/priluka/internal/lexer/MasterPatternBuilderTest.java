package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterPatternBuilderTest {

    @Test
    void ordersRegexpBeforeKeywordSoIdentifierCanEatIfx() {
        MasterPattern pattern = build(
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", 0)
        );

        assertEquals(Id.class, pattern.getBranches().get(0).getTerminal().getType());
        assertEquals(If.class, pattern.getBranches().get(1).getTerminal().getType());
        Matcher matcher = pattern.getPattern().matcher("ifx");
        assertTrue(matcher.lookingAt());
        assertEquals("ifx", matcher.group());
    }

    @Test
    void priorityOverridesCategoryOrdering() {
        MasterPattern pattern = build(
            keyword(If.class, "if", 100),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", 0)
        );

        assertEquals(If.class, pattern.getBranches().get(0).getTerminal().getType());
        assertEquals(Id.class, pattern.getBranches().get(1).getTerminal().getType());
    }

    @Test
    void ordersLongerKeywordsBeforeShorterKeywords() {
        MasterPattern pattern = build(
            keyword(Else.class, "else", 0),
            keyword(ElseIf.class, "else if", 0)
        );

        assertEquals(ElseIf.class, pattern.getBranches().get(0).getTerminal().getType());
        assertEquals(Else.class, pattern.getBranches().get(1).getTerminal().getType());
    }

    @Test
    void lexemeModelKeepsSpanTypesAndSkipFlag() {
        Lexeme lexeme = new Lexeme(
            3,
            2,
            "if",
            Arrays.asList(keyword(If.class, "if", 0), regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", 0)),
            true
        );

        assertEquals(3, lexeme.getStart());
        assertEquals(2, lexeme.getLen());
        assertEquals("if", lexeme.getText());
        assertTrue(lexeme.hasTerminal(If.class));
        assertTrue(lexeme.hasTerminal(Id.class));
        assertFalse(lexeme.hasTerminal(Else.class));
        assertTrue(lexeme.isSkipped());
    }

    private MasterPattern build(TerminalSymbol... terminals) {
        return new MasterPatternBuilder().build(new LexerSpec(Arrays.asList(terminals)));
    }

    private TerminalSymbol regexp(Class<?> type, String pattern, int priority) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.REGEXP, pattern, false, priority);
    }

    private TerminalSymbol keyword(Class<?> type, String text, int priority) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.KEYWORD, text, false, priority);
    }

    static class If {
    }

    static class Id {
    }

    static class Else {
    }

    static class ElseIf {
    }

}
