package io.github.ukman.priluka.internal.lexer;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class BricsTerminalAutomata {
    private BricsTerminalAutomata() {
    }

    static RunAutomaton compile(TerminalSymbol terminal) {
        return compile(terminal, LexerOptions.DEFAULT);
    }

    static RunAutomaton compile(TerminalSymbol terminal, LexerOptions options) {
        Automaton automaton;
        if (terminal.getKind() == TerminalSymbol.Kind.KEYWORD) {
            automaton = keyword(terminal.getPattern(), terminal.isCaseSensitive());
        } else {
            automaton = regexp(terminal.getPattern());
            if (!options.isRegexpCaseSensitive()) {
                automaton = automaton.subst(asciiCaseFolding());
            }
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

    private static Map<Character, Set<Character>> asciiCaseFolding() {
        Map<Character, Set<Character>> result = new LinkedHashMap<Character, Set<Character>>();
        for (char c = 'a'; c <= 'z'; c++) {
            char upper = Character.toUpperCase(c);
            Set<Character> both = new LinkedHashSet<Character>();
            both.add(Character.valueOf(c));
            both.add(Character.valueOf(upper));
            result.put(Character.valueOf(c), both);
            result.put(Character.valueOf(upper), both);
        }
        return result;
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
