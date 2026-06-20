package io.github.ukman.priluka.internal;

import io.github.ukman.priluka.GrammarException;
import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Keywords;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Skip;
import io.github.ukman.priluka.annotation.Terminal;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.grammar.NonterminalSymbol;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;
import io.github.ukman.priluka.grammar.TerminalSymbol;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GrammarModelBuilder {
    private final List<Class<?>> universe;
    private final Map<Class<?>, NonterminalSymbol> nonterminals = new LinkedHashMap<Class<?>, NonterminalSymbol>();
    private final Map<Class<?>, TerminalSymbol> terminals = new LinkedHashMap<Class<?>, TerminalSymbol>();
    private final List<Class<?>> visiting = new ArrayList<Class<?>>();

    public GrammarModelBuilder(Class<?>[] universe) {
        this.universe = Arrays.asList(universe.clone());
    }

    public GrammarModel build(Class<?> start) {
        if (isTerminal(start)) {
            addTerminal(start);
            throw new GrammarException("Start symbol must be a nonterminal in the first GrammarModel version: " + start.getName());
        }
        NonterminalSymbol startSymbol = addNonterminal(start);
        return new GrammarModel(
            startSymbol,
            new ArrayList<NonterminalSymbol>(nonterminals.values()),
            new ArrayList<TerminalSymbol>(terminals.values())
        );
    }

    private NonterminalSymbol addNonterminal(Class<?> type) {
        NonterminalSymbol existing = nonterminals.get(type);
        if (existing != null) {
            return existing;
        }

        NonterminalSymbol symbol = new NonterminalSymbol(type);
        nonterminals.put(type, symbol);

        if (visiting.contains(type)) {
            return symbol;
        }

        visiting.add(type);
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            addInterfaceProductions(symbol, type);
        } else {
            addConstructorProductions(symbol, type);
        }
        visiting.remove(type);
        return symbol;
    }

    private void addInterfaceProductions(NonterminalSymbol symbol, Class<?> type) {
        List<Class<?>> implementations = new ArrayList<Class<?>>();
        for (Class<?> candidate : universe) {
            if (!candidate.equals(type) && type.isAssignableFrom(candidate) && !candidate.isInterface()) {
                implementations.add(candidate);
            }
        }
        sortClasses(implementations);
        if (implementations.isEmpty()) {
            throw new GrammarException("No implementations registered for nonterminal: " + type.getName());
        }
        for (Class<?> implementation : implementations) {
            addSymbol(implementation);
            List<ProductionPart> parts = new ArrayList<ProductionPart>();
            parts.add(ProductionPart.one(implementation));
            symbol.addProduction(new Production(symbol, null, implementation, parts));
        }
    }

    private void addConstructorProductions(NonterminalSymbol symbol, Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        Arrays.sort(constructors, new ConstructorComparator());
        for (Constructor<?> constructor : constructors) {
            List<ProductionPart> parts = new ArrayList<ProductionPart>();
            for (Parameter parameter : constructor.getParameters()) {
                parts.add(toPart(parameter));
            }
            symbol.addProduction(new Production(symbol, constructor, type, parts));
        }
    }

    private ProductionPart toPart(Parameter parameter) {
        Separator separator = parameter.getAnnotation(Separator.class);
        boolean oneOrMore = parameter.isAnnotationPresent(OneOrMore.class);
        Class<?> rawType = parameter.getType();

        if (rawType.isArray()) {
            Class<?> componentType = rawType.getComponentType();
            addSymbol(componentType);
            if (separator != null) {
                addSymbol(separator.value());
                return ProductionPart.separated(componentType, separator.value(), oneOrMore, separator.trailing());
            }
            return ProductionPart.repeated(componentType, oneOrMore);
        }

        if (Collection.class.isAssignableFrom(rawType)) {
            Class<?> elementType = collectionElementType(parameter);
            addSymbol(elementType);
            if (separator != null) {
                addSymbol(separator.value());
                return ProductionPart.separated(elementType, separator.value(), oneOrMore, separator.trailing());
            }
            return ProductionPart.repeated(elementType, oneOrMore);
        }

        if (java.util.Optional.class.equals(rawType)) {
            Class<?> elementType = optionalElementType(parameter);
            addSymbol(elementType);
            return ProductionPart.optional(elementType);
        }

        if (separator != null) {
            throw new GrammarException("@Separator can only be used on array or collection parameters: " + parameter);
        }

        addSymbol(rawType);
        return ProductionPart.one(rawType);
    }

    private void addSymbol(Class<?> type) {
        if (isTerminal(type)) {
            addTerminal(type);
        } else {
            addNonterminal(type);
        }
    }

    private void addTerminal(Class<?> type) {
        if (terminals.containsKey(type)) {
            return;
        }

        TerminalSymbol.Kind kind;
        String pattern;
        int priority;
        boolean caseSensitive;
        if (isBuiltIn(type)) {
            kind = TerminalSymbol.Kind.BUILT_IN;
            pattern = builtInPattern(type);
            priority = 0;
            caseSensitive = true;
        } else if (type.isAnnotationPresent(Terminal.class)) {
            Terminal terminal = type.getAnnotation(Terminal.class);
            kind = TerminalSymbol.Kind.REGEXP;
            pattern = terminalPattern(terminal, type);
            priority = terminal.priority();
            caseSensitive = true;
        } else if (type.isAnnotationPresent(Keyword.class)) {
            Keyword keyword = type.getAnnotation(Keyword.class);
            kind = TerminalSymbol.Kind.KEYWORD;
            pattern = keywordText(keyword, type);
            priority = keyword.priority();
            caseSensitive = keyword.caseSensitive();
        } else if (type.isEnum() && type.isAnnotationPresent(Keywords.class)) {
            Keywords keywords = type.getAnnotation(Keywords.class);
            kind = TerminalSymbol.Kind.KEYWORD;
            pattern = enumKeywordText(type);
            priority = keywords.priority();
            caseSensitive = keywords.caseSensitive();
        } else {
            throw new GrammarException("Not a terminal: " + type.getName());
        }
        terminals.put(type, new TerminalSymbol(type, kind, pattern, type.isAnnotationPresent(Skip.class), priority, caseSensitive));
    }

    private boolean isTerminal(Class<?> type) {
        return isBuiltIn(type)
            || type.isAnnotationPresent(Terminal.class)
            || type.isAnnotationPresent(Keyword.class)
            || (type.isEnum() && type.isAnnotationPresent(Keywords.class));
    }

    private boolean isBuiltIn(Class<?> type) {
        return Integer.class.equals(type)
            || Integer.TYPE.equals(type)
            || Double.class.equals(type)
            || Double.TYPE.equals(type)
            || Boolean.class.equals(type)
            || Boolean.TYPE.equals(type);
    }

    private String builtInPattern(Class<?> type) {
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return "[0-9]+";
        }
        if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return "([0-9]+\\.[0-9]*|[0-9]*\\.[0-9]+|[0-9]+)";
        }
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return "true|false";
        }
        throw new GrammarException("Unsupported built-in terminal: " + type.getName());
    }

    private String terminalPattern(Terminal terminal, Class<?> type) {
        boolean hasRegexp = !terminal.regexp().isEmpty();
        boolean hasRegex = !terminal.regex().isEmpty();
        if (hasRegexp && hasRegex && !terminal.regexp().equals(terminal.regex())) {
            throw new GrammarException("@Terminal defines both regexp and regex with different values: " + type.getName());
        }
        if (hasRegexp) {
            return terminal.regexp();
        }
        if (hasRegex) {
            return terminal.regex();
        }
        throw new GrammarException("@Terminal must define regexp or regex: " + type.getName());
    }

    private String keywordText(Keyword keyword, Class<?> type) {
        if (!keyword.value().isEmpty()) {
            return keyword.value();
        }
        return type.getSimpleName().toLowerCase();
    }

    private String enumKeywordText(Class<?> enumType) {
        Object[] constants = enumType.getEnumConstants();
        List<String> names = new ArrayList<String>();
        for (Object constant : constants) {
            names.add(((Enum<?>) constant).name().toLowerCase());
        }
        return names.toString();
    }

    private Class<?> collectionElementType(Parameter parameter) {
        Type type = parameter.getParameterizedType();
        if (!(type instanceof ParameterizedType)) {
            throw new GrammarException("Raw collection parameters are not supported: " + parameter);
        }
        Type argument = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (!(argument instanceof Class<?>)) {
            throw new GrammarException("Collection element type must be a class: " + parameter);
        }
        return (Class<?>) argument;
    }

    private Class<?> optionalElementType(Parameter parameter) {
        Type type = parameter.getParameterizedType();
        if (!(type instanceof ParameterizedType)) {
            throw new GrammarException("Raw Optional parameters are not supported: " + parameter);
        }
        Type argument = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (!(argument instanceof Class<?>)) {
            throw new GrammarException("Optional element type must be a class: " + parameter);
        }
        return (Class<?>) argument;
    }

    private void sortClasses(List<Class<?>> classes) {
        classes.sort(new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> left, Class<?> right) {
                return left.getName().compareTo(right.getName());
            }
        });
    }

    private static final class ConstructorComparator implements Comparator<Constructor<?>> {
        @Override
        public int compare(Constructor<?> left, Constructor<?> right) {
            return signature(left).compareTo(signature(right));
        }

        private String signature(Constructor<?> constructor) {
            StringBuilder builder = new StringBuilder();
            builder.append(constructor.getParameterCount()).append(':');
            for (Class<?> parameterType : constructor.getParameterTypes()) {
                builder.append(parameterType.getName()).append(';');
            }
            return builder.toString();
        }
    }
}
