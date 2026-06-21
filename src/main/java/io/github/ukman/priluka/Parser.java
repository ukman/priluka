package io.github.ukman.priluka;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NfaCompatibility;
import io.github.ukman.priluka.annotation.NoHardBoundary;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.lexer.LexerConfig;
import io.github.ukman.priluka.internal.nfa.DfaFindRecognizer;
import io.github.ukman.priluka.internal.nfa.NfaCompiler;
import io.github.ukman.priluka.internal.nfa.NfaFindResult;
import io.github.ukman.priluka.internal.nfa.NfaParseEngine;
import io.github.ukman.priluka.internal.nfa.NfaRecognizer;
import io.github.ukman.priluka.internal.parser.ParseEngine;
import io.github.ukman.priluka.internal.parser.ReflectiveParser;
import io.github.ukman.priluka.internal.parser.TraceObjectBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Fluent configuration builder for parser instances.
     */
    public static final class Builder {
        private final List<Class<?>> classes = new ArrayList<Class<?>>();
        private final List<Class<?>> lexerTerminalTypes = new ArrayList<Class<?>>();
        private final List<Class<?>> skipTerminalTypes = new ArrayList<Class<?>>();
        private LexerEngine lexerEngine = LexerEngine.DEFAULT;
        private FindEngine findEngine = FindEngine.NFA;
        private boolean regexpCaseSensitive = true;
        private boolean collectAmbiguousTerminalTypes = true;
        private boolean keywordCarrierOptimization = true;

        private Builder() {
        }

        public Builder classes(Class<?>... classes) {
            for (int i = 0; i < classes.length; i++) {
                this.classes.add(classes[i]);
            }
            return this;
        }

        public Builder classesFromOuterClass(Class<?> outerClass) {
            return classes(outerClass.getDeclaredClasses());
        }

        public Builder terminals(Class<?>... terminalTypes) {
            for (int i = 0; i < terminalTypes.length; i++) {
                this.lexerTerminalTypes.add(terminalTypes[i]);
            }
            return this;
        }

        public Builder skip(Class<?>... terminalTypes) {
            for (int i = 0; i < terminalTypes.length; i++) {
                this.skipTerminalTypes.add(terminalTypes[i]);
            }
            return this;
        }

        public Builder caseSensitive() {
            this.regexpCaseSensitive = true;
            return this;
        }

        public Builder caseInsensitive() {
            this.regexpCaseSensitive = false;
            return this;
        }

        public Builder engine(LexerEngine engine) {
            this.lexerEngine = engine;
            return this;
        }

        public Builder findEngine(FindEngine engine) {
            this.findEngine = engine;
            return this;
        }

        public Builder nfaFind() {
            this.findEngine = FindEngine.NFA;
            return this;
        }

        public Builder dfaFind() {
            this.findEngine = FindEngine.DFA;
            return this;
        }

        public Builder collectAmbiguousTerminals() {
            this.collectAmbiguousTerminalTypes = true;
            return this;
        }

        public Builder singleTerminal() {
            this.collectAmbiguousTerminalTypes = false;
            return this;
        }

        public Builder keywordCarrierOptimization() {
            this.keywordCarrierOptimization = true;
            return this;
        }

        public Builder disableKeywordCarrierOptimization() {
            this.keywordCarrierOptimization = false;
            return this;
        }

        public InitializedParser build() {
            return new InitializedParser(
                classes.toArray(new Class<?>[classes.size()]),
                new LexerConfig(
                    lexerTerminalTypes.toArray(new Class<?>[lexerTerminalTypes.size()]),
                    skipTerminalTypes.toArray(new Class<?>[skipTerminalTypes.size()]),
                    lexerEngine,
                    regexpCaseSensitive,
                    collectAmbiguousTerminalTypes,
                    keywordCarrierOptimization
                ),
                findEngine
            );
        }
    }

    /**
     * Parser instance configured with a class universe and lexer options.
     */
    public static final class InitializedParser {
        private final Class<?>[] classes;
        private final LexerConfig lexerConfig;
        private final FindEngine findEngine;
        private final Map<Class<?>, GrammarModel> modelCache = new LinkedHashMap<Class<?>, GrammarModel>();
        private final Map<Class<?>, NfaRecognizer> nfaFindCache = new LinkedHashMap<Class<?>, NfaRecognizer>();
        private final Map<Class<?>, DfaFindRecognizer> dfaFindCache = new LinkedHashMap<Class<?>, DfaFindRecognizer>();

        private InitializedParser(Class<?>[] classes) {
            this(classes, LexerConfig.DEFAULT);
        }

        private InitializedParser(Class<?>[] classes, LexerConfig lexerConfig) {
            this(classes, lexerConfig, FindEngine.NFA);
        }

        private InitializedParser(Class<?>[] classes, LexerConfig lexerConfig, FindEngine findEngine) {
            this.classes = classes.clone();
            this.lexerConfig = lexerConfig;
            this.findEngine = findEngine;
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
            NfaFindResult result = findWithConfiguredEngine(start, model, input);
            if (result == null) {
                return null;
            }
            if (rejectHardBoundaryCrossing(start)) {
                if (!crossesHardBoundary(input, result.getStart(), result.getEnd())) {
                    return toFindResult(start, result);
                }
                List<NfaFindResult> results = findAllWithConfiguredEngine(start, model, input);
                for (NfaFindResult candidate : results) {
                    if (!crossesHardBoundary(input, candidate.getStart(), candidate.getEnd())) {
                        return toFindResult(start, candidate);
                    }
                }
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
            List<NfaFindResult> nfaResults = findAllWithConfiguredEngine(start, model, input);
            List<ParseFindResult<S>> results = new ArrayList<ParseFindResult<S>>(nfaResults.size());
            for (NfaFindResult result : nfaResults) {
                if (rejectHardBoundaryCrossing(start)
                    && crossesHardBoundary(input, result.getStart(), result.getEnd())) {
                    continue;
                }
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
            GrammarModel cached = modelCache.get(start);
            if (cached != null) {
                return cached;
            }
            GrammarModel created = new GrammarModelBuilder(classes).build(start);
            modelCache.put(start, created);
            return created;
        }

        private ParseEngine parseEngine(GrammarModel model) {
            if (model.checkNfaCompatibility().isSupported()) {
                return new NfaParseEngine(model, lexerConfig);
            }
            return new ReflectiveParser(model, lexerConfig);
        }

        private NfaFindResult findWithConfiguredEngine(Class<?> start, GrammarModel model, String input) {
            if (isDfaFindEngine()) {
                List<NfaFindResult> results = dfaFindRecognizer(start, model).findAll(input);
                return results.isEmpty() ? null : results.get(0);
            }
            return nfaFindRecognizer(start, model).find(input);
        }

        private List<NfaFindResult> findAllWithConfiguredEngine(Class<?> start, GrammarModel model, String input) {
            if (isDfaFindEngine()) {
                return dfaFindRecognizer(start, model).findAll(input);
            }
            return nfaFindRecognizer(start, model).findAll(input);
        }

        private NfaRecognizer nfaFindRecognizer(Class<?> start, GrammarModel model) {
            NfaRecognizer cached = nfaFindCache.get(start);
            if (cached != null) {
                return cached;
            }
            NfaRecognizer created = new NfaRecognizer(model, lexerConfig);
            nfaFindCache.put(start, created);
            return created;
        }

        private DfaFindRecognizer dfaFindRecognizer(Class<?> start, GrammarModel model) {
            DfaFindRecognizer cached = dfaFindCache.get(start);
            if (cached != null) {
                return cached;
            }
            DfaFindRecognizer created = new DfaFindRecognizer(
                new NfaCompiler(model).compile(),
                lexerConfig.createLexer(model),
                lexerConfig.configuredTerminals(model)
            );
            dfaFindCache.put(start, created);
            return created;
        }

        private boolean isDfaFindEngine() {
            return findEngine == FindEngine.DFA;
        }

        private InitializedParser withTerminals(Class<?>... lexerTerminalTypes) {
            return new InitializedParser(classes, lexerConfig.withAdditionalTerminals(lexerTerminalTypes), findEngine);
        }

        private <S> ParseFindResult<S> toFindResult(Class<S> start, NfaFindResult result) {
            S value = new TraceObjectBuilder().build(start, result.getTrace());
            return new ParseFindResult<S>(value, result.getTrace(), result.getStart(), result.getEnd());
        }

        private boolean rejectHardBoundaryCrossing(Class<?> start) {
            return start.isAnnotationPresent(NoHardBoundary.class);
        }

        private boolean crossesHardBoundary(String input, int start, int end) {
            for (int i = start; i < end; i++) {
                char c = input.charAt(i);
                if (c == '\n' || c == '\r' || c == '\t' || c == '\f') {
                    return true;
                }
            }
            return false;
        }
    }
}
