package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    @Test
    void tokenizesIntegersAndSkipsWhitespace() {
        Lexer lexer = lexer(
            regexp(Integer.class, "[+-]?[0-9]+", false, 0),
            regexp(Whitespace.class, "\\s+", true, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("1   2");

        assertEquals(2, lexemes.size());
        assertEquals("1", lexemes.get(0).getText());
        assertEquals(0, lexemes.get(0).getStart());
        assertEquals("2", lexemes.get(1).getText());
        assertEquals(4, lexemes.get(1).getStart());
    }

    @Test
    void keepsSameSpanKeywordAndIdentifierAmbiguity() {
        Lexer lexer = lexer(
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("if");

        assertEquals(1, lexemes.size());
        assertEquals("if", lexemes.get(0).getText());
        assertTrue(lexemes.get(0).hasTerminal(If.class));
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
    }

    @Test
    void strictModeKeepsOnlyMasterBranchTerminal() {
        Lexer lexer = lexer(
            LexerOptions.STRICT,
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("if");

        assertEquals(1, lexemes.size());
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
        assertEquals(1, lexemes.get(0).getTerminalTypes().size());
    }

    @Test
    void keepsCaseInsensitiveKeywordAndIdentifierAmbiguity() {
        Lexer lexer = lexer(
            keyword(If.class, "if", false, 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("IF");

        assertEquals(1, lexemes.size());
        assertEquals("IF", lexemes.get(0).getText());
        assertTrue(lexemes.get(0).hasTerminal(If.class));
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
    }

    @Test
    void doesNotAddCaseSensitiveKeywordWhenCaseDiffers() {
        Lexer lexer = lexer(
            keyword(If.class, "if", true, 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("IF");

        assertEquals(1, lexemes.size());
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
        assertEquals(1, lexemes.get(0).getTerminalTypes().size());
    }

    @Test
    void identifierEatsKeywordPrefix() {
        Lexer lexer = lexer(
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("ifx");

        assertEquals(1, lexemes.size());
        assertEquals("ifx", lexemes.get(0).getText());
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
    }

    @Test
    void tokenizesCommaSeparatedNumbers() {
        Lexer lexer = lexer(
            regexp(Integer.class, "[+-]?[0-9]+", false, 0),
            regexp(Comma.class, ",", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("1,2");

        assertEquals(3, lexemes.size());
        assertEquals("1", lexemes.get(0).getText());
        assertEquals(",", lexemes.get(1).getText());
        assertEquals("2", lexemes.get(2).getText());
    }

    @Test
    void failsOnUnexpectedInput() {
        Lexer lexer = lexer(regexp(Integer.class, "[+-]?[0-9]+", false, 0));

        assertThrows(LexerException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                lexer.tokenize("@");
            }
        });
    }

    private Lexer lexer(TerminalSymbol... terminals) {
        return lexer(LexerOptions.DEFAULT, terminals);
    }

    private Lexer lexer(LexerOptions options, TerminalSymbol... terminals) {
        return new Lexer(new LexerSpec(Arrays.asList(terminals)), options);
    }

    private TerminalSymbol regexp(Class<?> type, String pattern, boolean skip, int priority) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.REGEXP, pattern, skip, priority);
    }

    private TerminalSymbol keyword(Class<?> type, String text, int priority) {
        return keyword(type, text, true, priority);
    }

    private TerminalSymbol keyword(Class<?> type, String text, boolean caseSensitive, int priority) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.KEYWORD, text, false, priority, caseSensitive);
    }

    interface ThrowingRunnable extends org.junit.jupiter.api.function.Executable {
    }

    static class Whitespace {
    }

    static class If {
    }

    static class Id {
    }

    static class Comma {
    }
}
