package io.github.ukman.priluka.benchmark;

import io.github.ukman.priluka.FindEngine;
import io.github.ukman.priluka.LexerEngine;
import io.github.ukman.priluka.ParseFindResult;
import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.evidence.MoneyGrammar;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for public {@code findAll(...)} throughput on the Money grammar.
 *
 * Warmup iterations are handled by JMH, while the trial setup performs one
 * priming call so DFA compilation and parser cache population happen before the
 * measured iterations. The benchmark is intentionally single-threaded to keep
 * the result representative of one CPU core and remove scheduling noise.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms4g", "-Xmx4g"})
@Threads(1)
public class MoneyFindAllDfaBenchmark {
    private static final int SEGMENT_BYTES = 64 * 1024;
    private static final String[] MONEY_SAMPLES = {
        "CAD 100",
        "CHF 800",
        "SAR 900",
        "1200 DKK",
        "1300 Norwegian kroner",
        "1400 PLN",
        "1500 zloty",
        "1600 CZK",
        "1700 koruna",
        "1800 HUF",
        "1900 forint",
        "2000 RON",
        "2100 lei",
        "2200 BGN",
        "2300 leva",
        "£10,000,000"
    };
    private static final String[] NOISE_WORDS = {
        "procurement", "notice", "award", "document", "evidence", "record", "section",
        "contract", "review", "summary", "supplier", "reference", "appendix", "window"
    };

    @Param({"10", "20", "100"})
    public int sizeMiB;

    private Parser.InitializedParser parser;
    private String corpus;
    private int expectedMatches;

    @Setup(Level.Trial)
    public void setUp() {
        parser = Parser
            .builder()
            .classes(grammarClasses(MoneyGrammar.class))
            .terminals(MoneyGrammar.Word.class, MoneyGrammar.NumberToken.class, MoneyGrammar.Symbol.class)
            .caseInsensitive()
            .engine(LexerEngine.ASCII_TEXT)
            .findEngine(FindEngine.DFA)
            .build();

        corpus = generateCorpus(sizeMiB * 1024 * 1024);
        expectedMatches = corpus.length() / SEGMENT_BYTES;

        List<ParseFindResult<MoneyGrammar.Money>> primed = parser.findAll(MoneyGrammar.Money.class, corpus);
        if (primed.size() != expectedMatches) {
            throw new IllegalStateException(
                "Unexpected money match count for " + sizeMiB + " MiB corpus: expected "
                    + expectedMatches + " but found " + primed.size()
            );
        }
    }

    @Benchmark
    public int findAllMoneyDfa() {
        return parser.findAll(MoneyGrammar.Money.class, corpus).size();
    }

    private String generateCorpus(int targetBytes) {
        int segments = targetBytes / SEGMENT_BYTES;
        StringBuilder builder = new StringBuilder(targetBytes);
        for (int i = 0; i < segments; i++) {
            builder.append(buildSegment(i));
        }
        return builder.toString();
    }

    private String buildSegment(int index) {
        String sample = MONEY_SAMPLES[index % MONEY_SAMPLES.length];
        StringBuilder builder = new StringBuilder(SEGMENT_BYTES);
        builder.append("procurement notice evidence trail ");
        builder.append(sample);
        builder.append(" benchmark corpus ");
        while (builder.length() < SEGMENT_BYTES) {
            builder.append(NOISE_WORDS[(index + builder.length()) % NOISE_WORDS.length]).append(' ');
        }
        if (builder.length() > SEGMENT_BYTES) {
            builder.setLength(SEGMENT_BYTES);
        }
        return builder.toString();
    }

    private static Class<?>[] grammarClasses(Class<?> root) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        collect(root, classes);
        return classes.toArray(new Class<?>[0]);
    }

    private static void collect(Class<?> type, List<Class<?>> classes) {
        classes.add(type);
        Class<?>[] nested = type.getDeclaredClasses();
        for (int i = 0; i < nested.length; i++) {
            collect(nested[i], classes);
        }
    }
}
