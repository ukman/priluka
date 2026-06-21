package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import io.github.ukman.priluka.annotation.Terminal;
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
    void rebuildsObjectFromParseTrace() {
        ParseTraceResult<Point> result = Parser.trace(Point.class, "456 78");

        Point rebuilt = Parser.buildFromTrace(Point.class, result.getTrace());

        assertEquals(Integer.valueOf(456), rebuilt.x);
        assertEquals(Integer.valueOf(78), rebuilt.y);
    }

    @Test
    void traceResultValueIsBuiltOnlyFromTraceReplay() {
        TraceBuiltPoint.constructorCalls = 0;

        ParseTraceResult<TraceBuiltPoint> result = Parser.trace(TraceBuiltPoint.class, "1 2");

        assertEquals(Integer.valueOf(1), result.getValue().x);
        assertEquals(Integer.valueOf(2), result.getValue().y);
        assertEquals(1, TraceBuiltPoint.constructorCalls);
    }

    @Test
    void parserUsesNfaCompatibleFastPathThroughPublicApi() {
        ParseTraceResult<FastPathList> result = Parser.trace(FastPathList.class, "1,2,3");

        assertEquals(3, result.getValue().numbers.length);
        assertEquals(Integer.valueOf(1), result.getValue().numbers[0]);
        assertEquals(ParseTraceEvent.Kind.BEGIN_REPEAT, result.getTrace().getEvents().get(1).getKind());
    }

    @Test
    void findsNfaCompatibleGrammarThroughPublicApi() {
        ParseFindResult<CommaInteger> result = Parser.find(CommaInteger.class, "1,2");

        assertEquals(1, result.getStart());
        assertEquals(3, result.getEnd());
        assertEquals(Integer.valueOf(2), result.getValue().value);
    }

    @Test
    void findsWithAdditionalLexerCarrierTerminalThroughPublicApi() {
        ParseFindResult<SmallPerfect> result = Parser
            .builder()
            .classes(SmallPerfect.class, SmallPronoun.class, SmallHaveHas.class, SmallVerb3.class)
            .terminals(SmallWord.class)
            .build()
            .find(SmallPerfect.class, "noise words i have started later");

        assertEquals(12, result.getStart());
        assertEquals(26, result.getEnd());
        assertEquals(SmallPronoun.I, result.getValue().pronoun);
        assertEquals(SmallHaveHas.HAVE, result.getValue().haveHas);
        assertEquals(SmallVerb3.STARTED, result.getValue().verb);
    }

    @Test
    void findsAllWithConfiguredLexerCarrierTerminalThroughPublicApi() {
        List<ParseFindResult<SmallPerfect>> results = Parser
            .builder()
            .classes(SmallPerfect.class, SmallPronoun.class, SmallHaveHas.class, SmallVerb3.class)
            .terminals(SmallWord.class)
            .build()
            .findAll(SmallPerfect.class, "i have started and they have finished");

        assertEquals(2, results.size());
        assertEquals(SmallPronoun.I, results.get(0).getValue().pronoun);
        assertEquals(SmallVerb3.STARTED, results.get(0).getValue().verb);
        assertEquals(SmallPronoun.THEY, results.get(1).getValue().pronoun);
        assertEquals(SmallVerb3.FINISHED, results.get(1).getValue().verb);
    }

    @Test
    void builderCanDisableKeywordCarrierOptimization() {
        ParseFindResult<SmallPerfect> result = Parser
            .builder()
            .classes(SmallPerfect.class, SmallPronoun.class, SmallHaveHas.class, SmallVerb3.class)
            .terminals(SmallWord.class)
            .disableKeywordCarrierOptimization()
            .build()
            .find(SmallPerfect.class, "noise words i have started later");

        assertEquals(12, result.getStart());
        assertEquals(26, result.getEnd());
        assertEquals(SmallPronoun.I, result.getValue().pronoun);
        assertEquals(SmallVerb3.STARTED, result.getValue().verb);
    }

    @Test
    void builderConfiguresSkippedTerminals() {
        NumberWithComments value = Parser
            .builder()
            .skip(Comment.class)
            .build()
            .parse(NumberWithComments.class, "/*before*/ 42 /*after*/");

        assertEquals(Integer.valueOf(42), value.value);
    }

    @Test
    void builderConfiguresJavaRegexLexerEngine() {
        NumberWithComments value = Parser
            .builder()
            .skip(Comment.class)
            .engine(LexerEngine.JAVA_REGEX)
            .build()
            .parse(NumberWithComments.class, "/*before*/ 42");

        assertEquals(Integer.valueOf(42), value.value);
    }

    @Test
    void builderConfiguresBricsLexerEngine() {
        NumberWithComments value = Parser
            .builder()
            .skip(Comment.class)
            .engine(LexerEngine.BRICS)
            .build()
            .parse(NumberWithComments.class, "/*before*/ 42");

        assertEquals(Integer.valueOf(42), value.value);
    }

    @Test
    void builderConfiguresCaseInsensitiveRegexpMode() {
        LowerWordExpression expression = Parser
            .builder()
            .caseInsensitive()
            .engine(LexerEngine.BRICS)
            .build()
            .parse(LowerWordExpression.class, "HELLO");

        assertEquals("HELLO", expression.word.text);
    }

    @Test
    void builderConfiguresCaseInsensitiveRegexpModeForJavaRegexEngine() {
        LowerWordExpression expression = Parser
            .builder()
            .caseInsensitive()
            .engine(LexerEngine.JAVA_REGEX)
            .build()
            .parse(LowerWordExpression.class, "HELLO");

        assertEquals("HELLO", expression.word.text);
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
    void parsesTerminalClassThroughInterfaceNonterminal() {
        OperatorExpression expression = Parser
            .init(OperatorExpression.class, Operator.class, Plus.class, Minus.class)
            .parse(OperatorExpression.class, "-");

        assertInstanceOf(Minus.class, expression.operator);
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
    void parsesCaseInsensitiveEnumKeywordTerminal() {
        CaseInsensitiveKeywordExpression expression = Parser
            .parse(CaseInsensitiveKeywordExpression.class, "StArTeD");

        assertEquals(CaseInsensitiveKeyword.STARTED, expression.keyword);
    }

    @Test
    void parsesDeepRightRecursiveGrammarWithoutJvmStackOverflow() {
        DeepGrammar.Start start = Parser
            .initFromOuterClass(DeepGrammar.class)
            .parse(DeepGrammar.Start.class, repeatedAThenZ(1500));

        assertNotNull(start);
    }

    @Test
    void builderCanLoadClassesFromOuterClass() {
        DeepGrammar.Start start = Parser
            .builder()
            .classesFromOuterClass(DeepGrammar.class)
            .build()
            .parse(DeepGrammar.Start.class, repeatedAThenZ(5));

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

    static class TraceBuiltPoint {
        static int constructorCalls;
        final Integer x;
        final Integer y;

        TraceBuiltPoint(Integer x, Integer y) {
            constructorCalls++;
            this.x = x;
            this.y = y;
        }
    }

    static class FastPathList {
        final Integer[] numbers;

        FastPathList(@io.github.ukman.priluka.annotation.Separator(Comma.class) Integer[] numbers) {
            this.numbers = numbers;
        }
    }

    static class CommaInteger {
        final Integer value;

        CommaInteger(Comma comma, Integer value) {
            this.value = value;
        }
    }

    static class NumberWithComments {
        final Integer value;

        NumberWithComments(Integer value) {
            this.value = value;
        }
    }

    static class LowerWordExpression {
        final LowerWord word;

        LowerWordExpression(LowerWord word) {
            this.word = word;
        }
    }

    @Keyword(",")
    static class Comma {
    }

    @Terminal(regexp = "/\\*[^*]*\\*/")
    static class Comment {
    }

    @Terminal(regexp = "[a-z]+")
    static class LowerWord {
        final String text;

        LowerWord(String text) {
            this.text = text;
        }
    }

    static class SmallPerfect {
        final SmallPronoun pronoun;
        final SmallHaveHas haveHas;
        final SmallVerb3 verb;

        SmallPerfect(SmallPronoun pronoun, SmallHaveHas haveHas, SmallVerb3 verb) {
            this.pronoun = pronoun;
            this.haveHas = haveHas;
            this.verb = verb;
        }
    }

    @Terminal(regexp = "[A-Za-z]+")
    static class SmallWord {
    }

    @Keywords(caseSensitive = false)
    enum SmallPronoun {
        I,
        THEY
    }

    @Keywords(caseSensitive = false)
    enum SmallHaveHas {
        HAVE,
        HAS
    }

    @Keywords(caseSensitive = false)
    enum SmallVerb3 {
        STARTED,
        FINISHED
    }

    interface Expression {
    }

    static class NumberExpression implements Expression {
        final Integer value;

        NumberExpression(Integer value) {
            this.value = value;
        }
    }

    static class OperatorExpression {
        final Operator operator;

        OperatorExpression(Operator operator) {
            this.operator = operator;
        }
    }

    interface Operator {
    }

    @Keyword("+")
    static class Plus implements Operator {
    }

    @Keyword("-")
    static class Minus implements Operator {
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

    static class CaseInsensitiveKeywordExpression {
        final CaseInsensitiveKeyword keyword;

        CaseInsensitiveKeywordExpression(CaseInsensitiveKeyword keyword) {
            this.keyword = keyword;
        }
    }

    @Keywords(caseSensitive = false)
    enum CaseInsensitiveKeyword {
        STARTED,
        FINISHED
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
