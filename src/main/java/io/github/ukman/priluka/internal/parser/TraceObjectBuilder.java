package io.github.ukman.priluka.internal.parser;

import io.github.ukman.priluka.ParseException;
import io.github.ukman.priluka.ParseTrace;
import io.github.ukman.priluka.ParseTraceEvent;
import io.github.ukman.priluka.Token;
import io.github.ukman.priluka.grammar.Production;
import io.github.ukman.priluka.grammar.ProductionPart;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class TraceObjectBuilder {
    public <S> S build(Class<S> start, ParseTrace trace) {
        Deque<BuildContext> contexts = new ArrayDeque<BuildContext>();
        RootContext root = new RootContext();
        contexts.push(root);

        for (ParseTraceEvent event : trace.getEvents()) {
            switch (event.getKind()) {
                case BEGIN_PRODUCTION:
                    contexts.push(new ProductionContext(event.getProduction()));
                    break;
                case END_PRODUCTION:
                    endProduction(contexts, event.getProduction());
                    break;
                case CONSUME_TERMINAL:
                    contexts.peek().addValue(terminalValue(event));
                    break;
                case BEGIN_REPEAT:
                    contexts.push(new RepeatContext(ownerProduction(contexts), event.getSymbolName()));
                    break;
                case APPEND_REPEAT_ELEMENT:
                    repeatContext(contexts).appendElement();
                    break;
                case END_REPEAT:
                    endRepeat(contexts, event);
                    break;
                case BEGIN_OPTIONAL:
                    contexts.push(new OptionalContext(ownerProduction(contexts), event.getSymbolName()));
                    break;
                case END_OPTIONAL:
                    endOptional(contexts, event);
                    break;
                default:
                    throw new ParseException("Unsupported trace event: " + event);
            }
        }

        if (contexts.size() != 1 || root.value == null) {
            throw new ParseException("Trace did not produce a single root object");
        }
        return start.cast(root.value);
    }

    private void endProduction(Deque<BuildContext> contexts, Production expected) {
        BuildContext context = contexts.pop();
        if (!(context instanceof ProductionContext)) {
            throw new ParseException("Trace endProduction without matching beginProduction");
        }
        ProductionContext productionContext = (ProductionContext) context;
        if (!productionContext.production.equals(expected)) {
            throw new ParseException("Trace production mismatch: " + expected.toBnf());
        }
        contexts.peek().addValue(instantiateProduction(productionContext.production, productionContext.values));
    }

    private void endRepeat(Deque<BuildContext> contexts, ParseTraceEvent event) {
        BuildContext context = contexts.pop();
        if (!(context instanceof RepeatContext)) {
            throw new ParseException("Trace endRepeat without matching beginRepeat");
        }
        RepeatContext repeat = (RepeatContext) context;
        if (!repeat.part.getSymbolName().equals(event.getSymbolName())) {
            throw new ParseException("Trace repeat mismatch: " + event);
        }
        if (event.getCount() != null && event.getCount().intValue() != repeat.items.size()) {
            throw new ParseException("Trace repeat count mismatch: " + event);
        }
        contexts.peek().addValue(repeatedValue(repeat.part.getSymbolType(), repeat.targetType, repeat.items));
    }

    private void endOptional(Deque<BuildContext> contexts, ParseTraceEvent event) {
        BuildContext context = contexts.pop();
        if (!(context instanceof OptionalContext)) {
            throw new ParseException("Trace endOptional without matching beginOptional");
        }
        OptionalContext optional = (OptionalContext) context;
        if (!optional.part.getSymbolName().equals(event.getSymbolName())) {
            throw new ParseException("Trace optional mismatch: " + event);
        }
        boolean present = event.getPresent() != null && event.getPresent().booleanValue();
        Object value = optional.values.isEmpty() ? null : optional.values.get(optional.values.size() - 1);
        contexts.peek().addValue(optionalValue(optional.targetType, value, present));
    }

    private ProductionContext ownerProduction(Deque<BuildContext> contexts) {
        BuildContext context = contexts.peek();
        if (!(context instanceof ProductionContext)) {
            throw new ParseException("Trace variable part outside production");
        }
        return (ProductionContext) context;
    }

    private RepeatContext repeatContext(Deque<BuildContext> contexts) {
        BuildContext context = contexts.peek();
        if (!(context instanceof RepeatContext)) {
            throw new ParseException("Trace appendRepeatElement outside repeat");
        }
        return (RepeatContext) context;
    }

    private Object instantiateProduction(Production production, List<Object> values) {
        Constructor<?> constructor = production.getConstructor();
        if (constructor == null) {
            if (values.size() != 1) {
                throw new ParseException("Interface production must produce exactly one value: " + production.toBnf());
            }
            return values.get(0);
        }
        return instantiate(constructor, values.toArray());
    }

    private Object terminalValue(ParseTraceEvent event) {
        Class<?> type = event.getTerminalType();
        String text = event.getText();
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return Integer.valueOf(text);
        }
        if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return Double.valueOf(text);
        }
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return Boolean.valueOf(text);
        }
        if (type.isEnum()) {
            return enumValue(type, text);
        }
        return instantiateTerminal(type, event);
    }

    private Object enumValue(Class<?> type, String text) {
        Object[] constants = type.getEnumConstants();
        for (Object constant : constants) {
            Enum<?> enumConstant = (Enum<?>) constant;
            if (enumConstant.name().equalsIgnoreCase(text)) {
                return enumConstant;
            }
        }
        throw new ParseException("Unknown enum terminal value " + text + " for " + type.getName());
    }

    private Object instantiateTerminal(Class<?> type, ParseTraceEvent event) {
        Constructor<?> textConstructor = findConstructor(type, String.class);
        if (textConstructor != null) {
            return instantiate(textConstructor, new Object[] {event.getText()});
        }

        Constructor<?> emptyConstructor = findConstructor(type);
        if (emptyConstructor == null) {
            throw new ParseException("Terminal has no supported constructor: " + type.getName());
        }
        Object value = instantiate(emptyConstructor, new Object[0]);
        if (value instanceof Token) {
            fillToken((Token) value, event);
        }
        return value;
    }

    private void fillToken(Token token, ParseTraceEvent event) {
        token.setStart(event.getStart());
        token.setLen(event.getLen());
        token.setText(event.getText());
    }

    private Object optionalValue(Class<?> targetType, Object value, boolean present) {
        if (Optional.class.equals(targetType)) {
            return present ? Optional.of(value) : Optional.empty();
        }
        return present ? value : null;
    }

    private Object repeatedValue(Class<?> componentType, Class<?> targetType, List<Object> values) {
        if (targetType.isArray()) {
            return toArray(componentType, values);
        }
        if (Collection.class.isAssignableFrom(targetType)) {
            return toCollection(targetType, values);
        }
        return toArray(componentType, values);
    }

    private Object toArray(Class<?> componentType, List<Object> values) {
        Object array = Array.newInstance(componentType, values.size());
        for (int i = 0; i < values.size(); i++) {
            Array.set(array, i, values.get(i));
        }
        return array;
    }

    private Object toCollection(Class<?> targetType, List<Object> values) {
        Collection<Object> collection;
        if (targetType.isInterface()) {
            collection = new ArrayList<Object>();
        } else {
            collection = instantiateCollection(targetType);
        }
        collection.addAll(values);
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> instantiateCollection(Class<?> targetType) {
        Constructor<?> constructor = findConstructor(targetType);
        if (constructor == null) {
            throw new ParseException("Collection parameter has no empty constructor: " + targetType.getName());
        }
        Object value = instantiate(constructor, new Object[0]);
        if (!(value instanceof Collection)) {
            throw new ParseException("Collection constructor did not produce a Collection: " + targetType.getName());
        }
        return (Collection<Object>) value;
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

    private interface BuildContext {
        void addValue(Object value);
    }

    private static final class RootContext implements BuildContext {
        private Object value;

        @Override
        public void addValue(Object value) {
            if (this.value != null) {
                throw new ParseException("Trace produced more than one root object");
            }
            this.value = value;
        }
    }

    private static final class ProductionContext implements BuildContext {
        private final Production production;
        private final List<Object> values = new ArrayList<Object>();

        private ProductionContext(Production production) {
            this.production = production;
        }

        @Override
        public void addValue(Object value) {
            values.add(value);
        }

        private ProductionPart nextPart(String symbolName) {
            if (values.size() >= production.getParts().size()) {
                throw new ParseException("Trace has too many values for production: " + production.toBnf());
            }
            ProductionPart part = production.getParts().get(values.size());
            if (!part.getSymbolName().equals(symbolName)) {
                throw new ParseException("Trace symbol mismatch: expected " + part.getSymbolName() + ", got " + symbolName);
            }
            return part;
        }

        private Class<?> nextParameterType() {
            Constructor<?> constructor = production.getConstructor();
            if (constructor == null) {
                return Object.class;
            }
            return constructor.getParameterTypes()[values.size()];
        }
    }

    private static final class RepeatContext implements BuildContext {
        private final ProductionPart part;
        private final Class<?> targetType;
        private final List<Object> scratch = new ArrayList<Object>();
        private final List<Object> items = new ArrayList<Object>();

        private RepeatContext(ProductionContext owner, String symbolName) {
            this.part = owner.nextPart(symbolName);
            this.targetType = owner.nextParameterType();
        }

        @Override
        public void addValue(Object value) {
            scratch.add(value);
        }

        private void appendElement() {
            if (scratch.isEmpty()) {
                throw new ParseException("Trace repeat element has no value: " + part.getSymbolName());
            }
            items.add(scratch.get(scratch.size() - 1));
            scratch.clear();
        }
    }

    private static final class OptionalContext implements BuildContext {
        private final ProductionPart part;
        private final Class<?> targetType;
        private final List<Object> values = new ArrayList<Object>();

        private OptionalContext(ProductionContext owner, String symbolName) {
            this.part = owner.nextPart(symbolName);
            this.targetType = owner.nextParameterType();
        }

        @Override
        public void addValue(Object value) {
            values.add(value);
        }
    }
}
