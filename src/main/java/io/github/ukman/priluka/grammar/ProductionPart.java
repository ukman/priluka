package io.github.ukman.priluka.grammar;

/**
 * One part of a production body, including optional, repeated, and separated forms.
 */
public final class ProductionPart {
    /**
     * Cardinality of a production part.
     */
    public enum Quantifier {
        ONE,
        OPTIONAL,
        ZERO_OR_MORE,
        ONE_OR_MORE
    }

    private final String symbolName;
    private final Class<?> symbolType;
    private final Quantifier quantifier;
    private final Class<?> separatorType;
    private final boolean trailingSeparator;

    private ProductionPart(
        String symbolName,
        Class<?> symbolType,
        Quantifier quantifier,
        Class<?> separatorType,
        boolean trailingSeparator
    ) {
        this.symbolName = symbolName;
        this.symbolType = symbolType;
        this.quantifier = quantifier;
        this.separatorType = separatorType;
        this.trailingSeparator = trailingSeparator;
    }

    public static ProductionPart one(Class<?> symbolType) {
        return new ProductionPart(symbolType.getSimpleName(), symbolType, Quantifier.ONE, null, false);
    }

    public static ProductionPart optional(Class<?> symbolType) {
        return new ProductionPart(symbolType.getSimpleName(), symbolType, Quantifier.OPTIONAL, null, false);
    }

    public static ProductionPart repeated(Class<?> symbolType, boolean oneOrMore) {
        return new ProductionPart(
            symbolType.getSimpleName(),
            symbolType,
            oneOrMore ? Quantifier.ONE_OR_MORE : Quantifier.ZERO_OR_MORE,
            null,
            false
        );
    }

    public static ProductionPart separated(Class<?> symbolType, Class<?> separatorType, boolean oneOrMore, boolean trailing) {
        return new ProductionPart(
            symbolType.getSimpleName(),
            symbolType,
            oneOrMore ? Quantifier.ONE_OR_MORE : Quantifier.ZERO_OR_MORE,
            separatorType,
            trailing
        );
    }

    public String getSymbolName() {
        return symbolName;
    }

    public Class<?> getSymbolType() {
        return symbolType;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    public Class<?> getSeparatorType() {
        return separatorType;
    }

    public boolean isTrailingSeparator() {
        return trailingSeparator;
    }

    public String toBnf() {
        if (separatorType != null) {
            String text = symbolName + " (" + separatorType.getSimpleName() + " " + symbolName + ")*";
            if (trailingSeparator) {
                text = text + " " + separatorType.getSimpleName() + "?";
            }
            if (quantifier == Quantifier.ZERO_OR_MORE) {
                return "(empty | " + text + ")";
            }
            return "(" + text + ")";
        }
        if (quantifier == Quantifier.OPTIONAL) {
            return symbolName + "?";
        }
        if (quantifier == Quantifier.ZERO_OR_MORE) {
            return symbolName + "*";
        }
        if (quantifier == Quantifier.ONE_OR_MORE) {
            return symbolName + "+";
        }
        return symbolName;
    }
}
