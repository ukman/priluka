package io.github.ukman.priluka.grammar;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grammar production discovered from a constructor or implementation alternative.
 */
public final class Production {
    private final NonterminalSymbol owner;
    private final Constructor<?> constructor;
    private final Class<?> implementation;
    private final List<ProductionPart> parts;

    public Production(
        NonterminalSymbol owner,
        Constructor<?> constructor,
        Class<?> implementation,
        List<ProductionPart> parts
    ) {
        this.owner = owner;
        this.constructor = constructor;
        this.implementation = implementation;
        this.parts = Collections.unmodifiableList(new ArrayList<ProductionPart>(parts));
    }

    public NonterminalSymbol getOwner() {
        return owner;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public Class<?> getImplementation() {
        return implementation;
    }

    public List<ProductionPart> getParts() {
        return parts;
    }

    public String toBnf() {
        StringBuilder builder = new StringBuilder();
        builder.append(owner.getName()).append(" =>");
        if (parts.isEmpty()) {
            builder.append(" empty");
            return builder.toString();
        }
        for (ProductionPart part : parts) {
            builder.append(' ').append(part.toBnf());
        }
        return builder.toString();
    }
}
