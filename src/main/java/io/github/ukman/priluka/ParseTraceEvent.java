package io.github.ukman.priluka;

import io.github.ukman.priluka.grammar.Production;

public final class ParseTraceEvent {
    public enum Kind {
        BEGIN_PRODUCTION,
        END_PRODUCTION,
        CONSUME_TERMINAL,
        BEGIN_REPEAT,
        APPEND_REPEAT_ELEMENT,
        END_REPEAT,
        BEGIN_OPTIONAL,
        END_OPTIONAL
    }

    private final Kind kind;
    private final Production production;
    private final Class<?> terminalType;
    private final String text;
    private final int start;
    private final int len;
    private final String symbolName;
    private final Boolean present;
    private final Integer count;

    private ParseTraceEvent(
        Kind kind,
        Production production,
        Class<?> terminalType,
        String text,
        int start,
        int len,
        String symbolName,
        Boolean present,
        Integer count
    ) {
        this.kind = kind;
        this.production = production;
        this.terminalType = terminalType;
        this.text = text;
        this.start = start;
        this.len = len;
        this.symbolName = symbolName;
        this.present = present;
        this.count = count;
    }

    public static ParseTraceEvent beginProduction(Production production) {
        return new ParseTraceEvent(Kind.BEGIN_PRODUCTION, production, null, null, -1, -1, null, null, null);
    }

    public static ParseTraceEvent endProduction(Production production) {
        return new ParseTraceEvent(Kind.END_PRODUCTION, production, null, null, -1, -1, null, null, null);
    }

    public static ParseTraceEvent consumeTerminal(Class<?> terminalType, String text, int start, int len) {
        return new ParseTraceEvent(Kind.CONSUME_TERMINAL, null, terminalType, text, start, len, null, null, null);
    }

    public static ParseTraceEvent beginRepeat(String symbolName) {
        return new ParseTraceEvent(Kind.BEGIN_REPEAT, null, null, null, -1, -1, symbolName, null, null);
    }

    public static ParseTraceEvent appendRepeatElement(String symbolName) {
        return new ParseTraceEvent(Kind.APPEND_REPEAT_ELEMENT, null, null, null, -1, -1, symbolName, null, null);
    }

    public static ParseTraceEvent endRepeat(String symbolName, int count) {
        return new ParseTraceEvent(Kind.END_REPEAT, null, null, null, -1, -1, symbolName, null, Integer.valueOf(count));
    }

    public static ParseTraceEvent beginOptional(String symbolName) {
        return new ParseTraceEvent(Kind.BEGIN_OPTIONAL, null, null, null, -1, -1, symbolName, null, null);
    }

    public static ParseTraceEvent endOptional(String symbolName, boolean present) {
        return new ParseTraceEvent(
            Kind.END_OPTIONAL,
            null,
            null,
            null,
            -1,
            -1,
            symbolName,
            Boolean.valueOf(present),
            null
        );
    }

    public Kind getKind() {
        return kind;
    }

    public Production getProduction() {
        return production;
    }

    public Class<?> getTerminalType() {
        return terminalType;
    }

    public String getText() {
        return text;
    }

    public int getStart() {
        return start;
    }

    public int getLen() {
        return len;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public Boolean getPresent() {
        return present;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public String toString() {
        switch (kind) {
            case BEGIN_PRODUCTION:
                return "beginProduction(" + production.toBnf() + ")";
            case END_PRODUCTION:
                return "endProduction(" + production.toBnf() + ")";
            case CONSUME_TERMINAL:
                return "consumeTerminal("
                    + terminalType.getSimpleName()
                    + ", \""
                    + escape(text)
                    + "\", start="
                    + start
                    + ", len="
                    + len
                    + ")";
            case BEGIN_REPEAT:
                return "beginRepeat(" + symbolName + ")";
            case APPEND_REPEAT_ELEMENT:
                return "appendRepeatElement(" + symbolName + ")";
            case END_REPEAT:
                return "endRepeat(" + symbolName + ", count=" + count + ")";
            case BEGIN_OPTIONAL:
                return "beginOptional(" + symbolName + ")";
            case END_OPTIONAL:
                return "endOptional(" + symbolName + ", " + (present.booleanValue() ? "present" : "absent") + ")";
            default:
                throw new IllegalStateException("Unknown trace event kind: " + kind);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
