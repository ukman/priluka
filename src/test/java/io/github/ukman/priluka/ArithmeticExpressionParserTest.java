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

        assertEquals(5, expression.eval());
    }

    @Test
    void parsesParenthesizedArithmeticExpression() {
        ArithmeticGrammar.Expression expression = Parser
            .initFromOuterClass(ArithmeticGrammar.class)
            .parse(ArithmeticGrammar.Expression.class, "(1+2)*(3-4/2)");

        assertEquals(3, expression.eval());
    }

    @Test
    void ignoresWhitespaceBetweenArithmeticTokens() {
        ArithmeticGrammar.Expression expression = Parser
            .initFromOuterClass(ArithmeticGrammar.class)
            .parse(ArithmeticGrammar.Expression.class, " 10 + 2 * ( 8 - 3 ) ");

        assertEquals(20, expression.eval());
    }

    static final class ArithmeticGrammar {
        static class Expression {
            final Term term;
            final ExpressionTail tail;

            Expression(Term term, ExpressionTail tail) {
                this.term = term;
                this.tail = tail;
            }

            int eval() {
                return tail.apply(term.eval());
            }
        }

        interface ExpressionTail {
            int apply(int left);
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
            public int apply(int left) {
                return tail.apply(left + term.eval());
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
            public int apply(int left) {
                return tail.apply(left - term.eval());
            }
        }

        static class ZEmptyExpressionTail implements ExpressionTail {
            ZEmptyExpressionTail() {
            }

            @Override
            public int apply(int left) {
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

            int eval() {
                return tail.apply(factor.eval());
            }
        }

        interface TermTail {
            int apply(int left);
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
            public int apply(int left) {
                return tail.apply(left / factor.eval());
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
            public int apply(int left) {
                return tail.apply(left * factor.eval());
            }
        }

        static class ZEmptyTermTail implements TermTail {
            ZEmptyTermTail() {
            }

            @Override
            public int apply(int left) {
                return left;
            }
        }

        interface Factor {
            int eval();
        }

        static class NumberFactor implements Factor {
            final Integer value;

            NumberFactor(Integer value) {
                this.value = value;
            }

            @Override
            public int eval() {
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
            public int eval() {
                return expression.eval();
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
