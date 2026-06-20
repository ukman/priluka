package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.internal.nfa.NfaParseEngine;
import io.github.ukman.priluka.internal.parser.ParseEngine;
import io.github.ukman.priluka.internal.parser.ReflectiveParser;
import io.github.ukman.priluka.internal.parser.TraceObjectBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

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
            + " left join table3 t3 on t2.id=t3.id"
            + " left join table4 t4 on t3.id=t4.id";
    }

    private String generatedJoinHeavySimplifiedSql(int fieldCount, int joinCount) {
        StringBuilder result = new StringBuilder(fieldCount * 12 + joinCount * 48 + 64);
        result.append("select p.*");
        for (int i = 0; i < fieldCount; i++) {
            result.append(", t").append(i).append(".col").append(i);
        }
        result.append(" from table0 t0");
        for (int i = 1; i <= joinCount; i++) {
            result.append(" left join table")
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

    static class NumberArray {
        final Integer[] numbers;

        NumberArray(@Separator(Comma.class) Integer[] numbers) {
            this.numbers = numbers;
        }
    }

    @Keyword(",")
    static class Comma {
    }
}
