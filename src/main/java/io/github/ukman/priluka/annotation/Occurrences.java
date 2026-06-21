package io.github.ukman.priluka.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines explicit bounds for an array or collection grammar repetition.
 *
 * <p>A negative {@code max} means unbounded repetition. Finite bounded repetitions
 * are matched lazily by the automata compiler: once {@code min} is satisfied,
 * the generated automaton prefers leaving the repetition before consuming more
 * elements.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
public @interface Occurrences {
    int min() default 0;

    int max() default -1;
}
