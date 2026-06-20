package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Keyword(",")
    static class Comma {
    }

    interface ThrowingRunnable extends org.junit.jupiter.api.function.Executable {
    }
}
