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
    private final int minOccurrences;
    private final int maxOccurrences;
    private final boolean noHardBoundary;

    private ProductionPart(
        String symbolName,
        Class<?> symbolType,
        Quantifier quantifier,
        Class<?> separatorType,
        boolean trailingSeparator,
        int minOccurrences,
        int maxOccurrences,
        boolean noHardBoundary
    ) {
        this.symbolName = symbolName;
        this.symbolType = symbolType;
        this.quantifier = quantifier;
        this.separatorType = separatorType;
        this.trailingSeparator = trailingSeparator;
        this.minOccurrences = minOccurrences;
        this.maxOccurrences = maxOccurrences;
        this.noHardBoundary = noHardBoundary;
    }

    public static ProductionPart one(Class<?> symbolType) {
        return new ProductionPart(symbolType.getSimpleName(), symbolType, Quantifier.ONE, null, false, 1, 1, false);
    }

    public static ProductionPart optional(Class<?> symbolType) {
        return new ProductionPart(symbolType.getSimpleName(), symbolType, Quantifier.OPTIONAL, null, false, 0, 1, false);
    }

    public static ProductionPart repeated(Class<?> symbolType, boolean oneOrMore) {
        return repeated(symbolType, oneOrMore ? 1 : 0, -1);
    }

    public static ProductionPart repeated(Class<?> symbolType, int minOccurrences, int maxOccurrences) {
        return new ProductionPart(
            symbolType.getSimpleName(),
            symbolType,
            minOccurrences > 0 ? Quantifier.ONE_OR_MORE : Quantifier.ZERO_OR_MORE,
            null,
            false,
            minOccurrences,
            maxOccurrences,
            false
        );
    }

    public static ProductionPart separated(Class<?> symbolType, Class<?> separatorType, boolean oneOrMore, boolean trailing) {
        return separated(symbolType, separatorType, oneOrMore ? 1 : 0, -1, trailing);
    }

    public static ProductionPart separated(
        Class<?> symbolType,
        Class<?> separatorType,
        int minOccurrences,
        int maxOccurrences,
        boolean trailing
    ) {
        return new ProductionPart(
            symbolType.getSimpleName(),
            symbolType,
            minOccurrences > 0 ? Quantifier.ONE_OR_MORE : Quantifier.ZERO_OR_MORE,
            separatorType,
            trailing,
            minOccurrences,
            maxOccurrences,
            false
        );
    }

    public ProductionPart withNoHardBoundary() {
        return new ProductionPart(
            symbolName,
            symbolType,
            quantifier,
            separatorType,
            trailingSeparator,
            minOccurrences,
            maxOccurrences,
            true
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

    public int getMinOccurrences() {
        return minOccurrences;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public boolean hasBoundedOccurrences() {
        return maxOccurrences >= 0;
    }

    public boolean isNoHardBoundary() {
        return noHardBoundary;
    }

    public String toBnf() {
        String boundaryPrefix = noHardBoundary ? "@NoHardBoundary " : "";
        if (separatorType != null) {
            String text = symbolName + " (" + separatorType.getSimpleName() + " " + symbolName + ")*";
            if (trailingSeparator) {
                text = text + " " + separatorType.getSimpleName() + "?";
            }
            if (hasCustomOccurrences()) {
                return boundaryPrefix + "(" + text + ")" + occurrenceSuffix();
            }
            if (quantifier == Quantifier.ZERO_OR_MORE) {
                return boundaryPrefix + "(empty | " + text + ")";
            }
            return boundaryPrefix + "(" + text + ")";
        }
        if (quantifier == Quantifier.OPTIONAL) {
            return boundaryPrefix + symbolName + "?";
        }
        if (quantifier == Quantifier.ZERO_OR_MORE) {
            if (hasCustomOccurrences()) {
                return boundaryPrefix + symbolName + occurrenceSuffix();
            }
            return boundaryPrefix + symbolName + "*";
        }
        if (quantifier == Quantifier.ONE_OR_MORE) {
            if (hasCustomOccurrences()) {
                return boundaryPrefix + symbolName + occurrenceSuffix();
            }
            return boundaryPrefix + symbolName + "+";
        }
        if (hasCustomOccurrences()) {
            return boundaryPrefix + symbolName + occurrenceSuffix();
        }
        return boundaryPrefix + symbolName;
    }

    private boolean hasCustomOccurrences() {
        return quantifier == Quantifier.ZERO_OR_MORE && (minOccurrences != 0 || maxOccurrences != -1)
            || quantifier == Quantifier.ONE_OR_MORE && (minOccurrences != 1 || maxOccurrences != -1);
    }

    private String occurrenceSuffix() {
        if (maxOccurrences < 0) {
            return "{" + minOccurrences + ",}";
        }
        if (minOccurrences == maxOccurrences) {
            return "{" + minOccurrences + "}";
        }
        return "{" + minOccurrences + "," + maxOccurrences + "}";
    }
}
