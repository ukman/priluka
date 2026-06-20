package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.grammar.GrammarModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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

    @Keyword("+")
    static class Plus {
    }

    @Keyword("-")
    static class Minus {
    }

    @Keyword(",")
    static class Comma {
    }
}
