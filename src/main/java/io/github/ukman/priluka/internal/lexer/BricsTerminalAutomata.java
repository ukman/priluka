package io.github.ukman.priluka.internal.lexer;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import io.github.ukman.priluka.grammar.TerminalSymbol;

final class BricsTerminalAutomata {
    private BricsTerminalAutomata() {
    }

    static RunAutomaton compile(TerminalSymbol terminal) {
        Automaton automaton;
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            automaton = keyword(terminal.getPattern(), terminal.isCaseSensitive());
        } else {
            automaton = regexp(terminal.getPattern());
        }
        automaton.determinize();
        automaton.minimize();
        return new RunAutomaton(automaton);
    }

    private static Automaton keyword(String text, boolean caseSensitive) {
        if (caseSensitive) {
            return Automaton.makeString(text);
        }

        Automaton result = Automaton.makeEmptyString();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char lower = Character.toLowerCase(c);
            char upper = Character.toUpperCase(c);
            Automaton next = lower == upper
                ? Automaton.makeChar(c)
                : Automaton.makeChar(lower).union(Automaton.makeChar(upper));
            result = result.concatenate(next);
        }
        return result;
    }

    private static Automaton regexp(String pattern) {
        if ("\\s+".equals(pattern)) {
            return Automaton.makeCharSet(" \t\r\n\f").repeat(1);
        }
        if ("\"([^\"\\\\]|\\\\.)*\"".equals(pattern)) {
            return quotedStringWithEscapes();
        }
        if ("\"[^\"]*\"".equals(pattern)) {
            return quotedStringWithoutEscapes();
        }
        return new RegExp(pattern).toAutomaton();
    }

    private static Automaton quotedStringWithEscapes() {
        Automaton quote = Automaton.makeChar('"');
        Automaton backslash = Automaton.makeChar('\\');
        Automaton bodyChar = Automaton.makeAnyChar()
            .minus(Automaton.makeChar('"').union(backslash));
        Automaton escaped = backslash.concatenate(Automaton.makeAnyChar());
        return quote.concatenate(bodyChar.union(escaped).repeat()).concatenate(Automaton.makeChar('"'));
    }

    private static Automaton quotedStringWithoutEscapes() {
        Automaton quote = Automaton.makeChar('"');
        Automaton body = Automaton.makeAnyChar().minus(Automaton.makeChar('"')).repeat();
        return quote.concatenate(body).concatenate(Automaton.makeChar('"'));
    }
}
