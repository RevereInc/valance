package dev.revere.valance.module.annotation;

import dev.revere.valance.module.Category;

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
public @interface ModuleInfo {
    String name();
    String description() default "";
    Category category();
    boolean defaultEnabled() default false;
}
