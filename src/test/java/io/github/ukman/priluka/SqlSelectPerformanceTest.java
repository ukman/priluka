package io.github.ukman.priluka;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class SqlSelectPerformanceTest {
    private static final int WARMUP_RUNS = Integer.getInteger("priluka.perf.warmup", 1);
    private static final int MEASURE_RUNS = Integer.getInteger("priluka.perf.runs", 3);

    @Test
    void parsesNestedLeftJoinSubqueries() {
        Assumptions.assumeTrue(
            Boolean.getBoolean("priluka.perf"),
            "Manual SQL parser performance test. Run with -Dpriluka.perf=true -Dtest=SqlSelectPerformanceTest."
        );

        Parser.InitializedParser parser = Parser.initFromOuterClass(SqlSelectParserTest.SqlGrammar.class);
        SqlShape shape = sqlShape();
        int[] depths = depths();
        for (int i = 0; i < depths.length; i++) {
            String sql = shape.sql(depths[i]);
            System.out.println(measure(shape.label(), parser, depths[i], sql));
        }
    }

    private Result measure(String label, Parser.InitializedParser parser, int depth, String sql) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            parser.parse(SqlSelectParserTest.SqlGrammar.SelectStatement.class, sql);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            parser.parse(SqlSelectParserTest.SqlGrammar.SelectStatement.class, sql);
            totalNanos += System.nanoTime() - start;
        }

        double averageMillis = (totalNanos / (double) MEASURE_RUNS) / 1_000_000.0;
        return new Result(label, depth, sql.length(), averageMillis);
    }

    private int[] depths() {
        String configured = System.getProperty("priluka.sql.depths");
        if (configured == null || configured.trim().isEmpty()) {
            return new int[] {10, 20, 30};
        }

        String[] parts = configured.split(",");
        int[] depths = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            depths[i] = Integer.parseInt(parts[i].trim());
        }
        return depths;
    }

    private String nestedSelect(int depth) {
        return nestedSelect(1, depth);
    }

    private String nestedSelect(int level, int maxDepth) {
        String alias = level == 1 ? "p1" : "pp" + level;
        if (level == maxDepth) {
            return "select * from person " + alias;
        }

        String joinedAlias = "p" + (level + 1);
        return "select * from person " + alias
            + " left join (" + nestedSelect(level + 1, maxDepth) + ") as " + joinedAlias
            + " on " + alias + ".id = " + joinedAlias + ".id";
    }

    private SqlShape sqlShape() {
        if ("tree".equals(System.getProperty("priluka.sql.shape"))) {
            return new SqlShape() {
                @Override
                public String label() {
                    return "nested-sql-tree";
                }

                @Override
                public String sql(int depth) {
                    return nestedTreeSelect(depth);
                }
            };
        }

        return new SqlShape() {
            @Override
            public String label() {
                return "nested-sql";
            }

            @Override
            public String sql(int depth) {
                return nestedSelect(depth);
            }
        };
    }

    private String nestedTreeSelect(int depth) {
        if (depth <= 1) {
            return "select * from person p left join company c on p.company_id = c.id";
        }

        String leftAlias = "l" + depth;
        String rightAlias = "r" + depth;
        return "select * from (" + nestedTreeSelect(depth - 1) + ") as " + leftAlias
            + " left join (" + nestedTreeSelect(depth - 1) + ") as " + rightAlias
            + " on " + leftAlias + ".company_id = " + rightAlias + ".id";
    }

    private interface SqlShape {
        String label();

        String sql(int depth);
    }

    private static final class Result {
        private final String label;
        private final int depth;
        private final int bytes;
        private final double averageMillis;
        private final double mebibytesPerSecond;

        private Result(String label, int depth, int bytes, double averageMillis) {
            this.label = label;
            this.depth = depth;
            this.bytes = bytes;
            this.averageMillis = averageMillis;
            this.mebibytesPerSecond = (bytes / (1024.0 * 1024.0)) / (averageMillis / 1000.0);
        }

        @Override
        public String toString() {
            return String.format(
                "%s depth=%d bytes=%d avg=%.3f ms speed=%.3f MiB/s",
                label,
                Integer.valueOf(depth),
                Integer.valueOf(bytes),
                Double.valueOf(averageMillis),
                Double.valueOf(mebibytesPerSecond)
            );
        }
    }
}
