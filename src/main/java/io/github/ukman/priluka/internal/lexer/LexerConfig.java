package io.github.ukman.priluka.internal.lexer;

import io.github.ukman.priluka.LexerEngine;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.GrammarModelBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LexerConfig {
    public static final LexerConfig DEFAULT = new LexerConfig(
        new Class<?>[0],
        new Class<?>[0],
        LexerEngine.DEFAULT,
        true
    );

    private final Class<?>[] terminalTypes;
    private final Class<?>[] skipTerminalTypes;
    private final LexerEngine engine;
    private final boolean regexpCaseSensitive;

    public LexerConfig(
        Class<?>[] terminalTypes,
        Class<?>[] skipTerminalTypes,
        LexerEngine engine,
        boolean regexpCaseSensitive
    ) {
        this.terminalTypes = terminalTypes.clone();
        this.skipTerminalTypes = skipTerminalTypes.clone();
        this.engine = engine;
        this.regexpCaseSensitive = regexpCaseSensitive;
    }

    public static LexerConfig terminals(Class<?>... terminalTypes) {
        return new LexerConfig(terminalTypes, new Class<?>[0], LexerEngine.DEFAULT, true);
    }

    public Lexer createLexer(GrammarModel model) {
        return createLexer(model.getTerminals());
    }

    public Lexer createLexer(List<TerminalSymbol> terminals) {
        LexerSpec spec = new LexerSpec(terminalsWithImplicitWhitespace(terminals));
        LexerOptions options = LexerOptions.DEFAULT.withRegexpCaseSensitive(regexpCaseSensitive);
        return Lexers.create(engine, spec, options);
    }

    public LexerConfig withAdditionalTerminals(Class<?>... extraTerminalTypes) {
        return new LexerConfig(
            combine(terminalTypes, extraTerminalTypes),
            skipTerminalTypes,
            engine,
            regexpCaseSensitive
        );
    }

    private List<TerminalSymbol> terminalsWithImplicitWhitespace(List<TerminalSymbol> terminals) {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>(terminals);
        for (int i = 0; i < terminalTypes.length; i++) {
            addIfAbsent(result, GrammarModelBuilder.terminalSymbol(terminalTypes[i]));
        }
        for (int i = 0; i < skipTerminalTypes.length; i++) {
            addOrMarkSkipped(result, GrammarModelBuilder.terminalSymbol(skipTerminalTypes[i]));
        }
        addOrMarkSkipped(result, new TerminalSymbol(ImplicitWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return result;
    }

    private void addIfAbsent(List<TerminalSymbol> terminals, TerminalSymbol terminal) {
        for (TerminalSymbol existing : terminals) {
            if (existing.getType().equals(terminal.getType())) {
                return;
            }
        }
        terminals.add(terminal);
    }

    private void addOrMarkSkipped(List<TerminalSymbol> terminals, TerminalSymbol terminal) {
        for (int i = 0; i < terminals.size(); i++) {
            TerminalSymbol existing = terminals.get(i);
            if (existing.getType().equals(terminal.getType())) {
                terminals.set(i, existing.withSkip(true));
                return;
            }
        }
        terminals.add(terminal.withSkip(true));
    }

    private Class<?>[] combine(Class<?>[] first, Class<?>[] second) {
        Set<Class<?>> result = new LinkedHashSet<Class<?>>();
        for (int i = 0; i < first.length; i++) {
            result.add(first[i]);
        }
        for (int i = 0; i < second.length; i++) {
            result.add(second[i]);
        }
        return result.toArray(new Class<?>[result.size()]);
    }

    private static final class ImplicitWhitespace {
    }
}
