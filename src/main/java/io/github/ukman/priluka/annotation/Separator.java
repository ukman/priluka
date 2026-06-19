package io.github.ukman.priluka.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the terminal that separates elements of an array or collection part.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
public @interface Separator {
    Class<?> value();

    boolean trailing() default false;
}
