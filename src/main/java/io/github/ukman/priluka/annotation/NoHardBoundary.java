package io.github.ukman.priluka.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects matches that cross a hard lexical boundary.
 *
 * <p>Hard boundaries are currently newlines, carriage returns, tabs, and
 * form-feeds. This is useful for extraction grammars where a match should not
 * silently span rows, paragraphs, or OCR line breaks.</p>
 *
 * <p>When applied to a root grammar class, the whole {@code find}/{@code findAll}
 * result is rejected if the matched span crosses a hard boundary. When applied
 * to a constructor parameter, every token consumed by that production part must
 * continue without a hard boundary before it.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface NoHardBoundary {
}
