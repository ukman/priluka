package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.lexer.Lexer;
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
