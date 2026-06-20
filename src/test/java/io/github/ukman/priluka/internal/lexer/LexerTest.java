package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void tokenizesIntegersAndSkipsWhitespace(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
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

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void keepsSameSpanKeywordAndIdentifierAmbiguity(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("if");

        assertEquals(1, lexemes.size());
        assertEquals("if", lexemes.get(0).getText());
        assertTrue(lexemes.get(0).hasTerminal(If.class));
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
    }

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void strictModeKeepsOnlyMasterBranchTerminal(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
            LexerOptions.STRICT,
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("if");

        assertEquals(1, lexemes.size());
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
        assertEquals(1, lexemes.get(0).getTerminalTypes().size());
    }

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void keepsCaseInsensitiveKeywordAndIdentifierAmbiguity(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
            keyword(If.class, "if", false, 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("IF");

        assertEquals(1, lexemes.size());
        assertEquals("IF", lexemes.get(0).getText());
        assertTrue(lexemes.get(0).hasTerminal(If.class));
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
    }

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void doesNotAddCaseSensitiveKeywordWhenCaseDiffers(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
            keyword(If.class, "if", true, 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("IF");

        assertEquals(1, lexemes.size());
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
        assertEquals(1, lexemes.get(0).getTerminalTypes().size());
    }

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void identifierEatsKeywordPrefix(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("ifx");

        assertEquals(1, lexemes.size());
        assertEquals("ifx", lexemes.get(0).getText());
        assertTrue(lexemes.get(0).hasTerminal(Id.class));
    }

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void tokenizesCommaSeparatedNumbers(LexerFactory factory) {
        Lexer lexer = lexer(
            factory,
            regexp(Integer.class, "[+-]?[0-9]+", false, 0),
            regexp(Comma.class, ",", false, 0)
        );

        List<Lexeme> lexemes = lexer.tokenize("1,2");

        assertEquals(3, lexemes.size());
        assertEquals("1", lexemes.get(0).getText());
        assertEquals(",", lexemes.get(1).getText());
        assertEquals("2", lexemes.get(2).getText());
    }

    @ParameterizedTest
    @MethodSource("lexerFactories")
    void failsOnUnexpectedInput(LexerFactory factory) {
        Lexer lexer = lexer(factory, regexp(Integer.class, "[+-]?[0-9]+", false, 0));

        assertThrows(LexerException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                lexer.tokenize("@");
            }
        });
    }

    @Test
    void javaRegexAndBricsLexersProduceSameLexemes() {
        LexerSpec spec = new LexerSpec(Arrays.asList(
            keyword(If.class, "if", 0),
            regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0),
            regexp(Integer.class, "[+-]?[0-9]+", false, 0),
            regexp(Whitespace.class, "\\s+", true, 0),
            regexp(Comma.class, ",", false, 0)
        ));

        List<Lexeme> javaRegexLexemes = Lexers.javaRegex(spec).tokenize("if ifx, 42");
        List<Lexeme> bricsLexemes = Lexers.brics(spec).tokenize("if ifx, 42");

        assertSameLexemes(javaRegexLexemes, bricsLexemes);
    }

    static Stream<LexerFactory> lexerFactories() {
        return Stream.of(
            new LexerFactory() {
                @Override
                public Lexer create(LexerSpec spec, LexerOptions options) {
                    return Lexers.javaRegex(spec, options);
                }

                @Override
                public String toString() {
                    return "java-regex";
                }
            },
            new LexerFactory() {
                @Override
                public Lexer create(LexerSpec spec, LexerOptions options) {
                    return Lexers.brics(spec, options);
                }

                @Override
                public String toString() {
                    return "brics";
                }
            }
        );
    }

    private Lexer lexer(LexerFactory factory, TerminalSymbol... terminals) {
        return lexer(factory, LexerOptions.DEFAULT, terminals);
    }

    private Lexer lexer(LexerFactory factory, LexerOptions options, TerminalSymbol... terminals) {
        return factory.create(new LexerSpec(Arrays.asList(terminals)), options);
    }

    private void assertSameLexemes(List<Lexeme> expected, List<Lexeme> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getStart(), actual.get(i).getStart());
            assertEquals(expected.get(i).getLen(), actual.get(i).getLen());
            assertEquals(expected.get(i).getText(), actual.get(i).getText());
            assertEquals(expected.get(i).getTerminalTypes().size(), actual.get(i).getTerminalTypes().size());
            for (int j = 0; j < expected.get(i).getTerminalTypes().size(); j++) {
                assertEquals(
                    expected.get(i).getTerminalTypes().get(j).getType(),
                    actual.get(i).getTerminalTypes().get(j).getType()
                );
            }
        }
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

    interface LexerFactory {
        Lexer create(LexerSpec spec, LexerOptions options);
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
