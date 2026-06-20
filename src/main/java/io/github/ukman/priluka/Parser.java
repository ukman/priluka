package io.github.ukman.priluka;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NfaCompatibility;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.nfa.NfaFindResult;
import io.github.ukman.priluka.internal.nfa.NfaParseEngine;
import io.github.ukman.priluka.internal.nfa.NfaRecognizer;
import io.github.ukman.priluka.internal.parser.ParseEngine;
import io.github.ukman.priluka.internal.parser.ReflectiveParser;
import io.github.ukman.priluka.internal.parser.TraceObjectBuilder;

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

    public static <S> ParseTraceResult<S> trace(Class<S> start, String input) {
        return init(start).trace(start, input);
    }

    public static <S> ParseFindResult<S> find(Class<S> start, String input) {
        return init(start).find(start, input);
    }

    public static <S> S buildFromTrace(Class<S> start, ParseTrace trace) {
        return new TraceObjectBuilder().build(start, trace);
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
            return trace(start, input).getValue();
        }

        public <S> ParseTraceResult<S> trace(Class<S> start, String input) {
            GrammarModel model = describe(start);
            ParseEngine engine = parseEngine(model);
            ParseTrace trace = engine.parseTrace(start, input);
            return new ParseTraceResult<S>(new TraceObjectBuilder().build(start, trace), trace);
        }

        public <S> ParseFindResult<S> find(Class<S> start, String input) {
            GrammarModel model = describe(start);
            NfaCompatibility compatibility = model.checkNfaCompatibility();
            if (!compatibility.isSupported()) {
                throw new GrammarException(compatibility.toString());
            }
            NfaFindResult result = new NfaRecognizer(model).find(input);
            if (result == null) {
                return null;
            }
            S value = new TraceObjectBuilder().build(start, result.getTrace());
            return new ParseFindResult<S>(value, result.getTrace(), result.getStart(), result.getEnd());
        }

        public <S> S buildFromTrace(Class<S> start, ParseTrace trace) {
            return new TraceObjectBuilder().build(start, trace);
        }

        public GrammarModel describe(Class<?> start) {
            return new GrammarModelBuilder(classes).build(start);
        }

        private ParseEngine parseEngine(GrammarModel model) {
            if (model.checkNfaCompatibility().isSupported()) {
                return new NfaParseEngine(model);
            }
            return new ReflectiveParser(model);
        }
    }
}
