package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.GrammarException;
import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.grammar.GrammarModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfaCompilerTest {
    @Test
    void compilesSimpleConstructorGrammar() {
        NfaGraph graph = compile(Point.class);

        assertEquals(0, graph.getStart().getId());
        assertEquals(1, graph.getAccept().getId());
        assertEquals(1, count(graph, NfaTransition.Kind.BEGIN_PRODUCTION));
        assertEquals(1, count(graph, NfaTransition.Kind.END_PRODUCTION));
        assertEquals(2, count(graph, NfaTransition.Kind.TERMINAL));
        assertEquals(2, countTerminal(graph, Integer.class));
    }

    @Test
    void compilesOptionalPartAsEpsilonAndTerminalAlternatives() {
        NfaGraph graph = compile(SignedNumber.class);

        assertEquals(2, count(graph, NfaTransition.Kind.TERMINAL));
        assertEquals(1, count(graph, NfaTransition.Kind.BEGIN_OPTIONAL));
        assertEquals(1, count(graph, NfaTransition.Kind.END_OPTIONAL_PRESENT));
        assertEquals(1, count(graph, NfaTransition.Kind.END_OPTIONAL_ABSENT));
        assertEquals(1, countTerminal(graph, Minus.class));
        assertEquals(1, countTerminal(graph, Integer.class));
    }

    @Test
    void compilesSeparatedRepetitionWithSeparatorTerminal() {
        NfaGraph graph = compile(NumberArray.class);

        assertEquals(1, count(graph, NfaTransition.Kind.BEGIN_REPEAT));
        assertEquals(2, count(graph, NfaTransition.Kind.END_REPEAT));
        assertEquals(2, count(graph, NfaTransition.Kind.APPEND_REPEAT_ELEMENT));
        assertEquals(2, countTerminal(graph, Integer.class));
        assertEquals(1, countTerminal(graph, Comma.class));
    }

    @Test
    void rejectsRecursiveGrammarBeforeCompilation() {
        GrammarModel model = Parser.describe(SelfRecursive.class);

        GrammarException exception = assertThrows(
            GrammarException.class,
            () -> new NfaCompiler(model).compile()
        );

        assertTrue(exception.getMessage().contains("Recursive nonterminal cycle"));
    }

    private NfaGraph compile(Class<?> start) {
        return new NfaCompiler(Parser.describe(start)).compile();
    }

    private int count(NfaGraph graph, NfaTransition.Kind kind) {
        int count = 0;
        for (NfaTransition transition : graph.getTransitions()) {
            if (transition.getKind() == kind) {
                count++;
            }
        }
        return count;
    }

    private int countTerminal(NfaGraph graph, Class<?> terminalType) {
        int count = 0;
        for (NfaTransition transition : graph.getTransitions()) {
            if (
                transition.getKind() == NfaTransition.Kind.TERMINAL
                    && terminalType.equals(transition.getSymbolType())
            ) {
                count++;
            }
        }
        return count;
    }

    static class Point {
        public Point(Integer x, Integer y) {
        }
    }

    static class SignedNumber {
        public SignedNumber(java.util.Optional<Minus> sign, Integer number) {
        }
    }

    static class NumberArray {
        public NumberArray(@Separator(Comma.class) Integer[] numbers) {
        }
    }

    static class SelfRecursive {
        public SelfRecursive(SelfRecursive next) {
        }
    }

    @Keyword("-")
    static class Minus {
    }

    @Keyword(",")
    static class Comma {
    }
}
