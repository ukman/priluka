package io.github.ukman.priluka.internal.nfa;

import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerException;
import io.github.ukman.priluka.internal.lexer.LexerOptions;
import io.github.ukman.priluka.internal.lexer.LexerSpec;
import io.github.ukman.priluka.internal.lexer.Lexers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NfaRecognizer {
    private final NfaGraph graph;
    private final Lexer lexer;
    private final Map<NfaState, List<NfaTransition>> outgoing = new LinkedHashMap<NfaState, List<NfaTransition>>();

    public NfaRecognizer(GrammarModel model) {
        this(new NfaCompiler(model).compile(), lexerFor(model));
    }

    public NfaRecognizer(NfaGraph graph, Lexer lexer) {
        this.graph = graph;
        this.lexer = lexer;
        for (NfaState state : graph.getStates()) {
            outgoing.put(state, new ArrayList<NfaTransition>());
        }
        for (NfaTransition transition : graph.getTransitions()) {
            outgoing.get(transition.getFrom()).add(transition);
        }
    }

    public boolean recognizes(String input) {
        try {
            return recognizes(lexer.tokenize(input));
        } catch (LexerException e) {
            return false;
        }
    }

    public boolean recognizes(List<Lexeme> lexemes) {
        Set<NfaState> active = epsilonClosure(singleton(graph.getStart()));
        for (Lexeme lexeme : lexemes) {
            Set<NfaState> next = new LinkedHashSet<NfaState>();
            for (NfaState state : active) {
                List<NfaTransition> transitions = outgoing.get(state);
                for (NfaTransition transition : transitions) {
                    if (
                        transition.getKind() == NfaTransition.Kind.TERMINAL
                            && lexeme.hasTerminal(transition.getSymbolType())
                    ) {
                        next.add(transition.getTo());
                    }
                }
            }
            if (next.isEmpty()) {
                return false;
            }
            active = epsilonClosure(next);
        }
        return active.contains(graph.getAccept());
    }

    private Set<NfaState> epsilonClosure(Set<NfaState> seed) {
        Set<NfaState> closed = new LinkedHashSet<NfaState>(seed);
        Deque<NfaState> queue = new ArrayDeque<NfaState>(seed);
        while (!queue.isEmpty()) {
            NfaState state = queue.removeFirst();
            List<NfaTransition> transitions = outgoing.get(state);
            for (NfaTransition transition : transitions) {
                if (transition.getKind() == NfaTransition.Kind.TERMINAL) {
                    continue;
                }
                if (closed.add(transition.getTo())) {
                    queue.addLast(transition.getTo());
                }
            }
        }
        return closed;
    }

    private Set<NfaState> singleton(NfaState state) {
        Set<NfaState> result = new LinkedHashSet<NfaState>();
        result.add(state);
        return result;
    }

    private static Lexer lexerFor(GrammarModel model) {
        return Lexers.defaultLexer(new LexerSpec(terminalsWithImplicitWhitespace(model)), LexerOptions.DEFAULT);
    }

    private static List<TerminalSymbol> terminalsWithImplicitWhitespace(GrammarModel model) {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>(model.getTerminals());
        result.add(new TerminalSymbol(ImplicitWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return result;
    }

    private static final class ImplicitWhitespace {
    }
}
