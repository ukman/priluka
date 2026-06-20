package io.github.ukman.priluka.grammar;

import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
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
    void describesAbstractClassImplementationsAsAlternativesInsideUniverse() {
        GrammarModel model = Parser
            .init(AstNode.class, TextNode.class, NumberNode.class)
            .describe(AstNode.class);

        assertEquals(
            "AstNode => NumberNode" + System.lineSeparator()
                + "AstNode => TextNode" + System.lineSeparator()
                + "NumberNode => NumberKeyword" + System.lineSeparator()
                + "TextNode => TextKeyword",
            model.toBnf()
        );
    }

    @Test
    void describesEnumKeywordsAsTerminal() {
        GrammarModel model = Parser.describe(BooleanLiteralExpression.class);

        assertEquals("BooleanLiteralExpression => BooleanLiteral", model.toBnf());
        assertEquals("false|true", model.getTerminals().get(0).getPattern());
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

    @Test
    void terminalSymbolKeepsLexerPriority() {
        GrammarModel model = Parser.describe(IdentifierExpression.class);

        assertEquals(13, model.getTerminals().get(0).getPriority());
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

    abstract static class AstNode {
    }

    static class TextNode extends AstNode {
        public TextNode(TextKeyword text) {
        }
    }

    static class NumberNode extends AstNode {
        public NumberNode(NumberKeyword number) {
        }
    }

    @Keyword("text")
    static class TextKeyword {
    }

    @Keyword("number")
    static class NumberKeyword {
    }

    static class BooleanLiteralExpression {
        public BooleanLiteralExpression(BooleanLiteral literal) {
        }
    }

    @Keywords
    enum BooleanLiteral {
        FALSE,
        TRUE
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

    static class IdentifierExpression {
        public IdentifierExpression(Id id) {
        }
    }

    @Terminal(regexp = "[A-Za-z_][A-Za-z0-9_]*", priority = 13)
    static class Id {
    }
}
