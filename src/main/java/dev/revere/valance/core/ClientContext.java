package dev.revere.valance.core;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ClientInitializationException;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.core.lifecycle.IService;
import dev.revere.valance.util.ReflectionUtil;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class ClientContext {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":Context] ";

    // --- State ---
    // Registry: Service Interface Class -> Service Instance
    private final Map<Class<? extends IService>, IService> serviceInstances = new ConcurrentHashMap<>();

    private enum ServiceState {PENDING, CONSTRUCTING, CONSTRUCTED, SETUP, INITIALIZING, INITIALIZED, SHUTTING_DOWN, SHUTDOWN, FAILED}

    private final Map<Class<? extends IService>, ServiceState> serviceStates = new ConcurrentHashMap<>();

    /**
     * Initializes the context by discovering, instantiating, settings up, and initializing services.
     *
     * @throws ClientInitializationException if initialization fails catastrophically.
     */
    public void initialize() throws ClientInitializationException {
        long startTime = System.currentTimeMillis();
        System.out.println(LOG_PREFIX + "--- ClientContext Initialization Start ---");

        List<Class<? extends IService>> serviceImplClasses = null;
        List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> sortedServices = null;

        try {
            // --- 1. Service Discovery ---
            serviceImplClasses = discoverServiceImplementations();
            if (serviceImplClasses.isEmpty()) {
                System.out.println(LOG_PREFIX + "[WARN] No service implementations found.");
                return;
            }

            // --- 2. Create Registration Map & Sort ---
            Map<Class<? extends IService>, Class<? extends IService>> serviceRegistry = mapAndValidateServices(serviceImplClasses);
            sortedServices = sortServices(serviceRegistry);
            System.out.println(LOG_PREFIX + "Service Initialization Order: " + sortedServices.stream().map(e -> e.getKey().getSimpleName() + " (" + e.getValue().getSimpleName() + ")").collect(Collectors.joining(" -> ")));

            // --- 3. Instantiate Services (Handle constructor dependencies) ---
            System.out.println(LOG_PREFIX + "--- Phase: Service Instantiation ---");
            for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : sortedServices) {
                getOrCreateServiceInstance(entry.getKey(), entry.getValue());
            }
            System.out.println(LOG_PREFIX + "--- Phase Complete: Service Instantiation ---");


            // --- 4. Setup Services ---
            System.out.println(LOG_PREFIX + "--- Phase: Service Setup ---");
            for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : sortedServices) {
                setupService(entry.getKey());
            }
            System.out.println(LOG_PREFIX + "--- Phase Complete: Service Setup ---");

            // --- 5. Initialize Services ---
            System.out.println(LOG_PREFIX + "--- Phase: Service Initialization ---");
            for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : sortedServices) {
                initializeService(entry.getKey());
            }
            System.out.println(LOG_PREFIX + "--- Phase Complete: Service Initialization ---");

        } catch (Throwable t) {
            System.err.println(LOG_PREFIX + "[FATAL] Unrecoverable error during ClientContext initialization.");
            if (t instanceof ServiceException) {
                System.err.println(LOG_PREFIX + "[FATAL] Reason: " + t.getMessage());
                if (t.getCause() != null) t.getCause().printStackTrace();
                else t.printStackTrace();
            } else {
                t.printStackTrace();
            }

            if (sortedServices != null) {
                sortedServices.forEach(entry -> serviceStates.put(entry.getKey(), ServiceState.FAILED));
            }

            System.err.println(LOG_PREFIX + "[FATAL] Attempting emergency shutdown...");
            shutdownInternal(true);
            throw new ClientInitializationException("ClientContext initialization failed critically.", t);
        }

        long endTime = System.currentTimeMillis();
        System.out.println(LOG_PREFIX + "--- ClientContext Initialization Success (" + (endTime - startTime) + "ms) ---");
    }

    /**
     * Discovers service implementation classes.
     */
    private List<Class<? extends IService>> discoverServiceImplementations() {
        System.out.println(LOG_PREFIX + "Discovering service implementations via reflection...");

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("dev.revere.valance.service.impl"))
                .setScanners(Scanners.TypesAnnotated)
        );

        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(Service.class);
        List<Class<? extends IService>> serviceClasses = new ArrayList<>();

        for (Class<?> cls : annotatedClasses) {
            if (IService.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
                serviceClasses.add((Class<? extends IService>) cls);
            } else {
                System.err.println(LOG_PREFIX + "[WARN] Class " + cls.getName() + " has @Service annotation but does not implement IService or is abstract/interface. Skipping.");
            }
        }

        if (serviceClasses.isEmpty()) {
            System.out.println(LOG_PREFIX + "[WARN] No valid classes annotated with @Service found in " + "dev.revere.valance.service.impl");
        } else {
            System.out.println(LOG_PREFIX + "Discovered " + serviceClasses.size() + " potential service implementations via reflection.");
        }
        return serviceClasses;
    }

    /**
     * Maps service interfaces to implementations and validates @Service annotation.
     */
    private Map<Class<? extends IService>, Class<? extends IService>> mapAndValidateServices(List<Class<? extends IService>> implementations) throws ServiceException {
        Map<Class<? extends IService>, Class<? extends IService>> registry = new HashMap<>();

        for (Class<? extends IService> implClass : implementations) {
            if (implClass.isInterface() || Modifier.isAbstract(implClass.getModifiers())) {
                System.out.println(LOG_PREFIX + "[WARN] Skipping non-concrete service implementation: " + implClass.getName());
                continue;
            }

            Service serviceAnnotation = implClass.getAnnotation(Service.class);
            if (serviceAnnotation == null) {
                System.err.println(LOG_PREFIX + "[WARN] Service implementation " + implClass.getName() + " is missing @Service annotation. Skipping.");
                continue;
            }

            Class<? extends IService> providedInterface = serviceAnnotation.provides();
            if (!providedInterface.isInterface()) {
                throw new ServiceException("@Service annotation on " + implClass.getName() + " specifies a non-interface 'provides' value: " + providedInterface.getName());
            }
            if (!providedInterface.isAssignableFrom(implClass)) {
                throw new ServiceException("Service implementation " + implClass.getName() + " does not implement the interface specified in its @Service annotation: " + providedInterface.getName());
            }
            if (registry.containsKey(providedInterface)) {
                throw new ServiceException("Multiple service implementations provide the same interface '" + providedInterface.getName() + "': [" + registry.get(providedInterface).getName() + ", " + implClass.getName() + "]");
            }

            registry.put(providedInterface, implClass);
            serviceStates.put(providedInterface, ServiceState.PENDING);
        }
        System.out.println(LOG_PREFIX + "Validated and mapped " + registry.size() + " services.");
        return registry;
    }

    /**
     * Sorts services based on priority. todo: ddd dependency graph (topological sort) if we need in future.
     */
    private List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> sortServices(Map<Class<? extends IService>, Class<? extends IService>> serviceRegistry) {
        return serviceRegistry.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getAnnotation(Service.class).priority()))
                .collect(Collectors.toList());
    }

    /**
     * Gets or creates a service instance, handling dependency resolution via constructor injection.
     */
    private <T extends IService> T getOrCreateServiceInstance(Class<T> serviceInterface, Class<? extends IService> serviceImplClass) throws ServiceException {
        IService existingInstance = serviceInstances.get(serviceInterface);
        if (existingInstance != null) {
            @SuppressWarnings("unchecked")
            T castInstance = (T) existingInstance;
            return castInstance;
        }

        ServiceState currentState = serviceStates.get(serviceInterface);
        if (currentState == ServiceState.CONSTRUCTING) {
            throw new ServiceException("Cyclic dependency detected during construction involving: " + serviceInterface.getName());
        }
        if (currentState != ServiceState.PENDING) {
            throw new ServiceException("Cannot create service " + serviceInterface.getName() + " in state: " + currentState);
        }

        serviceStates.put(serviceInterface, ServiceState.CONSTRUCTING);
        System.out.println(LOG_PREFIX + "Constructing service: " + serviceInterface.getSimpleName() + " (" + serviceImplClass.getSimpleName() + ")");

        try {
            // --- Constructor Injection Logic ---
            Constructor<?> constructor = ReflectionUtil.getServiceConstructor(serviceImplClass);
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if (IService.class.isAssignableFrom(paramType) && paramType.isInterface()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends IService> dependencyInterface = (Class<? extends IService>) paramType;
                    Class<? extends IService> dependencyImplClass = findImplClassForInterface(dependencyInterface);
                    args[i] = getOrCreateServiceInstance(dependencyInterface, dependencyImplClass);
                } else {
                    throw new ServiceException("Unsupported constructor dependency type [" + paramType.getName() +
                            "] in service implementation " + serviceImplClass.getName() + " for " + serviceInterface.getName());
                }
            }

            Object newInstanceObj = constructor.newInstance(args);
            if (!serviceInterface.isInstance(newInstanceObj)) {
                throw new ServiceException("Instantiated service " + serviceImplClass.getName() + " is not an instance of expected interface " + serviceInterface.getName());
            }

            @SuppressWarnings("unchecked")
            T newInstance = (T) newInstanceObj;
            serviceInstances.put(serviceInterface, newInstance);
            serviceStates.put(serviceInterface, ServiceState.CONSTRUCTED);
            System.out.println(LOG_PREFIX + "Constructed service: " + serviceInterface.getSimpleName() + " successfully.");
            return newInstance;

        } catch (Exception e) {
            serviceStates.put(serviceInterface, ServiceState.FAILED);
            System.err.println(LOG_PREFIX + "[ERROR] Failed to construct service: " + serviceInterface.getName());
            throw new ServiceException("Construction failed for service " + serviceInterface.getName(), e);
        }
    }

    /**
     * Helper to find the implementation class for a given service interface from the sorted list
     */
    private Class<? extends IService> findImplClassForInterface(Class<? extends IService> serviceInterface) throws ServiceException {
        // This relies on the initial mapping being correct. A better approach might be needed if the map isn't readily available here.
        // We could pass the initial map, or re-scan the @Service annotation if needed.
        // For simplicity now, assume we can look it up based on the instance map's keys' annotations
        for (IService instance : serviceInstances.values()) {
            Service serviceAnnotation = instance.getClass().getAnnotation(Service.class);
            if (serviceAnnotation != null && serviceAnnotation.provides().equals(serviceInterface)) {
                return instance.getClass();
            }
        }
        throw new ServiceException("Could not find implementation class for dependency interface: " + serviceInterface.getName() + ". This indicates an issue in the context's dependency resolution.");
    }


    /**
     * Calls the setup() method on a service instance.
     */
    private void setupService(Class<? extends IService> serviceInterface) throws ServiceException {
        IService instance = serviceInstances.get(serviceInterface);
        ServiceState state = serviceStates.get(serviceInterface);

        if (instance == null || state == ServiceState.FAILED || state == ServiceState.PENDING || state == ServiceState.CONSTRUCTING) {
            throw new ServiceException("Cannot setup service " + serviceInterface.getSimpleName() + " - instance not ready or failed (State: " + state + ")");
        }
        if (state == ServiceState.SETUP || state == ServiceState.INITIALIZING || state == ServiceState.INITIALIZED) {
            return;
        }
        if (state != ServiceState.CONSTRUCTED) {
            throw new ServiceException("Cannot setup service " + serviceInterface.getSimpleName() + " - unexpected state: " + state);
        }

        System.out.println(LOG_PREFIX + "Setting up service: " + serviceInterface.getSimpleName());
        try {
            instance.setup(this);
            serviceStates.put(serviceInterface, ServiceState.SETUP);
            System.out.println(LOG_PREFIX + "Setup service: " + serviceInterface.getSimpleName() + " successfully.");
        } catch (Exception e) {
            serviceStates.put(serviceInterface, ServiceState.FAILED);
            System.err.println(LOG_PREFIX + "[ERROR] Failed to setup service: " + serviceInterface.getSimpleName());
            throw new ServiceException("Setup failed for service " + serviceInterface.getName(), e);
        }
    }


    /**
     * Calls the initialize() method on a service instance.
     */
    private void initializeService(Class<? extends IService> serviceInterface) throws ServiceException {
        IService instance = serviceInstances.get(serviceInterface);
        ServiceState state = serviceStates.get(serviceInterface);

        if (instance == null || state == ServiceState.FAILED || state == ServiceState.PENDING || state == ServiceState.CONSTRUCTING || state == ServiceState.CONSTRUCTED) {
            throw new ServiceException("Cannot initialize service " + serviceInterface.getSimpleName() + " - instance not ready or failed (State: " + state + ")");
        }
        if (state == ServiceState.INITIALIZING || state == ServiceState.INITIALIZED) {
            return;
        }
        if (state != ServiceState.SETUP) {
            throw new ServiceException("Cannot initialize service " + serviceInterface.getSimpleName() + " - must be in SETUP state (Current: " + state + ")");
        }

        serviceStates.put(serviceInterface, ServiceState.INITIALIZING);
        System.out.println(LOG_PREFIX + "Initializing service: " + serviceInterface.getSimpleName());
        try {
            instance.initialize(this);
            serviceStates.put(serviceInterface, ServiceState.INITIALIZED);
            System.out.println(LOG_PREFIX + "Initialized service: " + serviceInterface.getSimpleName() + " successfully.");
        } catch (Exception e) {
            serviceStates.put(serviceInterface, ServiceState.FAILED);
            System.err.println(LOG_PREFIX + "[ERROR] Failed to initialize service: " + serviceInterface.getSimpleName());
            throw new ServiceException("Initialization failed for service " + serviceInterface.getName(), e);
        }
    }

    /**
     * Retrieves an initialized service instance by its interface class.
     *
     * @param serviceInterface The interface class of the service.
     * @param <T>              The type of the service interface.
     * @return An Optional containing the service instance if found and initialized, otherwise empty.
     */
    public <T extends IService> Optional<T> getService(Class<T> serviceInterface) {
        IService instance = serviceInstances.get(serviceInterface);
        ServiceState state = serviceStates.get(serviceInterface);

        if (instance != null && state != ServiceState.PENDING && state != ServiceState.CONSTRUCTING && state != ServiceState.FAILED) {
            @SuppressWarnings("unchecked")
            T castInstance = (T) instance;
            return Optional.of(castInstance);
        }
        return Optional.empty();
    }

    /**
     * Shuts down all managed services gracefully.
     */
    public void shutdown() {
        shutdownInternal(false);
    }

    /**
     * Internal shutdown logic
     */
    private void shutdownInternal(boolean emergency) {
        if (!emergency && serviceStates.values().stream().allMatch(s -> s == ServiceState.SHUTDOWN || s == ServiceState.PENDING || s == ServiceState.FAILED)) {
            System.out.println(LOG_PREFIX + "Shutdown requested, but services appear already shutdown or failed.");
            return;
        }

        System.out.println(LOG_PREFIX + "--- ClientContext Shutdown Start ---");

        List<Map.Entry<Class<? extends IService>, IService>> shutdownOrder = serviceInstances.entrySet().stream()
                .filter(entry -> entry.getKey().isAnnotationPresent(Service.class))
                .sorted((e1, e2) -> Integer.compare(
                        e2.getKey().getAnnotation(Service.class).priority(),
                        e1.getKey().getAnnotation(Service.class).priority()
                ))
                .collect(Collectors.toList());

        System.out.println(LOG_PREFIX + "Service shutdown order: " +
                shutdownOrder.stream().map(e -> e.getKey().getSimpleName()).collect(Collectors.joining(" -> ")));

        for (Map.Entry<Class<? extends IService>, IService> entry : shutdownOrder) {
            Class<? extends IService> iface = entry.getKey();
            IService instance = entry.getValue();
            ServiceState currentState = serviceStates.getOrDefault(iface, ServiceState.FAILED);

            if (currentState != ServiceState.SHUTDOWN && currentState != ServiceState.FAILED && currentState != ServiceState.PENDING && currentState != ServiceState.CONSTRUCTING) {
                System.out.println(LOG_PREFIX + "Shutting down service: " + iface.getSimpleName() + " (State: " + currentState + ")");
                serviceStates.put(iface, ServiceState.SHUTTING_DOWN);
                try {
                    instance.shutdown(this);
                    serviceStates.put(iface, ServiceState.SHUTDOWN);
                    System.out.println(LOG_PREFIX + "Shutdown service: " + iface.getSimpleName() + " successfully.");
                } catch (Throwable t) {
                    serviceStates.put(iface, ServiceState.FAILED);
                    System.err.println(LOG_PREFIX + "[ERROR] Error shutting down service " + iface.getName() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            } else {
                System.out.println(LOG_PREFIX + "Skipping shutdown for service: " + iface.getSimpleName() + " (State: " + currentState + ")");
            }
        }

        serviceInstances.clear();
        serviceStates.clear();
        System.out.println(LOG_PREFIX + "--- ClientContext Shutdown Complete ---");
    }
}
