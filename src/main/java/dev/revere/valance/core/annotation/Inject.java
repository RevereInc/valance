package dev.revere.valance.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 * <p>
 * Explicitly marks a constructor to be used by the ClientContext for dependency injection.
 * If absent, the context will attempt to use a suitable public constructor.
 * Recommended for clarity when multiple constructors exist.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Inject {}