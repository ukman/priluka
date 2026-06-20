package io.github.ukman.priluka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    void parsesInterfaceAlternativeFromInitializedUniverse() {
        Expression expression = Parser
            .init(Expression.class, NumberExpression.class)
            .parse(Expression.class, "42");

        assertInstanceOf(NumberExpression.class, expression);
        assertEquals(Integer.valueOf(42), ((NumberExpression) expression).value);
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

    interface ThrowingRunnable extends org.junit.jupiter.api.function.Executable {
    }
}
