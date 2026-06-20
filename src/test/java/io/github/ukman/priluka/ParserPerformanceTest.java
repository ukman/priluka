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

    private Result result(String label, String input, NumberArray value, long totalNanos) {
        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        return new Result(label, input.length(), value.numbers.length, averageSeconds);
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
