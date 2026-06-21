package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.LexerEngine;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import io.github.ukman.priluka.annotation.Occurrences;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerConfig;
import io.github.ukman.priluka.internal.lexer.LexerSpec;
import io.github.ukman.priluka.internal.lexer.Lexers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NfaRecognizerTest {
    @Test
    void recognizesSimpleConstructorGrammar() {
        NfaRecognizer recognizer = recognizer(Point.class);

        assertEquals(true, recognizer.recognizes("1 2"));
        assertEquals(false, recognizer.recognizes("1"));
        assertEquals(false, recognizer.recognizes("1 2 3"));
    }

    @Test
    void emitsTraceForSimpleConstructorGrammar() {
        NfaRecognizer recognizer = recognizer(Point.class);

        Point point = Parser.buildFromTrace(Point.class, recognizer.parseTrace("1 2"));

        assertEquals(1, point.x);
        assertEquals(2, point.y);
    }

    @Test
    void recognizesOptionalPart() {
        NfaRecognizer recognizer = recognizer(SignedNumber.class);

        assertEquals(true, recognizer.recognizes("1"));
        assertEquals(true, recognizer.recognizes("-1"));
        assertEquals(true, recognizer.recognizes("- 1"));
        assertEquals(false, recognizer.recognizes("-"));
    }

    @Test
    void emitsTraceForOptionalPart() {
        NfaRecognizer recognizer = recognizer(SignedNumber.class);

        SignedNumber unsigned = Parser.buildFromTrace(SignedNumber.class, recognizer.parseTrace("1"));
        SignedNumber signed = Parser.buildFromTrace(SignedNumber.class, recognizer.parseTrace("-1"));

        assertEquals(false, unsigned.sign.isPresent());
        assertEquals(1, unsigned.number);
        assertEquals(true, signed.sign.isPresent());
        assertEquals(1, signed.number);
    }

    @Test
    void recognizesSeparatedRepetition() {
        NfaRecognizer recognizer = recognizer(NumberArray.class);

        assertEquals(true, recognizer.recognizes(""));
        assertEquals(true, recognizer.recognizes("1"));
        assertEquals(true, recognizer.recognizes("1,2,3"));
        assertEquals(false, recognizer.recognizes("1,"));
        assertEquals(false, recognizer.recognizes(",1"));
    }

    @Test
    void emitsTraceForSeparatedRepetition() {
        NfaRecognizer recognizer = recognizer(NumberArray.class);

        NumberArray empty = Parser.buildFromTrace(NumberArray.class, recognizer.parseTrace(""));
        NumberArray numbers = Parser.buildFromTrace(NumberArray.class, recognizer.parseTrace("1,2,3"));

        assertEquals(0, empty.numbers.length);
        assertEquals(3, numbers.numbers.length);
        assertEquals(1, numbers.numbers[0]);
        assertEquals(2, numbers.numbers[1]);
        assertEquals(3, numbers.numbers[2]);
    }

    @Test
    void recognizesConstructorAlternatives() {
        NfaRecognizer recognizer = recognizer(PlusMinus.class);

        assertEquals(true, recognizer.recognizes("+"));
        assertEquals(true, recognizer.recognizes("-"));
        assertEquals(false, recognizer.recognizes("*"));
    }

    @Test
    void returnsNullTraceForRejectedInput() {
        NfaRecognizer recognizer = recognizer(Point.class);

        ParseTrace trace = recognizer.parseTrace("1");

        assertEquals(null, trace);
    }

    @Test
    void findsFirstMatchingSpanInsideTokenStream() {
        NfaRecognizer recognizer = recognizer(PlusNumber.class);

        NfaFindResult result = recognizer.find("1 + 2");
        PlusNumber value = Parser.buildFromTrace(PlusNumber.class, result.getTrace());

        assertEquals(2, result.getStart());
        assertEquals(5, result.getEnd());
        assertEquals(2, value.value);
    }

    @Test
    void returnsNullWhenNoSpanMatches() {
        NfaRecognizer recognizer = recognizer(PlusNumber.class);

        assertEquals(null, recognizer.find("1 2 3"));
    }

    @Test
    void findsAllNonOverlappingMatchingSpansInsideTokenStream() {
        NfaRecognizer recognizer = recognizer(PlusNumber.class);

        List<NfaFindResult> results = recognizer.findAll("1 + 2 3 + 4");

        assertEquals(2, results.size());
        assertEquals(2, results.get(0).getStart());
        assertEquals(5, results.get(0).getEnd());
        assertEquals(8, results.get(1).getStart());
        assertEquals(11, results.get(1).getEnd());
    }

    @Test
    void findsAdjacentNonOverlappingMatchingSpans() {
        NfaRecognizer recognizer = recognizer(PlusNumber.class);

        List<NfaFindResult> results = recognizer.findAll("+1+2");

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).getStart());
        assertEquals(2, results.get(0).getEnd());
        assertEquals(2, results.get(1).getStart());
        assertEquals(4, results.get(1).getEnd());
    }

    @Test
    void findsSpansWithoutBuildingResultTraces() {
        NfaRecognizer recognizer = recognizer(PlusNumber.class);

        List<NfaFindSpan> results = recognizer.findSpans("1 + 2 3 + 4");

        assertEquals(2, results.size());
        assertEquals(2, results.get(0).getStart());
        assertEquals(5, results.get(0).getEnd());
        assertEquals(8, results.get(1).getStart());
        assertEquals(11, results.get(1).getEnd());
    }

    @Test
    void dfaFindsSameSpansAsNfaForAlternativesAndRepetition() {
        GrammarModel model = Parser.describe(NumberArray.class);
        NfaRecognizer nfa = new NfaRecognizer(model);
        DfaFindRecognizer dfa = dfaRecognizer(model);

        List<NfaFindSpan> nfaResults = nfa.findSpans("1,2,3 x 4,5 + 6");
        List<NfaFindSpan> dfaResults = dfa.findSpans("1,2,3 x 4,5 + 6");

        assertEquals(nfaResults.size(), dfaResults.size());
        for (int i = 0; i < nfaResults.size(); i++) {
            assertEquals(nfaResults.get(i).getStart(), dfaResults.get(i).getStart());
            assertEquals(nfaResults.get(i).getEnd(), dfaResults.get(i).getEnd());
        }
    }

    @Test
    void dfaFindStopsAfterBestSpanWhenFollowingTokenCannotContinueMatch() {
        GrammarModel model = Parser.describe(PlusNumber.class);
        NfaRecognizer nfa = new NfaRecognizer(model);
        DfaFindRecognizer dfa = dfaRecognizer(model);

        List<NfaFindSpan> nfaResults = nfa.findSpans("+ 12 34 56");
        List<NfaFindSpan> dfaResults = dfa.findSpans("+ 12 34 56");

        assertEquals(nfaResults.size(), dfaResults.size());
        assertEquals(1, dfaResults.size());
        assertEquals(0, dfaResults.get(0).getStart());
        assertEquals(4, dfaResults.get(0).getEnd());
    }

    @Test
    void dfaFindsSameSpansAsNfaForBoundedRepetition() {
        GrammarModel model = Parser.describe(BoundedNumberArray.class);
        NfaRecognizer nfa = new NfaRecognizer(model);
        DfaFindRecognizer dfa = dfaRecognizer(model);

        List<NfaFindSpan> nfaResults = nfa.findSpans("1 2 3 4 5 6");
        List<NfaFindSpan> dfaResults = dfa.findSpans("1 2 3 4 5 6");

        assertSameSpans(nfaResults, dfaResults);
        assertEquals(2, dfaResults.size());
        assertEquals(0, dfaResults.get(0).getStart());
        assertEquals(5, dfaResults.get(0).getEnd());
        assertEquals(6, dfaResults.get(1).getStart());
        assertEquals(11, dfaResults.get(1).getEnd());
    }

    @Test
    void dfaFindsSameSpanAsNfaForLazyBoundedGap() {
        GrammarModel model = Parser.init(LazyGap.class, Initial.class, Word.class, Duration.class).describe(LazyGap.class);
        NfaRecognizer nfa = new NfaRecognizer(model);
        DfaFindRecognizer dfa = dfaRecognizer(model);

        List<NfaFindSpan> nfaResults = nfa.findSpans("noise initial period of duration later");
        List<NfaFindSpan> dfaResults = dfa.findSpans("noise initial period of duration later");

        assertSameSpans(nfaResults, dfaResults);
        assertEquals(1, dfaResults.size());
        assertEquals(6, dfaResults.get(0).getStart());
        assertEquals(32, dfaResults.get(0).getEnd());
    }

    @Test
    void dfaFindsRuntimeOverlappingRegexTerminalSets() {
        GrammarModel model = Parser
            .init(
                OrganizationEvidence.class,
                ContractingAuthorityLine.class,
                OrganizationName.class,
                NameToken.class,
                Contracting.class,
                Authority.class,
                Colon.class,
                WordToken.class,
                CapitalizedWord.class,
                City.class,
                Council.class,
                SymbolToken.class
            )
            .describe(OrganizationEvidence.class);
        LexerConfig lexerConfig = new LexerConfig(
            new Class<?>[] {WordToken.class, SymbolToken.class},
            new Class<?>[0],
            LexerEngine.JAVA_REGEX,
            false,
            true,
            true
        );
        NfaRecognizer nfa = new NfaRecognizer(new NfaCompiler(model).compile(), lexerConfig.createLexer(model));
        DfaFindRecognizer dfa = new DfaFindRecognizer(
            new NfaCompiler(model).compile(),
            lexerConfig.createLexer(model),
            lexerConfig.configuredTerminals(model)
        );

        List<NfaFindSpan> nfaResults = nfa.findSpans("noise Contracting Authority: Birmingham City Council.");
        List<NfaFindSpan> dfaResults = dfa.findSpans("noise Contracting Authority: Birmingham City Council.");

        assertSameSpans(nfaResults, dfaResults);
        assertEquals(1, dfaResults.size());
        assertEquals(6, dfaResults.get(0).getStart());
        assertEquals(52, dfaResults.get(0).getEnd());
    }

    @Test
    void dfaFindBuildsTraceByReparsingMatchedSpan() {
        GrammarModel model = Parser.describe(PlusNumber.class);
        DfaFindRecognizer dfa = dfaRecognizer(model);

        List<NfaFindResult> results = dfa.findAll("1 + 2 3 + 4");
        PlusNumber first = Parser.buildFromTrace(PlusNumber.class, results.get(0).getTrace());
        PlusNumber second = Parser.buildFromTrace(PlusNumber.class, results.get(1).getTrace());

        assertEquals(2, first.value);
        assertEquals(4, second.value);
    }

    @Test
    void dfaFindsKeywordWhenLexerAlsoAddsUnusedCarrierTerminal() {
        GrammarModel model = Parser
            .builder()
            .classes(KeywordOnly.class, Hello.class, WordCarrier.class)
            .terminals(WordCarrier.class)
            .build()
            .describe(KeywordOnly.class);
        LexerConfig lexerConfig = LexerConfig.terminals(WordCarrier.class);
        DfaFindRecognizer dfa = new DfaFindRecognizer(
            new NfaCompiler(model).compile(),
            lexerConfig.createLexer(model),
            lexerConfig.configuredTerminals(model)
        );

        List<NfaFindSpan> results = dfa.findSpans("noise hello later");

        assertEquals(1, results.size());
        assertEquals(6, results.get(0).getStart());
        assertEquals(11, results.get(0).getEnd());
    }

    @Test
    void findsSpansThroughStreamingAsciiWordLexer() {
        GrammarModel model = Parser
            .initFromOuterClass(SmallPerfectGrammar.class)
            .describe(SmallPerfectGrammar.SentencePerfect.class);
        List<TerminalSymbol> terminals = new ArrayList<TerminalSymbol>(model.getTerminals());
        terminals.add(GrammarModelBuilder.terminalSymbol(SmallPerfectGrammar.Word.class));
        Lexer lexer = Lexers.asciiWord(new LexerSpec(terminals), SmallPerfectGrammar.Word.class);
        NfaRecognizer recognizer = new NfaRecognizer(new NfaCompiler(model).compile(), lexer);

        List<NfaFindSpan> results = recognizer.findSpans("noise i have started later they have finished");

        assertEquals(2, results.size());
        assertEquals(6, results.get(0).getStart());
        assertEquals(20, results.get(0).getEnd());
        assertEquals(27, results.get(1).getStart());
        assertEquals(45, results.get(1).getEnd());
    }

    @Test
    void boundedRepeatLeavesGapLazilyWhenNextPartCanMatch() {
        Parser.InitializedParser parser = Parser.init(LazyGap.class, Initial.class, Word.class, Duration.class);

        LazyGap result = parser.parse(LazyGap.class, "initial period of duration");

        assertEquals(2, result.gap.length);
        assertEquals("period", result.gap[0].text);
        assertEquals("of", result.gap[1].text);
    }

    @Test
    void boundedRepeatRejectsMoreThanMaxElements() {
        NfaRecognizer recognizer = recognizer(BoundedNumberArray.class);

        assertEquals(true, recognizer.recognizes("1 2 3"));
        assertEquals(false, recognizer.recognizes("1 2 3 4"));
    }

    @Test
    void findKeepsLongestMatchForFirstAcceptedStart() {
        NfaRecognizer recognizer = recognizer(NumberArray.class);

        NfaFindResult result = recognizer.find("1,2,3 4,5");

        assertEquals(0, result.getStart());
        assertEquals(5, result.getEnd());
    }

    private NfaRecognizer recognizer(Class<?> start) {
        GrammarModel model = Parser.describe(start);
        return new NfaRecognizer(model);
    }

    private DfaFindRecognizer dfaRecognizer(GrammarModel model) {
        return new DfaFindRecognizer(
            new NfaCompiler(model).compile(),
            LexerConfig.DEFAULT.createLexer(model),
            LexerConfig.DEFAULT.configuredTerminals(model)
        );
    }

    private void assertSameSpans(List<NfaFindSpan> expected, List<NfaFindSpan> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getStart(), actual.get(i).getStart());
            assertEquals(expected.get(i).getEnd(), actual.get(i).getEnd());
        }
    }

    static class Point {
        final Integer x;
        final Integer y;

        public Point(Integer x, Integer y) {
            this.x = x;
            this.y = y;
        }
    }

    static class SignedNumber {
        final Optional<Minus> sign;
        final Integer number;

        public SignedNumber(Optional<Minus> sign, Integer number) {
            this.sign = sign;
            this.number = number;
        }
    }

    static class NumberArray {
        final Integer[] numbers;

        public NumberArray(@Separator(Comma.class) Integer[] numbers) {
            this.numbers = numbers;
        }
    }

    static class BoundedNumberArray {
        public BoundedNumberArray(@Occurrences(min = 1, max = 3) Integer[] numbers) {
        }
    }

    static class LazyGap {
        final Word[] gap;

        public LazyGap(Initial initial, @Occurrences(max = 6) Word[] gap, Duration duration) {
            this.gap = gap;
        }
    }

    @Keyword("initial")
    static class Initial {
    }

    @Keyword("duration")
    static class Duration {
    }

    @Terminal(regexp = "[A-Za-z]+")
    static class Word {
        final String text;

        public Word(String text) {
            this.text = text;
        }
    }

    interface OrganizationExpression {
    }

    static class OrganizationEvidence {
        OrganizationEvidence(OrganizationExpression expression) {
        }
    }

    static class ContractingAuthorityLine implements OrganizationExpression {
        ContractingAuthorityLine(Contracting contracting, Authority authority, Colon colon, OrganizationName name) {
        }
    }

    static class OrganizationName {
        OrganizationName(@Occurrences(min = 1, max = 4) NameToken[] tokens) {
        }
    }

    interface NameToken {
    }

    @Terminal(regexp = "[A-Za-z]+")
    static class WordToken {
    }

    @Terminal(regexp = "[^A-Za-z0-9\\s]")
    static class SymbolToken {
    }

    @Terminal(regexp = "[A-Z][A-Za-z0-9]*")
    static class CapitalizedWord implements NameToken {
    }

    @Keyword(value = "contracting", caseSensitive = false)
    static class Contracting {
    }

    @Keyword(value = "authority", caseSensitive = false)
    static class Authority {
    }

    @Keyword(":")
    static class Colon {
    }

    @Keyword(value = "city", caseSensitive = false)
    static class City implements NameToken {
    }

    @Keyword(value = "council", caseSensitive = false)
    static class Council implements NameToken {
    }

    static class PlusMinus {
        public PlusMinus(Plus plus) {
        }

        public PlusMinus(Minus minus) {
        }
    }

    static class PlusNumber {
        final Integer value;

        public PlusNumber(Plus plus, Integer value) {
            this.value = value;
        }
    }

    @Keyword("+")
    static class Plus {
    }

    @Keyword("-")
    static class Minus {
    }

    static class KeywordOnly {
        KeywordOnly(Hello hello) {
        }
    }

    @Keyword(value = "hello", caseSensitive = false)
    static class Hello {
    }

    @Terminal(regexp = "[A-Za-z]+")
    static class WordCarrier {
    }

    @Keyword(",")
    static class Comma {
    }

    static final class SmallPerfectGrammar {
        static class SentencePerfect {
            SentencePerfect(Pronoun pronoun, HaveHas haveHas, ParticipleVerb verb) {
            }
        }

        @Terminal(regexp = "[A-Za-z]+")
        static class Word {
        }

        @Keywords(caseSensitive = false)
        enum Pronoun {
            I,
            THEY
        }

        @Keywords(caseSensitive = false)
        enum HaveHas {
            HAVE,
            HAS
        }

        @Keywords(caseSensitive = false)
        enum ParticipleVerb {
            STARTED,
            FINISHED
        }
    }
}
