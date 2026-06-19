package io.github.ukman.priluka.grammar;

import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrammarModelBuilderTest {

    @Test
    void describesConstructorAsProduction() {
        GrammarModel model = Parser.describe(Point.class);

        assertEquals("Point => Integer Integer", model.toBnf());
    }

    @Test
    void describesMultipleConstructorsAsAlternatives() {
        GrammarModel model = Parser.describe(PrimaryExpression.class);

        assertEquals(
            "PrimaryExpression => Integer" + System.lineSeparator()
                + "PrimaryExpression => OpenBracket Expression CloseBracket" + System.lineSeparator()
                + "Expression => Integer",
            model.toBnf()
        );
    }

    @Test
    void describesInterfaceImplementationsAsAlternativesInsideUniverse() {
        GrammarModel model = Parser
            .init(Statement.class, IfStatement.class, ForStatement.class)
            .describe(Statement.class);

        assertEquals(
            "Statement => ForStatement" + System.lineSeparator()
                + "Statement => IfStatement" + System.lineSeparator()
                + "ForStatement => empty" + System.lineSeparator()
                + "IfStatement => empty",
            model.toBnf()
        );
    }

    @Test
    void describesSeparatedRepetition() {
        GrammarModel model = Parser.describe(NumberArray.class);

        assertEquals("NumberArray => (empty | Integer (Comma Integer)*)", model.toBnf());
    }

    @Test
    void describesOneOrMoreSeparatedRepetitionWithTrailingSeparator() {
        GrammarModel model = Parser.describe(NonEmptyNumberArray.class);

        assertEquals("NonEmptyNumberArray => (Integer (Comma Integer)* Comma?)", model.toBnf());
    }

    static class Point {
        public Point(Integer x, Integer y) {
        }
    }

    static class PrimaryExpression {
        public PrimaryExpression(Integer number) {
        }

        public PrimaryExpression(OpenBracket openBracket, Expression expression, CloseBracket closeBracket) {
        }
    }

    static class Expression {
        public Expression(Integer number) {
        }
    }

    @Keyword("(")
    static class OpenBracket {
    }

    @Keyword(")")
    static class CloseBracket {
    }

    interface Statement {
    }

    static class IfStatement implements Statement {
    }

    static class ForStatement implements Statement {
    }

    @Terminal(regexp = ",")
    static class Comma {
    }

    static class NumberArray {
        public NumberArray(@Separator(Comma.class) Integer[] numbers) {
        }
    }

    static class NonEmptyNumberArray {
        public NonEmptyNumberArray(
            @OneOrMore @Separator(value = Comma.class, trailing = true) Integer[] numbers
        ) {
        }
    }
}
