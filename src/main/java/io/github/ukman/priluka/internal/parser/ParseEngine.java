package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.ParseTrace;

public interface ParseEngine {
    ParseTrace parseTrace(Class<?> start, String input);
}
