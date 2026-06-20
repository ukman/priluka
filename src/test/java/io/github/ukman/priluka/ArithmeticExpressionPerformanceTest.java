package io.github.ukman.priluka;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ArithmeticExpressionPerformanceTest {
    private static final int WARMUP_RUNS = Integer.getInteger("priluka.perf.warmup", 1);
    private static final int MEASURE_RUNS = Integer.getInteger("priluka.perf.runs", 3);

    @Test
    void parsesGeneratedArithmeticExpressions() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual arithmetic parser performance test. Run with -Dpriluka.perf=true -Dtest=ArithmeticExpressionPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(ArithmeticExpressionParserTest.ArithmeticGrammar.class);
        int[] sizes = sizes();
        for (int i = 0; i < sizes.length; i++) {
            String expression = generatedExpression(sizes[i]);
            System.out.println(measure(parser, sizes[i], expression));
        }
    }

    private Result measure(Parser.InitializedParser parser, int targetBytes, String expression) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            parser.parse(ArithmeticExpressionParserTest.ArithmeticGrammar.Expression.class, expression);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            parser.parse(ArithmeticExpressionParserTest.ArithmeticGrammar.Expression.class, expression);
            totalNanos += System.nanoTime() - start;
        }

        double averageMillis = (totalNanos / (double) MEASURE_RUNS) / 1_000_000.0;
        return new Result(targetBytes, expression.length(), averageMillis);
    }

    private int[] sizes() {
        String configured = System.getProperty("priluka.arithmetic.bytes");
        if (configured == null || configured.trim().isEmpty()) {
            return new int[] {1024, 5 * 1024, 10 * 1024, 20 * 1024};
        }

        String[] parts = configured.split(",");
        int[] sizes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            sizes[i] = Integer.parseInt(parts[i].trim());
        }
        return sizes;
    }

    private String generatedExpression(int targetBytes) {
        String block = "((1*2-3/4)*(5-4)/2)";
        StringBuilder result = new StringBuilder(targetBytes + block.length());
        result.append(block);
        int index = 0;
        while (result.length() < targetBytes) {
            switch (index % 4) {
                case 0:
                    result.append("+");
                    break;
                case 1:
                    result.append("-");
                    break;
                case 2:
                    result.append("*");
                    break;
                default:
                    result.append("/");
                    break;
            }
            result.append(block);
            index++;
        }
        return result.toString();
    }

    private static final class Result {
        private final int targetBytes;
        private final int bytes;
        private final double averageMillis;
        private final double mebibytesPerSecond;

        private Result(int targetBytes, int bytes, double averageMillis) {
            this.targetBytes = targetBytes;
            this.bytes = bytes;
            this.averageMillis = averageMillis;
            this.mebibytesPerSecond = (bytes / (1024.0 * 1024.0)) / (averageMillis / 1000.0);
        }

        @Override
        public String toString() {
            return String.format(
                "arithmetic-expression targetBytes=%d bytes=%d avg=%.3f ms speed=%.3f MiB/s",
                Integer.valueOf(targetBytes),
                Integer.valueOf(bytes),
                Double.valueOf(averageMillis),
                Double.valueOf(mebibytesPerSecond)
            );
        }
    }
}
