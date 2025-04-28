package dev.revere.valance.core.annotation;

import dev.revere.valance.core.lifecycle.IService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
    /**
     * Defines the priority for initialization and shutdown sequence.
     * Lower numbers = initialize earlier, shutdown later.
     * Higher numbers = initialize later, shutdown earlier.
     * Services with the same priority have undefined relative order (unless dependencies force it).
     */
    int priority() default 0;

    /**
     * Specifies the primary interface this service implementation provides.
     * Used by the ClientContext for registration and lookup.
     * MUST be an interface that extends IService.
     * @return The primary service interface class.
     */
    Class<? extends IService> provides();
}