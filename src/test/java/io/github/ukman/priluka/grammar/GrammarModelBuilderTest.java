package io.github.ukman.priluka.grammar;

import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void describesTerminalImplementationsAsInterfaceAlternativesInsideUniverse() {
        GrammarModel model = Parser
            .init(Sign.class, Plus.class, Minus.class)
            .describe(Sign.class);

        assertEquals(
            "Sign => Minus" + System.lineSeparator()
                + "Sign => Plus",
            model.toBnf()
        );
        assertEquals(2, model.getTerminals().size());
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
    void describesSeparatedCollectionRepetition() {
        GrammarModel model = Parser.describe(NumberList.class);

        assertEquals("NumberList => (empty | Integer (Comma Integer)*)", model.toBnf());
    }

    @Test
    void describesOptionalParameter() {
        GrammarModel model = Parser.describe(SignedNumber.class);

        assertEquals("SignedNumber => Minus? Integer", model.toBnf());
    }

    @Test
    void reportsSimpleConstructorGrammarAsNfaCompatible() {
        GrammarModel model = Parser.describe(Point.class);

        assertEquals(true, model.checkNfaCompatibility().isSupported());
    }

    @Test
    void reportsSeparatedListGrammarAsNfaCompatible() {
        GrammarModel model = Parser.describe(NumberArray.class);

        assertEquals(true, model.checkNfaCompatibility().isSupported());
    }

    @Test
    void reportsOptionalGrammarAsNfaCompatible() {
        GrammarModel model = Parser.describe(SignedNumber.class);

        assertEquals(true, model.checkNfaCompatibility().isSupported());
    }

    @Test
    void reportsRecursiveGrammarAsNfaIncompatible() {
        GrammarModel model = Parser.describe(SelfRecursive.class);

        NfaCompatibility compatibility = model.checkNfaCompatibility();

        assertEquals(false, compatibility.isSupported());
        assertEquals(1, compatibility.getReasons().size());
        assertEquals(
            "Recursive nonterminal cycle is outside NFA v1 subset: SelfRecursive -> SelfRecursive",
            compatibility.getReasons().get(0)
        );
    }

    @Test
    void terminalSymbolKeepsLexerPriority() {
        GrammarModel model = Parser.describe(IdentifierExpression.class);

        assertEquals(13, model.getTerminals().get(0).getPriority());
    }

    @Test
    void reportsPredictionConflictForSameLookaheadAlternatives() {
        GrammarModel model = Parser
            .init(ConflictingValue.class, FirstIntegerValue.class, SecondIntegerValue.class)
            .describe(ConflictingValue.class);

        List<PredictionConflict> conflicts = model.findPredictionConflicts();

        assertEquals(1, conflicts.size());
        assertEquals("ConflictingValue", conflicts.get(0).getNonterminal().getName());
        assertEquals("Integer", conflicts.get(0).getLookaheadName());
        assertEquals(2, conflicts.get(0).getProductions().size());
        assertEquals(
            "ConflictingValue conflicts on Integer: "
                + "ConflictingValue => FirstIntegerValue | ConflictingValue => SecondIntegerValue",
            conflicts.get(0).toString()
        );
    }

    @Test
    void reportsNoPredictionConflictForDistinctLookaheadAlternatives() {
        GrammarModel model = Parser
            .init(DistinctValue.class, IntegerValue.class, TextValue.class)
            .describe(DistinctValue.class);

        assertTrue(model.findPredictionConflicts().isEmpty());
    }

    @Test
    void reportsPredictionConflictBetweenNullableProductionAndFollow() {
        GrammarModel model = Parser
            .init(NullableConflictStart.class, NullableTail.class, CommaTail.class, EmptyTail.class, Comma.class)
            .describe(NullableConflictStart.class);

        List<PredictionConflict> conflicts = model.findPredictionConflicts();

        assertEquals(1, conflicts.size());
        assertEquals("NullableTail", conflicts.get(0).getNonterminal().getName());
        assertEquals("Comma", conflicts.get(0).getLookaheadName());
        assertEquals(2, conflicts.get(0).getProductions().size());
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

    static class NumberList {
        public NumberList(@Separator(Comma.class) List<Integer> numbers) {
        }
    }

    static class SignedNumber {
        public SignedNumber(Optional<Minus> minus, Integer number) {
        }
    }

    static class SelfRecursive {
        public SelfRecursive(SelfRecursive next) {
        }
    }

    @Keyword("-")
    static class Minus implements Sign {
    }

    interface Sign {
    }

    @Keyword("+")
    static class Plus implements Sign {
    }

    static class IdentifierExpression {
        public IdentifierExpression(Id id) {
        }
    }

    @Terminal(regexp = "[A-Za-z_][A-Za-z0-9_]*", priority = 13)
    static class Id {
    }

    interface ConflictingValue {
    }

    static class FirstIntegerValue implements ConflictingValue {
        public FirstIntegerValue(Integer value) {
        }
    }

    static class SecondIntegerValue implements ConflictingValue {
        public SecondIntegerValue(Integer value) {
        }
    }

    interface DistinctValue {
    }

    static class IntegerValue implements DistinctValue {
        public IntegerValue(Integer value) {
        }
    }

    static class TextValue implements DistinctValue {
        public TextValue(TextKeyword text) {
        }
    }

    static class NullableConflictStart {
        public NullableConflictStart(NullableTail tail, Comma comma) {
        }
    }

    interface NullableTail {
    }

    static class CommaTail implements NullableTail {
        public CommaTail(Comma comma) {
        }
    }

    static class EmptyTail implements NullableTail {
    }
}
