package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.ParseException;
import io.github.ukman.priluka.Token;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;
import io.github.ukman.priluka.internal.lexer.Lexeme;
import io.github.ukman.priluka.internal.lexer.Lexer;
import io.github.ukman.priluka.internal.lexer.LexerOptions;
import io.github.ukman.priluka.internal.lexer.LexerSpec;
import io.github.ukman.priluka.internal.lexer.Lexers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReflectiveParser {
    private final GrammarModel model;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Map<Class<?>, TerminalSymbol> terminals = new LinkedHashMap<Class<?>, TerminalSymbol>();

    public ReflectiveParser(GrammarModel model) {
        this.model = model;
        for (NonterminalSymbol nonterminal : model.getNonterminals()) {
            nonterminals.put(nonterminal.getType(), nonterminal);
        }
        for (TerminalSymbol terminal : model.getTerminals()) {
            terminals.put(terminal.getType(), terminal);
        }
    }

    public <S> S parse(Class<S> start, String input) {
        Lexer lexer = Lexers.defaultLexer(new LexerSpec(terminalsWithImplicitWhitespace()), LexerOptions.DEFAULT);
        LexemeStream stream = new LexemeStream(lexer.tokenize(input));
        Object result = parseNonterminal(start, stream);
        if (result == null) {
            throw new ParseException("Input does not match start symbol: " + start.getName());
        }
        if (!stream.isEnd()) {
            Lexeme unexpected = stream.peek();
            throw new ParseException("Unexpected token at offset " + unexpected.getStart() + ": " + unexpected.getText());
        }
        return start.cast(result);
    }

    private Object parseNonterminal(Class<?> type, LexemeStream stream) {
        NonterminalSymbol nonterminal = nonterminals.get(type);
        if (nonterminal == null) {
            return null;
        }

        for (Production production : nonterminal.getProductions()) {
            int mark = stream.mark();
            Object result = parseProduction(production, stream);
            if (result != null) {
                return result;
            }
            stream.reset(mark);
        }
        return null;
    }

    private Object parseProduction(Production production, LexemeStream stream) {
        List<Object> values = new ArrayList<Object>();
        for (ProductionPart part : production.getParts()) {
            if (part.getQuantifier() != ProductionPart.Quantifier.ONE) {
                throw new ParseException("Parser v1 supports only single production parts: " + part.toBnf());
            }
            Object value = parseSymbol(part.getSymbolType(), stream);
            if (value == null) {
                return null;
            }
            values.add(value);
        }

        Constructor<?> constructor = production.getConstructor();
        if (constructor == null) {
            if (values.size() != 1) {
                throw new ParseException("Interface production must produce exactly one value: " + production.toBnf());
            }
            return values.get(0);
        }
        return instantiate(constructor, values.toArray());
    }

    private Object parseSymbol(Class<?> type, LexemeStream stream) {
        if (terminals.containsKey(type)) {
            return parseTerminal(type, stream);
        }
        return parseNonterminal(type, stream);
    }

    private Object parseTerminal(Class<?> type, LexemeStream stream) {
        Lexeme lexeme = stream.peek();
        if (lexeme == null || !lexeme.hasTerminal(type)) {
            return null;
        }
        stream.consume();
        return terminalValue(type, lexeme);
    }

    private Object terminalValue(Class<?> type, Lexeme lexeme) {
        String text = lexeme.getText();
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return Integer.valueOf(text);
        }
        if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return Double.valueOf(text);
        }
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return Boolean.valueOf(text);
        }
        return instantiateTerminal(type, lexeme);
    }

    private Object instantiateTerminal(Class<?> type, Lexeme lexeme) {
        try {
            Constructor<?> textConstructor = findConstructor(type, String.class);
            if (textConstructor != null) {
                return instantiate(textConstructor, new Object[] {lexeme.getText()});
            }

            Constructor<?> emptyConstructor = findConstructor(type);
            if (emptyConstructor == null) {
                throw new ParseException("Terminal has no supported constructor: " + type.getName());
            }
            Object value = instantiate(emptyConstructor, new Object[0]);
            if (value instanceof Token) {
                fillToken((Token) value, lexeme);
            }
            return value;
        } catch (SecurityException e) {
            throw new ParseException("Cannot instantiate terminal: " + type.getName(), e);
        }
    }

    private void fillToken(Token token, Lexeme lexeme) {
        token.setStart(lexeme.getStart());
        token.setLen(lexeme.getLen());
        token.setText(lexeme.getText());
    }

    private Object instantiate(Constructor<?> constructor, Object[] args) {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InstantiationException e) {
            throw new ParseException("Cannot instantiate " + constructor.getDeclaringClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new ParseException("Cannot access constructor " + constructor, e);
        } catch (InvocationTargetException e) {
            throw new ParseException("Constructor failed: " + constructor, e.getCause());
        }
    }

    private Constructor<?> findConstructor(Class<?> type, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private List<TerminalSymbol> terminalsWithImplicitWhitespace() {
        List<TerminalSymbol> result = new ArrayList<TerminalSymbol>(model.getTerminals());
        result.add(new TerminalSymbol(ImplicitWhitespace.class, TerminalSymbol.Kind.REGEXP, "\\s+", true, -1000));
        return result;
    }

    private static final class ImplicitWhitespace {
    }
}
