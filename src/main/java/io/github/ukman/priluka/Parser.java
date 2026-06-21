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

import java.util.ArrayList;
import java.util.List;

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

    public static Builder builder() {
        return new Builder();
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

    public static <S> ParseFindResult<S> find(Class<S> start, String input, Class<?>... lexerTerminalTypes) {
        return builder().classes(start).terminals(lexerTerminalTypes).build().find(start, input);
    }

    public static <S> S buildFromTrace(Class<S> start, ParseTrace trace) {
        return new TraceObjectBuilder().build(start, trace);
    }

    public static GrammarModel describe(Class<?> start) {
        return init(start).describe(start);
    }

    public static final class Builder {
        private final List<Class<?>> classes = new ArrayList<Class<?>>();
        private final List<Class<?>> lexerTerminalTypes = new ArrayList<Class<?>>();

        private Builder() {
        }

        public Builder classes(Class<?>... classes) {
            for (int i = 0; i < classes.length; i++) {
                this.classes.add(classes[i]);
            }
            return this;
        }

        public Builder terminals(Class<?>... terminalTypes) {
            for (int i = 0; i < terminalTypes.length; i++) {
                this.lexerTerminalTypes.add(terminalTypes[i]);
            }
            return this;
        }

        public InitializedParser build() {
            return new InitializedParser(
                classes.toArray(new Class<?>[classes.size()]),
                lexerTerminalTypes.toArray(new Class<?>[lexerTerminalTypes.size()])
            );
        }
    }

    public static final class InitializedParser {
        private final Class<?>[] classes;
        private final Class<?>[] lexerTerminalTypes;

        private InitializedParser(Class<?>[] classes) {
            this(classes, new Class<?>[0]);
        }

        private InitializedParser(Class<?>[] classes, Class<?>[] lexerTerminalTypes) {
            this.classes = classes.clone();
            this.lexerTerminalTypes = lexerTerminalTypes.clone();
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
            NfaFindResult result = new NfaRecognizer(model, lexerTerminalTypes).find(input);
            if (result == null) {
                return null;
            }
            return toFindResult(start, result);
        }

        public <S> ParseFindResult<S> find(Class<S> start, String input, Class<?>... lexerTerminalTypes) {
            return withTerminals(lexerTerminalTypes).find(start, input);
        }

        public <S> List<ParseFindResult<S>> findAll(Class<S> start, String input) {
            GrammarModel model = describe(start);
            NfaCompatibility compatibility = model.checkNfaCompatibility();
            if (!compatibility.isSupported()) {
                throw new GrammarException(compatibility.toString());
            }
            List<NfaFindResult> nfaResults = new NfaRecognizer(model, lexerTerminalTypes).findAll(input);
            List<ParseFindResult<S>> results = new ArrayList<ParseFindResult<S>>(nfaResults.size());
            for (NfaFindResult result : nfaResults) {
                results.add(toFindResult(start, result));
            }
            return results;
        }

        public <S> List<ParseFindResult<S>> findAll(Class<S> start, String input, Class<?>... lexerTerminalTypes) {
            return withTerminals(lexerTerminalTypes).findAll(start, input);
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

        private InitializedParser withTerminals(Class<?>... lexerTerminalTypes) {
            Class<?>[] combined = new Class<?>[this.lexerTerminalTypes.length + lexerTerminalTypes.length];
            System.arraycopy(this.lexerTerminalTypes, 0, combined, 0, this.lexerTerminalTypes.length);
            System.arraycopy(lexerTerminalTypes, 0, combined, this.lexerTerminalTypes.length, lexerTerminalTypes.length);
            return new InitializedParser(classes, combined);
        }

        private <S> ParseFindResult<S> toFindResult(Class<S> start, NfaFindResult result) {
            S value = new TraceObjectBuilder().build(start, result.getTrace());
            return new ParseFindResult<S>(value, result.getTrace(), result.getStart(), result.getEnd());
        }
    }
}
