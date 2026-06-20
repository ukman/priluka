package io.github.ukman.priluka.internal.lexer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RegexEnginePerformanceTest {
    private static final int TARGET_BYTES = Integer.getInteger("priluka.perf.bytes", 5 * 1024 * 1024);
    private static final int WARMUP_RUNS = Integer.getInteger("priluka.perf.warmup", 3);
    private static final int MEASURE_RUNS = Integer.getInteger("priluka.perf.runs", 5);

    @Test
    void countNumbersWithPlainJavaRegex() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual regex baseline. Run with -Dpriluka.perf=true -Dtest=RegexEnginePerformanceTest."
        );

        String input = generateNumberInput(TARGET_BYTES);
        Pattern pattern = Pattern.compile("[0-9]+");

        Result result = measure("java-regex-number-find", pattern, input);

        System.out.println(result);
    }

    @Test
    void countMixedTokensWithPlainJavaRegex() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual regex baseline. Run with -Dpriluka.perf=true -Dtest=RegexEnginePerformanceTest."
        );

        String input = generateMixedInput(TARGET_BYTES);
        Pattern pattern = Pattern.compile(
            "\\s+|[A-Za-z_][A-Za-z0-9_]*|[0-9]+|\"([^\"\\\\]|\\\\.)*\"|[+\\-=()\\[\\]{}.,*/?]"
        );

        Result result = measure("java-regex-mixed-token-find", pattern, input);

        System.out.println(result);
    }

    private Result measure(String name, Pattern pattern, String input) {
        int tokenCount = 0;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            tokenCount = count(pattern, input);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            tokenCount = count(pattern, input);
            totalNanos += System.nanoTime() - start;
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        double megabytes = input.length() / (1024.0 * 1024.0);
        return new Result(name, input.length(), tokenCount, averageSeconds, megabytes / averageSeconds);
    }

    private int count(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String generateNumberInput(int targetBytes) {
        Random random = new Random(123456789L);
        StringBuilder builder = new StringBuilder(targetBytes + 16);
        while (builder.length() < targetBytes) {
            int len = 1 + random.nextInt(10);
            for (int i = 0; i < len; i++) {
                builder.append((char) ('0' + random.nextInt(10)));
            }
            builder.append(' ');
        }
        return builder.toString();
    }

    private String generateMixedInput(int targetBytes) {
        Random random = new Random(987654321L);
        String[] ids = {
            "alpha", "beta_2", "gammaValue", "if", "else", "for", "while", "return", "className", "item123"
        };
        String[] strings = {
            "\"hello\"",
            "\"quoted string\"",
            "\"escaped \\\" quote\"",
            "\"path\\\\to\\\\file\""
        };
        char[] operators = "+-=()[]{}.,*/?".toCharArray();

        StringBuilder builder = new StringBuilder(targetBytes + 64);
        while (builder.length() < targetBytes) {
            int kind = random.nextInt(5);
            if (kind == 0) {
                appendNumber(builder, random);
            } else if (kind == 1) {
                builder.append(ids[random.nextInt(ids.length)]);
            } else if (kind == 2) {
                builder.append(strings[random.nextInt(strings.length)]);
            } else if (kind == 3) {
                builder.append(operators[random.nextInt(operators.length)]);
            } else {
                int spaces = 1 + random.nextInt(4);
                for (int i = 0; i < spaces; i++) {
                    builder.append(' ');
                }
                continue;
            }
            builder.append(' ');
        }
        return builder.toString();
    }

    private void appendNumber(StringBuilder builder, Random random) {
        int len = 1 + random.nextInt(10);
        for (int i = 0; i < len; i++) {
            builder.append((char) ('0' + random.nextInt(10)));
        }
    }

    private static final class Result {
        private final String name;
        private final int bytes;
        private final int tokenCount;
        private final double averageSeconds;
        private final double megabytesPerSecond;

        private Result(String name, int bytes, int tokenCount, double averageSeconds, double megabytesPerSecond) {
            this.name = name;
            this.bytes = bytes;
            this.tokenCount = tokenCount;
            this.averageSeconds = averageSeconds;
            this.megabytesPerSecond = megabytesPerSecond;
        }

        @Override
        public String toString() {
            return String.format(
                "%s: bytes=%d numbers=%d avg=%.4fs speed=%.2f MiB/s",
                name,
                Integer.valueOf(bytes),
                Integer.valueOf(tokenCount),
                Double.valueOf(averageSeconds),
                Double.valueOf(megabytesPerSecond)
            );
        }
    }
}
