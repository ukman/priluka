package io.github.ukman.priluka.internal.lexer;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RegexEngineComparisonPerformanceTest {
    private static final int TARGET_BYTES = Integer.getInteger("priluka.perf.bytes", 5 * 1024 * 1024);
    private static final int WARMUP_RUNS = Integer.getInteger("priluka.perf.warmup", 3);
    private static final int MEASURE_RUNS = Integer.getInteger("priluka.perf.runs", 5);

    private static final TokenSpec[] TOKENS = new TokenSpec[] {
        new TokenSpec("SPACE", "[ ]+", "[ ]+", true),
        new TokenSpec("ID", "[A-Za-z_][A-Za-z0-9_]*", "[A-Za-z_][A-Za-z0-9_]*", false),
        new TokenSpec("NUMBER", "[0-9]+", "[0-9]+", false),
        new TokenSpec("STRING", "\"[^\"]*\"", "\"[^\"]*\"", false),
        new TokenSpec("OP", "[+\\-=()\\[\\]{}.,*/?]", "[+\\-=()\\[\\]{}.,*/?]", false)
    };

    @Test
    void compareTokenizingRegexEngines() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual regex engine comparison. Run with -Dpriluka.perf=true -Dtest=RegexEngineComparisonPerformanceTest."
        );

        String input = generateMixedInput(TARGET_BYTES);
        List<ScannerCase> scanners = new ArrayList<ScannerCase>();
        scanners.add(new ScannerCase("java.util.regex master", new JavaRegexScanner(TOKENS), input));
        scanners.add(new ScannerCase("RE2/J master", new Re2jScanner(TOKENS), input));
        scanners.add(new ScannerCase("dk.brics per-token", new BricsScanner(TOKENS), input));

        for (ScannerCase scanner : scanners) {
            System.out.println(measure(scanner.name, scanner.scanner, scanner.input));
        }
    }

    private Result measure(String name, TokenScanner scanner, String input) {
        int tokenCount = 0;
        for (int i = 0; i < WARMUP_RUNS; i++) {
            tokenCount = scanner.count(input);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            tokenCount = scanner.count(input);
            totalNanos += System.nanoTime() - start;
        }

        double averageSeconds = (totalNanos / (double) MEASURE_RUNS) / 1_000_000_000.0;
        double megabytes = input.length() / (1024.0 * 1024.0);
        return new Result(name, input.length(), tokenCount, averageSeconds, megabytes / averageSeconds);
    }

    private String generateMixedInput(int targetBytes) {
        Random random = new Random(246813579L);
        String[] ids = {
            "alpha", "beta_2", "gammaValue", "if", "else", "for", "while", "return", "className", "item123"
        };
        String[] strings = {
            "\"hello\"",
            "\"quoted string\"",
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

    interface TokenScanner {
        int count(String input);
    }

    private static final class JavaRegexScanner implements TokenScanner {
        private final Pattern pattern;
        private final TokenSpec[] specs;

        private JavaRegexScanner(TokenSpec[] specs) {
            this.specs = specs;
            this.pattern = Pattern.compile(masterRegex(specs));
        }

        @Override
        public int count(String input) {
            Matcher matcher = pattern.matcher(input);
            int position = 0;
            int count = 0;
            while (matcher.find()) {
                if (matcher.start() != position) {
                    throw new IllegalStateException("Gap at " + position);
                }
                int type = matchedGroup(matcher, specs.length);
                if (!specs[type].skip) {
                    count++;
                }
                position = matcher.end();
            }
            if (position != input.length()) {
                throw new IllegalStateException("Gap at " + position);
            }
            return count;
        }

        private int matchedGroup(Matcher matcher, int groupCount) {
            for (int i = 1; i <= groupCount; i++) {
                if (matcher.start(i) >= 0) {
                    return i - 1;
                }
            }
            throw new IllegalStateException("No group matched");
        }
    }

    private static final class Re2jScanner implements TokenScanner {
        private final com.google.re2j.Pattern pattern;
        private final TokenSpec[] specs;

        private Re2jScanner(TokenSpec[] specs) {
            this.specs = specs;
            this.pattern = com.google.re2j.Pattern.compile(masterRegex(specs));
        }

        @Override
        public int count(String input) {
            com.google.re2j.Matcher matcher = pattern.matcher(input);
            int position = 0;
            int count = 0;
            while (matcher.find()) {
                if (matcher.start() != position) {
                    throw new IllegalStateException("Gap at " + position);
                }
                int type = matchedGroup(matcher, specs.length);
                if (!specs[type].skip) {
                    count++;
                }
                position = matcher.end();
            }
            if (position != input.length()) {
                throw new IllegalStateException("Gap at " + position);
            }
            return count;
        }

        private int matchedGroup(com.google.re2j.Matcher matcher, int groupCount) {
            for (int i = 1; i <= groupCount; i++) {
                if (matcher.start(i) >= 0) {
                    return i - 1;
                }
            }
            throw new IllegalStateException("No group matched");
        }
    }

    private static final class BricsScanner implements TokenScanner {
        private final RunAutomaton[] automata;
        private final TokenSpec[] specs;

        private BricsScanner(TokenSpec[] specs) {
            this.specs = specs;
            this.automata = new RunAutomaton[specs.length];
            for (int i = 0; i < specs.length; i++) {
                this.automata[i] = new RunAutomaton(bricsAutomaton(specs[i]));
            }
        }

        @Override
        public int count(String input) {
            int position = 0;
            int count = 0;
            while (position < input.length()) {
                Match match = longestMatch(input, position);
                if (match.length == 0) {
                    throw new IllegalStateException("Gap at " + position);
                }
                if (!specs[match.type].skip) {
                    count++;
                }
                position += match.length;
            }
            return count;
        }

        private Match longestMatch(String input, int position) {
            int bestType = -1;
            int bestLength = 0;
            for (int i = 0; i < automata.length; i++) {
                int length = matchLength(automata[i], input, position);
                if (length > bestLength) {
                    bestType = i;
                    bestLength = length;
                }
            }
            return new Match(bestType, bestLength);
        }

        private int matchLength(RunAutomaton automaton, String input, int position) {
            int state = automaton.getInitialState();
            int best = 0;
            for (int i = position; i < input.length(); i++) {
                state = automaton.step(state, input.charAt(i));
                if (state == -1) {
                    break;
                }
                if (automaton.isAccept(state)) {
                    best = i - position + 1;
                }
            }
            return best;
        }

        private Automaton bricsAutomaton(TokenSpec spec) {
            if ("STRING".equals(spec.name)) {
                Automaton quote = Automaton.makeChar('"');
                Automaton body = Automaton.makeAnyChar().minus(Automaton.makeChar('"')).repeat();
                return quote.concatenate(body).concatenate(Automaton.makeChar('"'));
            }
            if ("OP".equals(spec.name)) {
                return Automaton.makeCharSet("+-=()[]{}.,*/?");
            }
            return new RegExp(spec.bricsRegex).toAutomaton();
        }
    }

    private static String masterRegex(TokenSpec[] specs) {
        StringBuilder builder = new StringBuilder();
        for (TokenSpec spec : specs) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append("((?:").append(spec.javaRegex).append("))");
        }
        return builder.toString();
    }

    private static final class TokenSpec {
        private final String name;
        private final String javaRegex;
        private final String bricsRegex;
        private final boolean skip;

        private TokenSpec(String name, String javaRegex, String bricsRegex, boolean skip) {
            this.name = name;
            this.javaRegex = javaRegex;
            this.bricsRegex = bricsRegex;
            this.skip = skip;
        }
    }

    private static final class Match {
        private final int type;
        private final int length;

        private Match(int type, int length) {
            this.type = type;
            this.length = length;
        }
    }

    private static final class ScannerCase {
        private final String name;
        private final TokenScanner scanner;
        private final String input;

        private ScannerCase(String name, TokenScanner scanner, String input) {
            this.name = name;
            this.scanner = scanner;
            this.input = input;
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
                "%s: bytes=%d tokens=%d avg=%.4fs speed=%.2f MiB/s",
                name,
                Integer.valueOf(bytes),
                Integer.valueOf(tokenCount),
                Double.valueOf(averageSeconds),
                Double.valueOf(megabytesPerSecond)
            );
        }
    }
}
