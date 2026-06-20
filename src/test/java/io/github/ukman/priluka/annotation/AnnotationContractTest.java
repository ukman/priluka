package io.github.ukman.priluka.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationContractTest {

    @Test
    void terminalMetadataIsAvailableAtRuntime() {
        Terminal terminal = IntNumber.class.getAnnotation(Terminal.class);

        assertNotNull(terminal);
        assertEquals("[0-9]+", terminal.regexp());
        assertEquals(7, terminal.priority());
    }

    @Test
    void keywordPriorityIsAvailableAtRuntime() {
        Keyword keyword = If.class.getAnnotation(Keyword.class);

        assertNotNull(keyword);
        assertEquals(11, keyword.priority());
    }

    @Test
    void separatorMetadataIsAvailableOnConstructorParameters() throws NoSuchMethodException {
        Constructor<Numbers> constructor = Numbers.class.getConstructor(Integer[].class);
        Parameter parameter = constructor.getParameters()[0];

        Separator separator = parameter.getAnnotation(Separator.class);

        assertNotNull(separator);
        assertEquals(Comma.class, separator.value());
        assertTrue(separator.trailing());
    }

    @Test
    void regexGroupCanUseIndexOrName() throws NoSuchMethodException {
        Constructor<DateToken> constructor = DateToken.class.getConstructor(int.class, int.class);
        Parameter[] parameters = constructor.getParameters();

        assertEquals(1, parameters[0].getAnnotation(RegexGroup.class).index());
        assertEquals("month", parameters[1].getAnnotation(RegexGroup.class).name());
    }

    @Terminal(regexp = "[0-9]+", priority = 7)
    static class IntNumber {
    }

    @Keyword(priority = 11)
    static class If {
    }

    @Terminal(regexp = ",")
    static class Comma {
    }

    static class Numbers {
        public Numbers(@Separator(value = Comma.class, trailing = true) Integer[] numbers) {
        }
    }

    @Terminal(regexp = "([0-9]{4})-(?<month>[0-9]{2})")
    static class DateToken {
        public DateToken(@RegexGroup(index = 1) int year, @RegexGroup(name = "month") int month) {
        }
    }
}
