package io.github.ukman.priluka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered parser trace events that describe one accepted derivation.
 */
public final class ParseTrace {
    private final List<ParseTraceEvent> events;

    public ParseTrace(List<ParseTraceEvent> events) {
        this.events = Collections.unmodifiableList(new ArrayList<ParseTraceEvent>(events));
    }

    public List<ParseTraceEvent> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(events.get(i));
        }
        return builder.toString();
    }
}
