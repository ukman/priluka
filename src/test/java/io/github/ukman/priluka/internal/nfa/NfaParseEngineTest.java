package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.ParseException;
import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.Parser;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.internal.parser.TraceObjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NfaParseEngineTest {
    @Test
    void emitsTraceThroughParseEngineContract() {
        GrammarModel model = Parser.describe(Point.class);
        NfaParseEngine engine = new NfaParseEngine(model);

        ParseTrace trace = engine.parseTrace(Point.class, "1 2");
        Point point = new TraceObjectBuilder().build(Point.class, trace);

        assertEquals(1, point.x);
        assertEquals(2, point.y);
    }

    @Test
    void rejectsInputThroughParseEngineContract() {
        GrammarModel model = Parser.describe(Point.class);
        NfaParseEngine engine = new NfaParseEngine(model);

        assertThrows(ParseException.class, () -> engine.parseTrace(Point.class, "1"));
    }

    static class Point {
        final Integer x;
        final Integer y;

        public Point(Integer x, Integer y) {
            this.x = x;
            this.y = y;
        }
    }
}
