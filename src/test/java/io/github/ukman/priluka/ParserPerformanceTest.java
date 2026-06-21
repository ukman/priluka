package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.GrammarModelBuilder;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerOptions;
import io.github.ukman.priluka.internal.lexer.LexerSpec;
import io.github.ukman.priluka.internal.lexer.Lexers;
import io.github.ukman.priluka.internal.nfa.NfaCompiler;
import io.github.ukman.priluka.internal.nfa.NfaFindSpan;
import io.github.ukman.priluka.internal.nfa.NfaParseEngine;
import io.github.ukman.priluka.internal.nfa.NfaRecognizer;
import io.github.ukman.priluka.internal.parser.ParseEngine;
import io.github.ukman.priluka.internal.parser.ReflectiveParser;
import io.github.ukman.priluka.internal.parser.TraceObjectBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ParserPerformanceTest {
    private static final int WARMUP_RUNS = Integer.getInteger("priluka.perf.warmup", 1);
    private static final int MEASURE_RUNS = Integer.getInteger("priluka.perf.runs", 3);

    @Test
    void comparesPublicNfaAndReflectiveParserOnSeparatedNumberLists() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser publicParser = Parser.init(NumberArray.class);
        GrammarModel model = Parser.describe(NumberArray.class);
        EngineRun nfaEngine = new EngineRun("cached-nfa-engine", new NfaParseEngine(model));
        EngineRun reflectiveEngine = new EngineRun("cached-reflective-engine", new ReflectiveParser(model));

        int[] sizes = sizes();
        for (int i = 0; i < sizes.length; i++) {
            String input = generatedNumberList(sizes[i]);
            System.out.println(measurePublic(publicParser, "public-parser-auto", input));
            System.out.println(measureEngine(nfaEngine, input));
            System.out.println(measureEngine(reflectiveEngine, input));
            System.out.println();
        }
    }

    @Test
    void comparesPublicNfaAndReflectiveParserOnSimplifiedSqlSelects() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser publicParser = Parser.initFromOuterClass(
            SimplifiedSqlSelectParserTest.SimpleSqlGrammar.class
        );
        GrammarModel model = publicParser.describe(
            SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class
        );
        SqlEngineRun nfaEngine = new SqlEngineRun("simplified-sql cached-nfa-engine", new NfaParseEngine(model));
        SqlEngineRun reflectiveEngine = new SqlEngineRun(
            "simplified-sql cached-reflective-engine",
            new ReflectiveParser(model)
        );

        int[] sizes = sqlSizes();
        for (int i = 0; i < sizes.length; i++) {
            String input = generatedSimplifiedSql(sizes[i]);
            System.out.println(measurePublicSql(publicParser, "simplified-sql public-parser-auto", input));
            System.out.println(measureSqlEngine(nfaEngine, input));
            System.out.println(measureSqlEngine(reflectiveEngine, input));
            System.out.println();
        }
    }

    @Test
    void comparesPublicNfaAndReflectiveParserOnJoinHeavySimplifiedSqlSelects() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser publicParser = Parser.initFromOuterClass(
            SimplifiedSqlSelectParserTest.SimpleSqlGrammar.class
        );
        GrammarModel model = publicParser.describe(
            SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class
        );
        SqlEngineRun nfaEngine = new SqlEngineRun("join-heavy-sql cached-nfa-engine", new NfaParseEngine(model));
        SqlEngineRun reflectiveEngine = new SqlEngineRun(
            "join-heavy-sql cached-reflective-engine",
            new ReflectiveParser(model)
        );

        int[] fields = sqlFieldCounts();
        int[] joins = sqlJoinCounts();
        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < joins.length; j++) {
                String input = generatedJoinHeavySimplifiedSql(fields[i], joins[j]);
                System.out.println(measurePublicSql(
                    publicParser,
                    "join-heavy-sql public-parser-auto fields=" + fields[i] + " joins=" + joins[j],
                    input
                ));
                System.out.println(measureSqlEngine(nfaEngine.withLabel(
                    "join-heavy-sql cached-nfa-engine fields=" + fields[i] + " joins=" + joins[j]
                ), input));
                System.out.println(measureSqlEngine(reflectiveEngine.withLabel(
                    "join-heavy-sql cached-reflective-engine fields=" + fields[i] + " joins=" + joins[j]
                ), input));
                System.out.println();
            }
        }
    }

    @Test
    void findsValidSqlQueriesInLargeTokenizableText() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(
            SimplifiedSqlSelectParserTest.SimpleSqlGrammar.class
        );
        GrammarModel model = parser.describe(SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class);
        NfaRecognizer recognizer = new NfaRecognizer(model);
        String input = generatedSqlFindText(Integer.getInteger("priluka.parser.find.bytes", 100 * 1024));

        for (int i = 0; i < WARMUP_RUNS; i++) {
            assertFoundSqlCount(recognizer, input, 3);
        }

        long totalNanos = 0;
        int found = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            found = recognizer.findAll(input).size();
            totalNanos += System.nanoTime() - start;
        }
        if (found != 3) {
            throw new AssertionError("Expected 3 valid SQL queries, found " + found);
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        System.out.println(new FindResult("sql-find", input.length(), found, averageSeconds));
    }

    @Test
    void findsPresentPerfectPhrasesInLargeEnglishText() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(PresentPerfectGrammar.class);
        GrammarModel model = parser.describe(PresentPerfectGrammar.SentencePerfect.class);
        NfaRecognizer recognizer = new NfaRecognizer(model, PresentPerfectGrammar.WordToken.class);
        String input = generatedPresentPerfectText(
            Integer.getInteger("priluka.parser.perfect.bytes", 100 * 1024)
        );

        for (int i = 0; i < WARMUP_RUNS; i++) {
            assertFoundCount("present perfect phrase", recognizer, input, 5);
        }

        long totalNanos = 0;
        int found = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            found = recognizer.findAll(input).size();
            totalNanos += System.nanoTime() - start;
        }
        if (found != 5) {
            throw new AssertionError("Expected 5 present perfect phrases, found " + found);
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        System.out.println(new FindResult("present-perfect-find", input.length(), found, averageSeconds));
    }

    @Test
    void findsPresentPerfectPhrasesWithHandWrittenWordLexer() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(PresentPerfectGrammar.class);
        GrammarModel model = parser.describe(PresentPerfectGrammar.SentencePerfect.class);
        NfaRecognizer recognizer = new NfaRecognizer(
            new NfaCompiler(model).compile(),
            presentPerfectAsciiWordLexer(model)
        );
        String input = generatedPresentPerfectText(
            Integer.getInteger("priluka.parser.perfect.bytes", 100 * 1024)
        );

        for (int i = 0; i < WARMUP_RUNS; i++) {
            assertFoundCount("present perfect phrase", recognizer, input, 5);
        }

        long totalNanos = 0;
        int found = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            found = recognizer.findAll(input).size();
            totalNanos += System.nanoTime() - start;
        }
        if (found != 5) {
            throw new AssertionError("Expected 5 present perfect phrases, found " + found);
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        System.out.println(new FindResult("present-perfect-word-find", input.length(), found, averageSeconds));
    }

    @Test
    void findsPresentPerfectSpansWithStreamingWordLexer() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(PresentPerfectGrammar.class);
        GrammarModel model = parser.describe(PresentPerfectGrammar.SentencePerfect.class);
        NfaRecognizer recognizer = new NfaRecognizer(
            new NfaCompiler(model).compile(),
            presentPerfectAsciiWordLexer(model)
        );
        String input = generatedPresentPerfectText(
            Integer.getInteger("priluka.parser.perfect.bytes", 100 * 1024)
        );

        for (int i = 0; i < WARMUP_RUNS; i++) {
            List<NfaFindSpan> spans = recognizer.findSpans(input);
            if (spans.size() != 5) {
                throw new AssertionError("Expected 5 present perfect spans, found " + spans.size());
            }
        }

        long totalNanos = 0;
        int found = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            found = recognizer.findSpans(input).size();
            totalNanos += System.nanoTime() - start;
        }
        if (found != 5) {
            throw new AssertionError("Expected 5 present perfect spans, found " + found);
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        System.out.println(new FindResult("present-perfect-word-spans", input.length(), found, averageSeconds));
    }

    @Test
    void lexesPresentPerfectEnglishTextOnly() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(PresentPerfectGrammar.class);
        GrammarModel model = parser.describe(PresentPerfectGrammar.SentencePerfect.class);
        Lexer lexer = presentPerfectLexer(model);
        String input = generatedPresentPerfectText(
            Integer.getInteger("priluka.parser.perfect.bytes", 100 * 1024)
        );

        int tokens = 0;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            tokens = lexer.tokenize(input).size();
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            tokens = lexer.tokenize(input).size();
            totalNanos += System.nanoTime() - start;
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        System.out.println(new LexerResult("present-perfect-lexer", input.length(), tokens, averageSeconds));
    }

    @Test
    void scansPresentPerfectEnglishTextWithHandWrittenWordScanner() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual parser performance dump. Run with -Dpriluka.perf=true -Dtest=ParserPerformanceTest."
        );

        Map<String, Integer> keywords = presentPerfectKeywordMap();
        String input = generatedPresentPerfectText(
            Integer.getInteger("priluka.parser.perfect.bytes", 100 * 1024)
        );

        ScannerStats stats = null;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            stats = scanWords(input, keywords);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            stats = scanWords(input, keywords);
            totalNanos += System.nanoTime() - start;
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        System.out.println(new ScannerResult(
            "present-perfect-hand-scanner",
            input.length(),
            stats.words,
            stats.keywordHits,
            averageSeconds
        ));
    }

    private Result measurePublic(Parser.InitializedParser parser, String label, String input) {
        NumberArray value = null;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            value = parser.parse(NumberArray.class, input);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            value = parser.parse(NumberArray.class, input);
            totalNanos += System.nanoTime() - start;
        }

        return result(label, input, value, totalNanos);
    }

    private Result measureEngine(EngineRun run, String input) {
        NumberArray value = null;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            value = run.parse(input);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            value = run.parse(input);
            totalNanos += System.nanoTime() - start;
        }

        return result(run.label, input, value, totalNanos);
    }

    private Result measurePublicSql(Parser.InitializedParser parser, String label, String input) {
        SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement value = null;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            value = parser.parse(SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class, input);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            value = parser.parse(SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class, input);
            totalNanos += System.nanoTime() - start;
        }

        return sqlResult(label, input, value, totalNanos);
    }

    private Result measureSqlEngine(SqlEngineRun run, String input) {
        SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement value = null;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            value = run.parse(input);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            value = run.parse(input);
            totalNanos += System.nanoTime() - start;
        }

        return sqlResult(run.label, input, value, totalNanos);
    }

    private Result result(String label, String input, NumberArray value, long totalNanos) {
        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        return new Result(label, input.length(), value.numbers.length, averageSeconds);
    }

    private Result sqlResult(
        String label,
        String input,
        SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement value,
        long totalNanos
    ) {
        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        return new Result(label, input.length(), value.selectItems.length, averageSeconds);
    }

    private int[] sizes() {
        String configured = System.getProperty("priluka.parser.bytes");
        if (configured == null || configured.trim().isEmpty()) {
            return new int[] {1024, 10 * 1024, 100 * 1024};
        }

        String[] parts = configured.split(",");
        int[] sizes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            sizes[i] = Integer.parseInt(parts[i].trim());
        }
        return sizes;
    }

    private int[] sqlSizes() {
        String configured = System.getProperty("priluka.parser.sql.bytes");
        if (configured == null || configured.trim().isEmpty()) {
            return new int[] {1024, 10 * 1024, 50 * 1024};
        }

        String[] parts = configured.split(",");
        int[] sizes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            sizes[i] = Integer.parseInt(parts[i].trim());
        }
        return sizes;
    }

    private int[] sqlFieldCounts() {
        return intList("priluka.parser.sql.fields", new int[] {100, 1000});
    }

    private int[] sqlJoinCounts() {
        return intList("priluka.parser.sql.joins", new int[] {10, 100});
    }

    private int[] intList(String property, int[] defaultValue) {
        String configured = System.getProperty(property);
        if (configured == null || configured.trim().isEmpty()) {
            return defaultValue;
        }

        String[] parts = configured.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i].trim());
        }
        return values;
    }

    private String generatedNumberList(int targetBytes) {
        StringBuilder result = new StringBuilder(targetBytes + 16);
        int index = 0;
        while (result.length() < targetBytes) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(10000 + (index % 90000));
            index++;
        }
        return result.toString();
    }

    private String generatedSimplifiedSql(int targetBytes) {
        StringBuilder selectList = new StringBuilder(targetBytes + 128);
        selectList.append("t.name, t.firstname, p.*");
        int index = 0;
        while (selectList.length() < targetBytes) {
            selectList.append(", t.col").append(index);
            index++;
        }
        return "select " + selectList
            + " from table1 t1"
            + " left join table2 t2 on t1.id=t2.id"
            + " right join table3 t3 on t2.id=t3.id"
            + " outer join table4 t4 on t3.id=t4.id"
            + " inner join table5 t5 on t4.id=t5.id"
            + " join table6 t6 on t5.id=t6.id";
    }

    private String generatedJoinHeavySimplifiedSql(int fieldCount, int joinCount) {
        StringBuilder result = new StringBuilder(fieldCount * 12 + joinCount * 48 + 64);
        result.append("select p.*");
        for (int i = 0; i < fieldCount; i++) {
            result.append(", t").append(i).append(".col").append(i);
        }
        result.append(" from table0 t0");
        for (int i = 1; i <= joinCount; i++) {
            result.append(' ')
                .append(joinSpec(i))
                .append(" table")
                .append(i)
                .append(" t")
                .append(i)
                .append(" on t")
                .append(i - 1)
                .append(".id=t")
                .append(i)
                .append(".id");
        }
        return result.toString();
    }

    private String generatedSqlFindText(int targetBytes) {
        String[] valid = new String[] {
            " select t1.name from table1 t1 join table2 t2 on t1.id=t2.id ",
            " select p.*, t.name, t.firstname from db.person p left join db.team t on p.team_id=t.id ",
            " select t0.col0, t1.col1 from table0 t0 right join table1 t1 on t0.id=t1.id outer join table2 t2 on t1.id=t2.id "
        };
        String[] invalid = new String[] {
            " select , from broken0 ",
            " select name , , from broken1 ",
            " select . name from broken2 ",
            " select * * from broken3 ",
            " select from missing_list ",
            " select name table_without_from ",
            " select name from . broken4 ",
            " select name from , broken5 ",
            " select name from = broken6 ",
            " select name from * broken7 "
        };

        StringBuilder result = new StringBuilder(targetBytes + 256);
        int validIndex = 0;
        int invalidIndex = 0;
        int chunk = 0;
        while (result.length() < targetBytes) {
            result.append(" noise").append(chunk).append(" alpha beta gamma ");
            if (invalidIndex < invalid.length && chunk % 4 == 1) {
                result.append(invalid[invalidIndex++]);
            }
            if (validIndex < valid.length && chunk % 9 == 4) {
                result.append(valid[validIndex++]);
            }
            chunk++;
        }
        while (invalidIndex < invalid.length) {
            result.append(invalid[invalidIndex++]);
        }
        while (validIndex < valid.length) {
            result.append(valid[validIndex++]);
        }
        return result.toString();
    }

    private void assertFoundSqlCount(NfaRecognizer recognizer, String input, int expected) {
        assertFoundCount("valid SQL queries", recognizer, input, expected);
    }

    private void assertFoundCount(String label, NfaRecognizer recognizer, String input, int expected) {
        int found = recognizer.findAll(input).size();
        if (found != expected) {
            throw new AssertionError("Expected " + expected + " " + label + ", found " + found);
        }
    }

    private Lexer presentPerfectLexer(GrammarModel model) {
        List<TerminalSymbol> terminals = new ArrayList<TerminalSymbol>(model.getTerminals());
        terminals.add(GrammarModelBuilder.terminalSymbol(PresentPerfectGrammar.WordToken.class));
        terminals.add(new TerminalSymbol(PerfWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return Lexers.defaultLexer(new LexerSpec(terminals), LexerOptions.DEFAULT);
    }

    private Lexer presentPerfectAsciiWordLexer(GrammarModel model) {
        List<TerminalSymbol> terminals = new ArrayList<TerminalSymbol>(model.getTerminals());
        terminals.add(GrammarModelBuilder.terminalSymbol(PresentPerfectGrammar.WordToken.class));
        return Lexers.asciiWord(new LexerSpec(terminals), PresentPerfectGrammar.WordToken.class);
    }

    private Map<String, Integer> presentPerfectKeywordMap() {
        Map<String, Integer> result = new HashMap<String, Integer>();
        addEnumKeywords(result, 1, PresentPerfectGrammar.Pronoun.class);
        addEnumKeywords(result, 2, PresentPerfectGrammar.HaveHas.class);
        addEnumKeywords(result, 3, PresentPerfectGrammar.ParticipleVerb.class);
        return result;
    }

    private void addEnumKeywords(Map<String, Integer> keywords, int kind, Class<? extends Enum<?>> enumType) {
        Enum<?>[] constants = enumType.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            keywords.put(constants[i].name().toLowerCase(Locale.ROOT), Integer.valueOf(kind));
        }
    }

    private ScannerStats scanWords(String input, Map<String, Integer> keywords) {
        int words = 0;
        int keywordHits = 0;
        int position = 0;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (!isAsciiLetter(c)) {
                position++;
                continue;
            }

            int start = position;
            position++;
            while (position < input.length() && isAsciiLetter(input.charAt(position))) {
                position++;
            }

            words++;
            Integer keyword = keywords.get(input.substring(start, position));
            if (keyword != null) {
                keywordHits++;
            }
        }
        return new ScannerStats(words, keywordHits);
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private String generatedPresentPerfectText(int targetBytes) {
        String[] valid = new String[] {
            " i have started ",
            " he has done ",
            " they have finished ",
            " we have eaten ",
            " she has written "
        };
        String[] safeChunks = new String[] {
            " started they finished have done she gone has eaten we ",
            " written it spoken have driven you seen has taken i ",
            " made they known has found we kept have held she ",
            " built it left have won you lost has met he ",
            " read they paid have said we thought has brought i "
        };

        StringBuilder result = new StringBuilder(targetBytes + 256);
        int validIndex = 0;
        int chunk = 0;
        while (result.length() < targetBytes) {
            result.append(safeChunks[chunk % safeChunks.length]);
            if (validIndex < valid.length && chunk % 17 == 8) {
                result.append(valid[validIndex++]);
            }
            chunk++;
        }
        while (validIndex < valid.length) {
            result.append(valid[validIndex++]);
        }
        return result.toString();
    }

    private String joinSpec(int index) {
        switch (index % 5) {
            case 1:
                return "left join";
            case 2:
                return "right join";
            case 3:
                return "outer join";
            case 4:
                return "inner join";
            default:
                return "join";
        }
    }

    private static final class EngineRun {
        private final String label;
        private final ParseEngine engine;
        private final TraceObjectBuilder builder = new TraceObjectBuilder();

        private EngineRun(String label, ParseEngine engine) {
            this.label = label;
            this.engine = engine;
        }

        private NumberArray parse(String input) {
            ParseTrace trace = engine.parseTrace(NumberArray.class, input);
            return builder.build(NumberArray.class, trace);
        }
    }

    private static final class SqlEngineRun {
        private final String label;
        private final ParseEngine engine;
        private final TraceObjectBuilder builder = new TraceObjectBuilder();

        private SqlEngineRun(String label, ParseEngine engine) {
            this.label = label;
            this.engine = engine;
        }

        private SqlEngineRun withLabel(String label) {
            return new SqlEngineRun(label, engine);
        }

        private SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement parse(String input) {
            ParseTrace trace = engine.parseTrace(
                SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class,
                input
            );
            return builder.build(SimplifiedSqlSelectParserTest.SimpleSqlGrammar.SelectStatement.class, trace);
        }
    }

    private static final class Result {
        private final String label;
        private final int bytes;
        private final int values;
        private final double averageSeconds;
        private final double mebibytesPerSecond;
        private final double valuesPerSecond;

        private Result(String label, int bytes, int values, double averageSeconds) {
            this.label = label;
            this.bytes = bytes;
            this.values = values;
            this.averageSeconds = averageSeconds;
            this.mebibytesPerSecond = (bytes / (1024.0 * 1024.0)) / averageSeconds;
            this.valuesPerSecond = values / averageSeconds;
        }

        @Override
        public String toString() {
            return String.format(
                "%s bytes=%d values=%d avg=%.4fs speed=%.2f MiB/s values=%.0f/s",
                label,
                Integer.valueOf(bytes),
                Integer.valueOf(values),
                Double.valueOf(averageSeconds),
                Double.valueOf(mebibytesPerSecond),
                Double.valueOf(valuesPerSecond)
            );
        }
    }

    private static final class FindResult {
        private final String label;
        private final int bytes;
        private final int found;
        private final double averageSeconds;
        private final double mebibytesPerSecond;

        private FindResult(String label, int bytes, int found, double averageSeconds) {
            this.label = label;
            this.bytes = bytes;
            this.found = found;
            this.averageSeconds = averageSeconds;
            this.mebibytesPerSecond = (bytes / (1024.0 * 1024.0)) / averageSeconds;
        }

        @Override
        public String toString() {
            return String.format(
                "%s bytes=%d valid=%d avg=%.4fs speed=%.2f MiB/s",
                label,
                Integer.valueOf(bytes),
                Integer.valueOf(found),
                Double.valueOf(averageSeconds),
                Double.valueOf(mebibytesPerSecond)
            );
        }
    }

    private static final class LexerResult {
        private final String label;
        private final int bytes;
        private final int tokens;
        private final double averageSeconds;
        private final double mebibytesPerSecond;
        private final double tokensPerSecond;

        private LexerResult(String label, int bytes, int tokens, double averageSeconds) {
            this.label = label;
            this.bytes = bytes;
            this.tokens = tokens;
            this.averageSeconds = averageSeconds;
            this.mebibytesPerSecond = (bytes / (1024.0 * 1024.0)) / averageSeconds;
            this.tokensPerSecond = tokens / averageSeconds;
        }

        @Override
        public String toString() {
            return String.format(
                "%s bytes=%d tokens=%d avg=%.4fs speed=%.2f MiB/s tokens=%.0f/s",
                label,
                Integer.valueOf(bytes),
                Integer.valueOf(tokens),
                Double.valueOf(averageSeconds),
                Double.valueOf(mebibytesPerSecond),
                Double.valueOf(tokensPerSecond)
            );
        }
    }

    private static final class ScannerStats {
        private final int words;
        private final int keywordHits;

        private ScannerStats(int words, int keywordHits) {
            this.words = words;
            this.keywordHits = keywordHits;
        }
    }

    private static final class ScannerResult {
        private final String label;
        private final int bytes;
        private final int words;
        private final int keywordHits;
        private final double averageSeconds;
        private final double mebibytesPerSecond;
        private final double wordsPerSecond;

        private ScannerResult(String label, int bytes, int words, int keywordHits, double averageSeconds) {
            this.label = label;
            this.bytes = bytes;
            this.words = words;
            this.keywordHits = keywordHits;
            this.averageSeconds = averageSeconds;
            this.mebibytesPerSecond = (bytes / (1024.0 * 1024.0)) / averageSeconds;
            this.wordsPerSecond = words / averageSeconds;
        }

        @Override
        public String toString() {
            return String.format(
                "%s bytes=%d words=%d keywords=%d avg=%.4fs speed=%.2f MiB/s words=%.0f/s",
                label,
                Integer.valueOf(bytes),
                Integer.valueOf(words),
                Integer.valueOf(keywordHits),
                Double.valueOf(averageSeconds),
                Double.valueOf(mebibytesPerSecond),
                Double.valueOf(wordsPerSecond)
            );
        }
    }

    static class NumberArray {
        final Integer[] numbers;

        NumberArray(@Separator(Comma.class) Integer[] numbers) {
            this.numbers = numbers;
        }
    }

    @Keyword(",")
    static class Comma {
    }

    static class PerfWhitespace {
    }

    static final class PresentPerfectGrammar {
        static class SentencePerfect {
            final Pronoun pronoun;
            final HaveHas haveHas;
            final Verb3Form verb;

            SentencePerfect(Pronoun pronoun, HaveHas haveHas, Verb3Form verb) {
                this.pronoun = pronoun;
                this.haveHas = haveHas;
                this.verb = verb;
            }
        }

        @Terminal(regexp = "[A-Za-z]+")
        static class WordToken {
        }

        interface VerbInf {
        }

        interface Verb2Form {
        }

        interface Verb3Form {
        }

        @Keywords(caseSensitive = false)
        enum Pronoun {
            I,
            YOU,
            HE,
            SHE,
            IT,
            WE,
            THEY
        }

        @Keywords(caseSensitive = false)
        enum HaveHas {
            HAVE,
            HAS
        }

        @Keywords(caseSensitive = false)
        enum InfinitiveVerb implements VerbInf {
            ACCEPT,
            ADD,
            ALLOW,
            ANSWER,
            ARRIVE,
            ASK,
            BAKE,
            BELIEVE,
            CALL,
            CARRY,
            CHANGE,
            CLEAN,
            CLOSE,
            COOK,
            CREATE,
            DANCE,
            DECIDE,
            DELIVER,
            DISCUSS,
            ENJOY,
            FINISH,
            FOLLOW,
            HELP,
            HOPE,
            INVITE,
            JOIN,
            LEARN,
            LIKE,
            LISTEN,
            LIVE,
            LOOK,
            MOVE,
            NEED,
            OPEN,
            PLAY,
            PREFER,
            PREPARE,
            RAIN,
            REMEMBER,
            START,
            STAY,
            STOP,
            STUDY,
            TALK,
            TRAVEL,
            TRY,
            USE,
            VISIT,
            WAIT,
            WALK,
            WANT,
            WATCH,
            WORK,
            WRITE,
            DO,
            GO,
            EAT,
            SEE,
            TAKE,
            MAKE,
            KNOW,
            FIND,
            KEEP,
            HOLD,
            BUILD,
            LEAVE,
            WIN,
            LOSE,
            MEET,
            READ,
            PAY,
            SAY,
            THINK,
            BRING,
            BUY,
            CATCH,
            CHOOSE,
            COME,
            DRINK,
            DRIVE,
            FALL,
            FEEL,
            FLY,
            FORGET,
            GET,
            GIVE,
            GROW,
            HEAR,
            HIDE,
            RIDE,
            RING,
            RUN,
            SEND,
            SING,
            SIT,
            SLEEP,
            SPEAK,
            SPEND,
            STAND,
            SWIM,
            TEACH,
            TELL,
            UNDERSTAND
        }

        @Keywords(caseSensitive = false)
        enum PastVerb implements Verb2Form {
            ACCEPTED,
            ADDED,
            ALLOWED,
            ANSWERED,
            ARRIVED,
            ASKED,
            BAKED,
            BELIEVED,
            CALLED,
            CARRIED,
            CHANGED,
            CLEANED,
            CLOSED,
            COOKED,
            CREATED,
            DANCED,
            DECIDED,
            DELIVERED,
            DISCUSSED,
            ENJOYED,
            FINISHED,
            FOLLOWED,
            HELPED,
            HOPED,
            INVITED,
            JOINED,
            LEARNED,
            LIKED,
            LISTENED,
            LIVED,
            LOOKED,
            MOVED,
            NEEDED,
            OPENED,
            PLAYED,
            PREFERRED,
            PREPARED,
            RAINED,
            REMEMBERED,
            STARTED,
            STAYED,
            STOPPED,
            STUDIED,
            TALKED,
            TRAVELED,
            TRIED,
            USED,
            VISITED,
            WAITED,
            WALKED,
            WANTED,
            WATCHED,
            WORKED,
            WROTE,
            DID,
            WENT,
            ATE,
            SAW,
            TOOK,
            MADE,
            KNEW,
            FOUND,
            KEPT,
            HELD,
            BUILT,
            LEFT,
            WON,
            LOST,
            MET,
            READ,
            PAID,
            SAID,
            THOUGHT,
            BROUGHT,
            BOUGHT,
            CAUGHT,
            CHOSE,
            CAME,
            DRANK,
            DROVE,
            FELL,
            FELT,
            FLEW,
            FORGOT,
            GOT,
            GAVE,
            GREW,
            HEARD,
            HID,
            RODE,
            RANG,
            RAN,
            SENT,
            SANG,
            SAT,
            SLEPT,
            SPOKE,
            SPENT,
            STOOD,
            SWAM,
            TAUGHT,
            TOLD,
            UNDERSTOOD
        }

        @Keywords(caseSensitive = false)
        enum ParticipleVerb implements Verb3Form {
            ACCEPTED,
            ADDED,
            ALLOWED,
            ANSWERED,
            ARRIVED,
            ASKED,
            BAKED,
            BELIEVED,
            CALLED,
            CARRIED,
            CHANGED,
            CLEANED,
            CLOSED,
            COOKED,
            CREATED,
            DANCED,
            DECIDED,
            DELIVERED,
            DISCUSSED,
            ENJOYED,
            FINISHED,
            FOLLOWED,
            HELPED,
            HOPED,
            INVITED,
            JOINED,
            LEARNED,
            LIKED,
            LISTENED,
            LIVED,
            LOOKED,
            MOVED,
            NEEDED,
            OPENED,
            PLAYED,
            PREFERRED,
            PREPARED,
            RAINED,
            REMEMBERED,
            STARTED,
            STAYED,
            STOPPED,
            STUDIED,
            TALKED,
            TRAVELED,
            TRIED,
            USED,
            VISITED,
            WAITED,
            WALKED,
            WANTED,
            WATCHED,
            WORKED,
            WRITTEN,
            DONE,
            GONE,
            EATEN,
            SEEN,
            TAKEN,
            MADE,
            KNOWN,
            FOUND,
            KEPT,
            HELD,
            BUILT,
            LEFT,
            WON,
            LOST,
            MET,
            READ,
            PAID,
            SAID,
            THOUGHT,
            BROUGHT,
            BOUGHT,
            CAUGHT,
            CHOSEN,
            COME,
            DRUNK,
            DRIVEN,
            FALLEN,
            FELT,
            FLOWN,
            FORGOTTEN,
            GOT,
            GIVEN,
            GROWN,
            HEARD,
            HIDDEN,
            RIDDEN,
            RUNG,
            RUN,
            SENT,
            SUNG,
            SAT,
            SLEPT,
            SPOKEN,
            SPENT,
            STOOD,
            SWUM,
            TAUGHT,
            TOLD,
            UNDERSTOOD
        }
    }
}
