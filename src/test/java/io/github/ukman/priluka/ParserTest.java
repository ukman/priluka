package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {
    @Test
    void parsesConstructorProductionWithBuiltInTerminals() {
        Point point = Parser.parse(Point.class, "456 78");

        assertEquals(Integer.valueOf(456), point.x);
        assertEquals(Integer.valueOf(78), point.y);
    }

    @Test
    void rejectsUnconsumedTokens() {
        assertThrows(ParseException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                Parser.parse(Point.class, "456 78 90");
            }
        });
    }

    @Test
    void returnsParseTraceWithConstructedValue() {
        ParseTraceResult<Point> result = Parser.trace(Point.class, "456 78");

        assertEquals(Integer.valueOf(456), result.getValue().x);
        assertEquals(Integer.valueOf(78), result.getValue().y);

        List<ParseTraceEvent> events = result.getTrace().getEvents();
        assertEquals(ParseTraceEvent.Kind.BEGIN_PRODUCTION, events.get(0).getKind());
        assertEquals("Point => Integer Integer", events.get(0).getProduction().toBnf());
        assertEquals(ParseTraceEvent.Kind.CONSUME_TERMINAL, events.get(1).getKind());
        assertEquals(Integer.class, events.get(1).getTerminalType());
        assertEquals("456", events.get(1).getText());
        assertEquals(0, events.get(1).getStart());
        assertEquals(3, events.get(1).getLen());
        assertEquals(ParseTraceEvent.Kind.CONSUME_TERMINAL, events.get(2).getKind());
        assertEquals("78", events.get(2).getText());
        assertEquals(ParseTraceEvent.Kind.END_PRODUCTION, events.get(3).getKind());
        assertEquals(
            "beginProduction(Point => Integer Integer)" + System.lineSeparator()
                + "consumeTerminal(Integer, \"456\", start=0, len=3)" + System.lineSeparator()
                + "consumeTerminal(Integer, \"78\", start=4, len=2)" + System.lineSeparator()
                + "endProduction(Point => Integer Integer)",
            result.getTrace().toString()
        );
    }

    @Test
    void parsesInterfaceAlternativeFromInitializedUniverse() {
        Expression expression = Parser
            .init(Expression.class, NumberExpression.class)
            .parse(Expression.class, "42");

        assertInstanceOf(NumberExpression.class, expression);
        assertEquals(Integer.valueOf(42), ((NumberExpression) expression).value);
    }

    @Test
    void parsesAbstractClassAlternativeFromInitializedUniverse() {
        AstNode node = Parser
            .init(AstNode.class, TextNode.class, NumberNode.class)
            .parse(AstNode.class, "number");

        assertInstanceOf(NumberNode.class, node);
    }

    @Test
    void parsesEnumKeywordTerminal() {
        BooleanLiteralExpression expression = Parser
            .parse(BooleanLiteralExpression.class, "true");

        assertEquals(BooleanLiteral.TRUE, expression.literal);
    }

    @Test
    void parsesDeepRightRecursiveGrammarWithoutJvmStackOverflow() {
        DeepGrammar.Start start = Parser
            .initFromOuterClass(DeepGrammar.class)
            .parse(DeepGrammar.Start.class, repeatedAThenZ(1500));

        assertNotNull(start);
    }

    static class Point {
        final Integer x;
        final Integer y;

        Point(Integer x, Integer y) {
            this.x = x;
            this.y = y;
        }
    }

    interface Expression {
    }

    static class NumberExpression implements Expression {
        final Integer value;

        NumberExpression(Integer value) {
            this.value = value;
        }
    }

    abstract static class AstNode {
    }

    static class TextNode extends AstNode {
        final TextKeyword text;

        TextNode(TextKeyword text) {
            this.text = text;
        }
    }

    static class NumberNode extends AstNode {
        final NumberKeyword number;

        NumberNode(NumberKeyword number) {
            this.number = number;
        }
    }

    @Keyword("text")
    static class TextKeyword {
    }

    @Keyword("number")
    static class NumberKeyword {
    }

    static class BooleanLiteralExpression {
        final BooleanLiteral literal;

        BooleanLiteralExpression(BooleanLiteral literal) {
            this.literal = literal;
        }
    }

    @Keywords
    enum BooleanLiteral {
        FALSE,
        TRUE
    }

    static final class DeepGrammar {
        static class Start {
            final Tail tail;

            Start(Tail tail) {
                this.tail = tail;
            }
        }

        interface Tail {
        }

        static class MoreTail implements Tail {
            final A a;
            final Tail tail;

            MoreTail(A a, Tail tail) {
                this.a = a;
                this.tail = tail;
            }
        }

        static class LastTail implements Tail {
            final Z z;

            LastTail(Z z) {
                this.z = z;
            }
        }

        @Keyword("a")
        static class A {
        }

        @Keyword("z")
        static class Z {
        }
    }

    private static String repeatedAThenZ(int count) {
        StringBuilder result = new StringBuilder(count * 2 + 1);
        for (int i = 0; i < count; i++) {
            result.append("a ");
        }
        result.append("z");
        return result.toString();
    }

    interface ThrowingRunnable extends org.junit.jupiter.api.function.Executable {
    }
}
