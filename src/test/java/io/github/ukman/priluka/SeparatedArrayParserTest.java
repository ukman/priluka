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

        List<ParseTraceEvent> events = result.getTrace().getEvents();
        assertEquals(Arrays.asList(3, 4), result.getValue().values);
        assertEquals(true, hasEvent(events, ParseTraceEvent.Kind.BEGIN_REPEAT, "Integer"));
        assertEquals(true, hasEvent(events, ParseTraceEvent.Kind.APPEND_REPEAT_ELEMENT, "Integer"));
        assertEquals(true, hasEndRepeat(events, "Integer", 2));
        assertEquals(true, hasTerminal(events, Comma.class, ",", 1, 1));
    }

    @Test
    void rebuildsSeparatedListFromTrace() {
        Parser.InitializedParser parser = Parser.init(ListNumberCollection.class, Comma.class);
        ParseTraceResult<ListNumberCollection> result = parser.trace(ListNumberCollection.class, "3,4");

        ListNumberCollection rebuilt = parser.buildFromTrace(ListNumberCollection.class, result.getTrace());

        assertEquals(Arrays.asList(3, 4), rebuilt.values);
    }

    @Test
    void rebuildsOptionalValueFromTrace() {
        Parser.InitializedParser parser = Parser.init(SignedNumber.class, Minus.class);
        ParseTraceResult<SignedNumber> result = parser.trace(SignedNumber.class, "- 5");

        SignedNumber rebuilt = parser.buildFromTrace(SignedNumber.class, result.getTrace());

        assertEquals(Integer.valueOf(5), rebuilt.value);
        assertEquals(true, rebuilt.minus.isPresent());
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

    private boolean hasEvent(List<ParseTraceEvent> events, ParseTraceEvent.Kind kind, String symbolName) {
        for (ParseTraceEvent event : events) {
            if (event.getKind() == kind && symbolName.equals(event.getSymbolName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEndRepeat(List<ParseTraceEvent> events, String symbolName, int count) {
        for (ParseTraceEvent event : events) {
            if (event.getKind() == ParseTraceEvent.Kind.END_REPEAT
                && symbolName.equals(event.getSymbolName())
                && Integer.valueOf(count).equals(event.getCount())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTerminal(
        List<ParseTraceEvent> events,
        Class<?> terminalType,
        String text,
        int start,
        int len
    ) {
        for (ParseTraceEvent event : events) {
            if (event.getKind() == ParseTraceEvent.Kind.CONSUME_TERMINAL
                && terminalType.equals(event.getTerminalType())
                && text.equals(event.getText())
                && event.getStart() == start
                && event.getLen() == len) {
                return true;
            }
        }
        return false;
    }

    interface ThrowingRunnable extends org.junit.jupiter.api.function.Executable {
    }
}
