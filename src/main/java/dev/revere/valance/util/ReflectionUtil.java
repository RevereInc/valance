package dev.revere.valance.util;

import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.exception.ServiceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public final class ReflectionUtil {
    private static final Map<Class<?>, Constructor<?>> SERVICE_CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>> MODULE_CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Method[]> SUBSCRIBER_METHOD_CACHE = new ConcurrentHashMap<>();

    private ReflectionUtil() {
        // Private constructor for utility class
    }

    /**
     * Finds and caches the appropriate constructor for a service class.
     * Prefers constructors marked with @Inject, falls back to the first declared public constructor.
     *
     * @param serviceClass The service class.
     * @return The cached constructor.
     * @throws ServiceException If no suitable constructor is found.
     */
    public static Constructor<?> getServiceConstructor(Class<?> serviceClass) throws ServiceException {
        return SERVICE_CONSTRUCTOR_CACHE.computeIfAbsent(serviceClass, clazz -> {
            Constructor<?> injectableConstructor = null;
            Constructor<?> firstPublicConstructor = null;

            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (constructor.isAnnotationPresent(Inject.class)) {
                    if (injectableConstructor != null) {
                        throw new ServiceException("Multiple constructors annotated with @Inject found in " + clazz.getName());
                    }
                    injectableConstructor = constructor;
                }
                if (firstPublicConstructor == null && java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) {
                    firstPublicConstructor = constructor;
                }
            }

            Constructor<?> chosenConstructor = (injectableConstructor != null) ? injectableConstructor : firstPublicConstructor;

            if (chosenConstructor == null) {
                if (clazz.getDeclaredConstructors().length == 1) {
                    chosenConstructor = clazz.getDeclaredConstructors()[0];
                } else {
                    throw new ServiceException("No suitable constructor found for service " + clazz.getName() + ". Use @Inject or provide a single public constructor.");
                }
            }

            chosenConstructor.setAccessible(true);
            return chosenConstructor;
        });
    }

    /**
     * Finds and caches the appropriate constructor for a module class.
     * Assumes a single constructor or requires @Inject if multiple exist.
     *
     * @param moduleClass The module class.
     * @return The cached constructor.
     * @throws ServiceException If no suitable constructor is found.
     */
    public static Constructor<?> getModuleConstructor(Class<?> moduleClass) throws ServiceException {
        return MODULE_CONSTRUCTOR_CACHE.computeIfAbsent(moduleClass, clazz -> {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Constructor<?> chosenConstructor = Arrays.stream(constructors)
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .findFirst()
                    .orElse(constructors.length == 1 ? constructors[0] : null);

            if (chosenConstructor == null) {
                throw new ServiceException("No suitable constructor found for module " + clazz.getName() + ". Use @Inject or provide a single constructor.");
            }

            chosenConstructor.setAccessible(true);
            return chosenConstructor;
        });
    }

    /**
     * Finds and caches all methods annotated with a specific annotation within a class.
     *
     * @param targetClass     The class to scan.
     * @param annotationClass The annotation to look for.
     * @return An array of cached methods annotated with the specified annotation.
     */
    public static Method[] getAnnotatedMethods(Class<?> targetClass, Class<? extends java.lang.annotation.Annotation> annotationClass) {
        return SUBSCRIBER_METHOD_CACHE.computeIfAbsent(targetClass, clazz ->
                Arrays.stream(clazz.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(annotationClass) && !method.isSynthetic())
                        .peek(method -> method.setAccessible(true))
                        .toArray(Method[]::new)
        );
    }
}
