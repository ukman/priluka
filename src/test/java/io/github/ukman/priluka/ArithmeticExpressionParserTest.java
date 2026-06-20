package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArithmeticExpressionParserTest {
    @Test
    void parsesArithmeticExpressionWithPrecedence() {
        ArithmeticGrammar.Expression expression = Parser
            .initFromOuterClass(ArithmeticGrammar.class)
            .parse(ArithmeticGrammar.Expression.class, "1+2*3-4/2");

        assertEquals(5, expression.calculate());
    }

    @Test
    void parsesParenthesizedArithmeticExpression() {
        ArithmeticGrammar.Expression expression = Parser
            .initFromOuterClass(ArithmeticGrammar.class)
            .parse(ArithmeticGrammar.Expression.class, "(1+2)*(3-4/2)");

        assertEquals(3, expression.calculate());
    }

    @Test
    void ignoresWhitespaceBetweenArithmeticTokens() {
        ArithmeticGrammar.Expression expression = Parser
            .initFromOuterClass(ArithmeticGrammar.class)
            .parse(ArithmeticGrammar.Expression.class, " 10 + 2 * ( 8 - 3 ) ");

        assertEquals(20, expression.calculate());
    }

    @Test
    void parsesUnaryMinus() {
        assertEquals(-1, calculate("-1"));
    }

    @Test
    void parsesUnaryMinusBeforeSubtraction() {
        assertEquals(-2, calculate("-1-1"));
    }

    @Test
    void parsesUnaryMinusBeforeParenthesizedExpression() {
        assertEquals(-1, calculate("-(2-1)"));
    }

    @Test
    void parsesNestedUnaryMinus() {
        assertEquals(1, calculate("-(-1)"));
    }

    @Test
    void reportsArithmeticGrammarAsNfaIncompatible() {
        assertEquals(
            false,
            Parser
                .initFromOuterClass(ArithmeticGrammar.class)
                .describe(ArithmeticGrammar.Expression.class)
                .checkNfaCompatibility()
                .isSupported()
        );
    }

    private int calculate(String input) {
        return Parser
            .initFromOuterClass(ArithmeticGrammar.class)
            .parse(ArithmeticGrammar.Expression.class, input)
            .calculate();
    }

    static final class ArithmeticGrammar {
        static class Expression {
            final Term term;
            final ExpressionTail tail;

            Expression(Term term, ExpressionTail tail) {
                this.term = term;
                this.tail = tail;
            }

            int calculate() {
                return tail.calculate(term.calculate());
            }
        }

        interface ExpressionTail {
            int calculate(int left);
        }

        static class AddExpressionTail implements ExpressionTail {
            final Plus plus;
            final Term term;
            final ExpressionTail tail;

            AddExpressionTail(Plus plus, Term term, ExpressionTail tail) {
                this.plus = plus;
                this.term = term;
                this.tail = tail;
            }

            @Override
            public int calculate(int left) {
                return tail.calculate(left + term.calculate());
            }
        }

        static class SubtractExpressionTail implements ExpressionTail {
            final Minus minus;
            final Term term;
            final ExpressionTail tail;

            SubtractExpressionTail(Minus minus, Term term, ExpressionTail tail) {
                this.minus = minus;
                this.term = term;
                this.tail = tail;
            }

            @Override
            public int calculate(int left) {
                return tail.calculate(left - term.calculate());
            }
        }

        static class ZEmptyExpressionTail implements ExpressionTail {
            ZEmptyExpressionTail() {
            }

            @Override
            public int calculate(int left) {
                return left;
            }
        }

        static class Term {
            final Factor factor;
            final TermTail tail;

            Term(Factor factor, TermTail tail) {
                this.factor = factor;
                this.tail = tail;
            }

            int calculate() {
                return tail.calculate(factor.calculate());
            }
        }

        interface TermTail {
            int calculate(int left);
        }

        static class DivideTermTail implements TermTail {
            final Divide divide;
            final Factor factor;
            final TermTail tail;

            DivideTermTail(Divide divide, Factor factor, TermTail tail) {
                this.divide = divide;
                this.factor = factor;
                this.tail = tail;
            }

            @Override
            public int calculate(int left) {
                return tail.calculate(left / factor.calculate());
            }
        }

        static class MultiplyTermTail implements TermTail {
            final Multiply multiply;
            final Factor factor;
            final TermTail tail;

            MultiplyTermTail(Multiply multiply, Factor factor, TermTail tail) {
                this.multiply = multiply;
                this.factor = factor;
                this.tail = tail;
            }

            @Override
            public int calculate(int left) {
                return tail.calculate(left * factor.calculate());
            }
        }

        static class ZEmptyTermTail implements TermTail {
            ZEmptyTermTail() {
            }

            @Override
            public int calculate(int left) {
                return left;
            }
        }

        interface Factor {
            int calculate();
        }

        static class NegativeFactor implements Factor {
            final Minus minus;
            final Factor factor;

            NegativeFactor(Minus minus, Factor factor) {
                this.minus = minus;
                this.factor = factor;
            }

            @Override
            public int calculate() {
                return -factor.calculate();
            }
        }

        static class NumberFactor implements Factor {
            final Integer value;

            NumberFactor(Integer value) {
                this.value = value;
            }

            @Override
            public int calculate() {
                return value.intValue();
            }
        }

        static class ParenthesizedFactor implements Factor {
            final OpenParen openParen;
            final Expression expression;
            final CloseParen closeParen;

            ParenthesizedFactor(OpenParen openParen, Expression expression, CloseParen closeParen) {
                this.openParen = openParen;
                this.expression = expression;
                this.closeParen = closeParen;
            }

            @Override
            public int calculate() {
                return expression.calculate();
            }
        }

        @Keyword("+")
        static class Plus {
        }

        @Keyword("-")
        static class Minus {
        }

        @Keyword("*")
        static class Multiply {
        }

        @Keyword("/")
        static class Divide {
        }

        @Keyword("(")
        static class OpenParen {
        }

        @Keyword(")")
        static class CloseParen {
        }
    }
}
