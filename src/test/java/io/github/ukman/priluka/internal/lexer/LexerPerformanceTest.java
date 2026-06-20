package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.grammar.TerminalSymbol;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class LexerPerformanceTest {
    private static final int TARGET_BYTES = Integer.getInteger("priluka.perf.bytes", 5 * 1024 * 1024);
    private static final int WARMUP_RUNS = Integer.getInteger("priluka.perf.warmup", 3);
    private static final int MEASURE_RUNS = Integer.getInteger("priluka.perf.runs", 5);

    @Test
    void compareStrictAndMultiVariantLexers() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual performance baseline. Run with -Dpriluka.perf=true -Dtest=LexerPerformanceTest."
        );
        String input = generateInput(TARGET_BYTES);

        Lexer strictLexer = new Lexer(
            new LexerSpec(Arrays.asList(
                regexp(Spaces.class, "\\s+", true, 0),
                regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0),
                regexp(NumberToken.class, "[0-9]+", false, 0),
                regexp(QuotedString.class, "\"([^\"\\\\]|\\\\.)*\"", false, 0),
                regexp(Operator.class, "[+\\-=()\\[\\]{}.,*/?]", false, 0)
            )),
            LexerOptions.STRICT
        );

        Lexer multiVariantLexer = new Lexer(
            new LexerSpec(Arrays.asList(
                regexp(Spaces.class, "\\s+", true, 0),
                regexp(Id.class, "[A-Za-z_][A-Za-z0-9_]*", false, 0),
                keyword(If.class, "if"),
                keyword(Else.class, "else"),
                keyword(For.class, "for"),
                keyword(While.class, "while"),
                keyword(Return.class, "return"),
                keyword(ClassKeyword.class, "class"),
                keyword(StaticKeyword.class, "static"),
                keyword(VoidKeyword.class, "void"),
                regexp(NumberToken.class, "[0-9]+", false, 0),
                regexp(QuotedString.class, "\"([^\"\\\\]|\\\\.)*\"", false, 0),
                regexp(Operator.class, "[+\\-=()\\[\\]{}.,*/?]", false, 0)
            )),
            LexerOptions.DEFAULT
        );

        Result strict = measure("strict", strictLexer, input);
        Result multiVariant = measure("multi-variant", multiVariantLexer, input);

        System.out.println(strict);
        System.out.println(multiVariant);
        System.out.printf(
            "multi/strict slowdown: %.2fx%n",
            Double.valueOf(strict.megabytesPerSecond / multiVariant.megabytesPerSecond)
        );
    }

    private Result measure(String name, Lexer lexer, String input) {
        int tokenCount = 0;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            tokenCount = lexer.tokenize(input).size();
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            List<Lexeme> lexemes = lexer.tokenize(input);
            totalNanos += System.nanoTime() - start;
            tokenCount = lexemes.size();
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        double megabytes = input.length() / (1024.0 * 1024.0);
        return new Result(name, input.length(), tokenCount, averageSeconds, megabytes / averageSeconds);
    }

    private String generateInput(int targetBytes) {
        String[] chunks = new String[] {
            "if alpha123 = 42, ",
            "else beta = \"hello world\" + gamma_7, ",
            "for item123 ( value [ index ] ) { return value / 3 ? item } ",
            "while condition_1 { className.staticCall(\"quoted string with spaces\") } "
        };
        StringBuilder builder = new StringBuilder(targetBytes + 128);
        int index = 0;
        while (builder.length() < targetBytes) {
            builder.append(chunks[index % chunks.length]);
            index++;
        }
        return builder.toString();
    }

    private TerminalSymbol regexp(Class<?> type, String pattern, boolean skip, int priority) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.REGEXP, pattern, skip, priority);
    }

    private TerminalSymbol keyword(Class<?> type, String text) {
        return new TerminalSymbol(type, TerminalSymbol.Kind.KEYWORD, text, false, 0);
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
                "%s: bytes=%d tokens=%d avg=%.4fs speed=%.2f MiB/s",
                name,
                Integer.valueOf(bytes),
                Integer.valueOf(tokenCount),
                Double.valueOf(averageSeconds),
                Double.valueOf(megabytesPerSecond)
            );
        }
    }

    static class Spaces {
    }

    static class Id {
    }

    static class NumberToken {
    }

    static class QuotedString {
    }

    static class Operator {
    }

    static class If {
    }

    static class Else {
    }

    static class For {
    }

    static class While {
    }

    static class Return {
    }

    static class ClassKeyword {
    }

    static class StaticKeyword {
    }

    static class VoidKeyword {
    }
}
