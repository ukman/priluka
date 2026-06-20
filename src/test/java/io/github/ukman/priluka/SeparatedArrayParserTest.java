package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeparatedArrayParserTest {
    @Test
    void parsesCommaSeparatedIntegerArray() {
        ListNumbers numbers = Parser
            .init(ListNumbers.class, NonEmptyListNumbers.class, Comma.class)
            .parse(ListNumbers.class, "1,2,3,5,5445,345634");

        assertArrayEquals(
            new Integer[] {1, 2, 3, 5, 5445, 345634},
            numbers.values
        );
    }

    @Test
    void parsesEmptySeparatedArrayByDefault() {
        ListNumbers numbers = Parser
            .init(ListNumbers.class, NonEmptyListNumbers.class, Comma.class)
            .parse(ListNumbers.class, "");

        assertArrayEquals(new Integer[0], numbers.values);
    }

    @Test
    void parsesOneOrMoreSeparatedArrayWithTrailingComma() {
        NonEmptyListNumbers numbers = Parser
            .init(ListNumbers.class, NonEmptyListNumbers.class, Comma.class)
            .parse(NonEmptyListNumbers.class, "10,20,30,");

        assertArrayEquals(new Integer[] {10, 20, 30}, numbers.values);
    }

    @Test
    void rejectsMissingElementBetweenSeparators() {
        assertThrows(ParseException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                Parser
                    .init(ListNumbers.class, NonEmptyListNumbers.class, Comma.class)
                    .parse(ListNumbers.class, "1,,2");
            }
        });
    }

    @Test
    void parsesWhitespaceSeparatedIntegerArray() {
        SpaceSeparatedNumbers numbers = Parser
            .init(ListNumbers.class, NonEmptyListNumbers.class, SpaceSeparatedNumbers.class, Comma.class)
            .parse(SpaceSeparatedNumbers.class, "7 8 9 10");

        assertArrayEquals(new Integer[] {7, 8, 9, 10}, numbers.values);
    }

    @Test
    void parsesCommaSeparatedIntegerList() {
        ListNumberCollection numbers = Parser
            .init(ListNumberCollection.class, Comma.class)
            .parse(ListNumberCollection.class, "3,4,5");

        assertEquals(Arrays.asList(3, 4, 5), numbers.values);
    }

    @Test
    void parsesEmptyIntegerListByDefault() {
        ListNumberCollection numbers = Parser
            .init(ListNumberCollection.class, Comma.class)
            .parse(ListNumberCollection.class, "");

        assertEquals(Arrays.asList(), numbers.values);
    }

    @Test
    void parsesOptionalPresentValue() {
        SignedNumber number = Parser
            .init(SignedNumber.class, Minus.class)
            .parse(SignedNumber.class, "- 5");

        assertEquals(Integer.valueOf(5), number.value);
        assertEquals(true, number.minus.isPresent());
    }

    @Test
    void parsesOptionalAbsentValue() {
        SignedNumber number = Parser
            .init(SignedNumber.class, Minus.class)
            .parse(SignedNumber.class, "5");

        assertEquals(Integer.valueOf(5), number.value);
        assertEquals(false, number.minus.isPresent());
    }

    @Test
    void returnsTraceForSeparatedList() {
        ParseTraceResult<ListNumberCollection> result = Parser
            .init(ListNumberCollection.class, Comma.class)
            .trace(ListNumberCollection.class, "3,4");

        List<String> events = result.getTrace().getEvents();
        assertEquals(Arrays.asList(3, 4), result.getValue().values);
        assertEquals(true, events.contains("beginRepeat(Integer)"));
        assertEquals(true, events.contains("appendRepeatElement(Integer)"));
        assertEquals(true, events.contains("endRepeat(Integer, count=2)"));
        assertEquals(true, events.contains("consumeTerminal(Comma, \",\", start=1, len=1)"));
    }

    static class ListNumbers {
        final Integer[] values;

        ListNumbers(@Separator(Comma.class) Integer[] values) {
            this.values = values;
        }
    }

    static class NonEmptyListNumbers {
        final Integer[] values;

        NonEmptyListNumbers(@OneOrMore @Separator(value = Comma.class, trailing = true) Integer[] values) {
            this.values = values;
        }
    }

    static class SpaceSeparatedNumbers {
        final Integer[] values;

        SpaceSeparatedNumbers(Integer[] values) {
            this.values = values;
        }
    }

    static class ListNumberCollection {
        final List<Integer> values;

        ListNumberCollection(@Separator(Comma.class) List<Integer> values) {
            this.values = values;
        }
    }

    static class SignedNumber {
        final Optional<Minus> minus;
        final Integer value;

        SignedNumber(Optional<Minus> minus, Integer value) {
            this.minus = minus;
            this.value = value;
        }
    }

    @Keyword(",")
    static class Comma {
    }

    @Keyword("-")
    static class Minus {
    }

    interface ThrowingRunnable extends org.junit.jupiter.api.function.Executable {
    }
}
