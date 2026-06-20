package io.github.ukman.priluka;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.parser.ReflectiveParser;

/**
 * Main Priluka entry point.
 */
public final class Parser {
    private Parser() {
    }

    public static InitializedParser init(Class<?>... classes) {
        return new InitializedParser(classes);
    }

    public static InitializedParser initFromOuterClass(Class<?> outerClass) {
        return new InitializedParser(outerClass.getDeclaredClasses());
    }

    public static <S> S parse(Class<S> start, String input) {
        return init(start).parse(start, input);
    }

    public static GrammarModel describe(Class<?> start) {
        return init(start).describe(start);
    }

    public static final class InitializedParser {
        private final Class<?>[] classes;

        private InitializedParser(Class<?>[] classes) {
            this.classes = classes.clone();
        }

        public <S> S parse(Class<S> start, String input) {
            GrammarModel model = describe(start);
            return new ReflectiveParser(model).parse(start, input);
        }

        public GrammarModel describe(Class<?> start) {
            return new GrammarModelBuilder(classes).build(start);
        }
    }
}
