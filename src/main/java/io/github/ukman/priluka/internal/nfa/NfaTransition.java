package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;

public final class NfaTransition {
    public enum Kind {
        EPSILON,
        BEGIN_PRODUCTION,
        END_PRODUCTION,
        TERMINAL
    }

    private final NfaState from;
    private final NfaState to;
    private final Kind kind;
    private final Class<?> symbolType;
    private final Production production;
    private final ProductionPart part;

    private NfaTransition(
        NfaState from,
        NfaState to,
        Kind kind,
        Class<?> symbolType,
        Production production,
        ProductionPart part
    ) {
        this.from = from;
        this.to = to;
        this.kind = kind;
        this.symbolType = symbolType;
        this.production = production;
        this.part = part;
    }

    static NfaTransition epsilon(NfaState from, NfaState to) {
        return new NfaTransition(from, to, Kind.EPSILON, null, null, null);
    }

    static NfaTransition beginProduction(NfaState from, NfaState to, Production production) {
        return new NfaTransition(from, to, Kind.BEGIN_PRODUCTION, null, production, null);
    }

    static NfaTransition endProduction(NfaState from, NfaState to, Production production) {
        return new NfaTransition(from, to, Kind.END_PRODUCTION, null, production, null);
    }

    static NfaTransition terminal(NfaState from, NfaState to, Class<?> symbolType, ProductionPart part) {
        return new NfaTransition(from, to, Kind.TERMINAL, symbolType, null, part);
    }

    public NfaState getFrom() {
        return from;
    }

    public NfaState getTo() {
        return to;
    }

    public Kind getKind() {
        return kind;
    }

    public Class<?> getSymbolType() {
        return symbolType;
    }

    public Production getProduction() {
        return production;
    }

    public ProductionPart getPart() {
        return part;
    }

    @Override
    public String toString() {
        if (kind == Kind.TERMINAL) {
            return from + " -" + symbolType.getSimpleName() + "-> " + to;
        }
        if (kind == Kind.BEGIN_PRODUCTION) {
            return from + " -begin " + production.toBnf() + "-> " + to;
        }
        if (kind == Kind.END_PRODUCTION) {
            return from + " -end " + production.toBnf() + "-> " + to;
        }
        return from + " -eps-> " + to;
    }
}
