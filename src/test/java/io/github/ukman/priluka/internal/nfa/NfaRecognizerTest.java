package io.github.ukman.priluka.internal.nfa;

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
    void recognizesOptionalPart() {
        NfaRecognizer recognizer = recognizer(SignedNumber.class);

        assertEquals(true, recognizer.recognizes("1"));
        assertEquals(true, recognizer.recognizes("-1"));
        assertEquals(true, recognizer.recognizes("- 1"));
        assertEquals(false, recognizer.recognizes("-"));
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
    void recognizesConstructorAlternatives() {
        NfaRecognizer recognizer = recognizer(PlusMinus.class);

        assertEquals(true, recognizer.recognizes("+"));
        assertEquals(true, recognizer.recognizes("-"));
        assertEquals(false, recognizer.recognizes("*"));
    }

    private NfaRecognizer recognizer(Class<?> start) {
        GrammarModel model = Parser.describe(start);
        return new NfaRecognizer(model);
    }

    static class Point {
        public Point(Integer x, Integer y) {
        }
    }

    static class SignedNumber {
        public SignedNumber(Optional<Minus> sign, Integer number) {
        }
    }

    static class NumberArray {
        public NumberArray(@Separator(Comma.class) Integer[] numbers) {
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
