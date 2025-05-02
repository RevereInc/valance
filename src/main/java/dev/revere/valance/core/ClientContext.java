package dev.revere.valance.core;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ClientInitializationException;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.core.lifecycle.IService;
import dev.revere.valance.util.LoggerUtil;
import dev.revere.valance.util.ReflectionUtil;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public final class ClientContext {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":Context]";

    private static final String SERVICE_IMPL_PACKAGE = "dev.revere.valance.service.impl";

    // --- State ---
    private enum ServiceState {PENDING, CONSTRUCTING, CONSTRUCTED, SETUP, INITIALIZING, INITIALIZED, SHUTTING_DOWN, SHUTDOWN, FAILED}

    // Registry: Service Interface Class -> Service Instance
    private final Map<Class<? extends IService>, IService> serviceInstances = new ConcurrentHashMap<>();

    // State Tracking: Service Interface Class -> Current Lifecycle State
    private final Map<Class<? extends IService>, ServiceState> serviceStates = new ConcurrentHashMap<>();

    // Holds the mapping from Interface -> Implementation Class after discovery
    private Map<Class<? extends IService>, Class<? extends IService>> serviceRegistry = Collections.emptyMap();

    /**
     * Initializes the context by discovering, instantiating, setting up, and initializing services
     * in a defined order.
     *
     * @throws ClientInitializationException if initialization fails catastrophically.
     */
    public void initialize() throws ClientInitializationException {
        long startTimeNs = System.nanoTime();
        LoggerUtil.info(LOG_PREFIX, "--- ClientContext Initialization Start ---");

        List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> sortedServiceEntries = null;

        try {
            // --- 1. Service Discovery ---
            List<Class<? extends IService>> serviceImplClasses = discoverServiceImplementations();
            if (serviceImplClasses.isEmpty()) {
                LoggerUtil.warn(LOG_PREFIX, "No service implementations found in package: " + SERVICE_IMPL_PACKAGE);
                LoggerUtil.info(LOG_PREFIX, "--- ClientContext Initialization Complete (No Services) ---");
                return;
            }

            // --- 2. Create Registration Map & Sort ---
            this.serviceRegistry = mapAndValidateServices(serviceImplClasses);
            sortedServiceEntries = sortServicesByPriority(serviceRegistry);

            LoggerUtil.info(LOG_PREFIX, "Service Initialization Order: " + formatServiceOrder(sortedServiceEntries));

            // --- 3. Instantiate Services (Handle constructor dependencies) ---
            LoggerUtil.info(LOG_PREFIX, "--- Phase: Service Instantiation ---");
            for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : sortedServiceEntries) {
                getOrCreateServiceInstance(entry.getKey(), entry.getValue(), serviceRegistry);
            }
            LoggerUtil.info(LOG_PREFIX, "--- Phase Complete: Service Instantiation ---");

            // --- 4. Setup Services ---
            LoggerUtil.info(LOG_PREFIX, "--- Phase: Service Setup ---");
            for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : sortedServiceEntries) {
                setupService(entry.getKey());
            }
            LoggerUtil.info(LOG_PREFIX, "--- Phase Complete: Service Setup ---");

            // --- 5. Initialize Services ---
            LoggerUtil.info(LOG_PREFIX, "--- Phase: Service Initialization ---");
            for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : sortedServiceEntries) {
                initializeService(entry.getKey());
            }
            LoggerUtil.info(LOG_PREFIX, "--- Phase Complete: Service Initialization ---");
        } catch (Throwable t) {
            LoggerUtil.error(LOG_PREFIX, "Unrecoverable error during ClientContext initialization.", t);

            if (sortedServiceEntries != null) {
                sortedServiceEntries.forEach(entry -> serviceStates.put(entry.getKey(), ServiceState.FAILED));
            } else if (serviceRegistry != null) {
                serviceRegistry.keySet().forEach(iface -> serviceStates.put(iface, ServiceState.FAILED));
            }

            LoggerUtil.error(LOG_PREFIX, "Attempting emergency shutdown...");
            try {
                shutdownInternal(true);
            } catch (Throwable shutdownError) {
                LoggerUtil.error(LOG_PREFIX, "Emergency shutdown failed.", shutdownError);
            }
            throw new ClientInitializationException("ClientContext initialization failed critically.", t);
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs);
        LoggerUtil.info(LOG_PREFIX, "--- ClientContext Initialization Success (" + durationMs + "ms) ---");
    }

    /**
     * Discovers service implementation classes using ClassGraph for better performance.
     */
    private List<Class<? extends IService>> discoverServiceImplementations() {
        LoggerUtil.info(LOG_PREFIX, "Discovering service implementations via ClassGraph in package: " + SERVICE_IMPL_PACKAGE + "...");
        List<Class<? extends IService>> serviceClasses = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                // .verbose()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(SERVICE_IMPL_PACKAGE)
                .scan()) {

            List<ClassInfo> serviceClassInfoList = scanResult
                    .getClassesImplementing(IService.class.getName())
                    .filter(classInfo ->
                            classInfo.hasAnnotation(Service.class.getName()) &&
                                    !classInfo.isInterface() &&
                                    !classInfo.isAbstract()
                    );

            LoggerUtil.info(LOG_PREFIX, "Found " + serviceClassInfoList.size() + " potential concrete service classes with @Service annotation.");

            for (ClassInfo classInfo : serviceClassInfoList) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends IService> cls = (Class<? extends IService>) classInfo.loadClass();
                    serviceClasses.add(cls);
                } catch (IllegalArgumentException | SecurityException | LinkageError e) {
                    LoggerUtil.warn(LOG_PREFIX, "Could not load class: " + classInfo.getName() + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
                } catch (Exception e) {
                    LoggerUtil.warn(LOG_PREFIX, "Unexpected error loading class: " + classInfo.getName() + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Error during ClassGraph scanning.", e);
            return Collections.emptyList();
        }

        if (serviceClasses.isEmpty()) {
            LoggerUtil.warn(LOG_PREFIX, "No valid concrete classes annotated with @Service and implementing IService found in " + SERVICE_IMPL_PACKAGE);
        } else {
            LoggerUtil.info(LOG_PREFIX, "Successfully loaded " + serviceClasses.size() + " service implementations.");
        }
        return serviceClasses;
    }

    /**
     * Maps and validates service implementations against their interfaces.
     *
     * @param implementations List of service implementation classes.
     * @return A map of service interfaces to their corresponding implementation classes.
     * @throws ServiceException if validation fails.
     */
    private Map<Class<? extends IService>, Class<? extends IService>> mapAndValidateServices(List<Class<? extends IService>> implementations) throws ServiceException {

        Map<Class<? extends IService>, Class<? extends IService>> registry = new HashMap<>();

        for (Class<? extends IService> implClass : implementations) {
            Service serviceAnnotation = implClass.getAnnotation(Service.class);
            if (serviceAnnotation == null) {
                LoggerUtil.warn(LOG_PREFIX, "Implementation " + implClass.getName() + " missing @Service annotation despite passing discovery filter. Skipping.");
                continue;
            }

            Class<? extends IService> providedInterface = serviceAnnotation.provides();

            // Validation
            if (!providedInterface.isInterface()) {
                throw new ServiceException("@Service annotation on " + implClass.getName() +
                        " specifies a non-interface 'provides' value: " + providedInterface.getName());
            }
            if (!providedInterface.isAssignableFrom(implClass)) {
                throw new ServiceException("Service implementation " + implClass.getName() +
                        " does not implement the interface specified in its @Service annotation: " + providedInterface.getName());
            }
            if (registry.containsKey(providedInterface)) {
                throw new ServiceException("Multiple service implementations provide the same interface '" +
                        providedInterface.getName() + "': [" + registry.get(providedInterface).getName() + ", " + implClass.getName() + "]");
            }

            registry.put(providedInterface, implClass);
            serviceStates.put(providedInterface, ServiceState.PENDING);
        }
        LoggerUtil.info(LOG_PREFIX, "Validated and mapped " + registry.size() + " services.");
        return Collections.unmodifiableMap(registry);
    }


    /**
     * Sorts services by priority using the @Service annotation.
     *
     * @param serviceMap The map of service interfaces to their implementation classes.
     * @return A sorted list of service entries.
     */
    private List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> sortServicesByPriority(
            Map<Class<? extends IService>, Class<? extends IService>> serviceMap) {

        return serviceMap.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> getServicePriority(entry.getValue())))
                .collect(Collectors.toUnmodifiableList());
    }

    // TODO: implement topological sort for robust dependency handling.
    private List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> sortServicesTopologically(
            Map<Class<? extends IService>, Class<? extends IService>> serviceMap) throws ServiceException {
        LoggerUtil.warn(LOG_PREFIX, "Topological sort based on constructor dependencies is not yet implemented. Falling back to priority sort.");
        return sortServicesByPriority(serviceMap);
    }

    /**
     * Retrieves the priority of a service implementation class based on the @Service annotation.
     *
     * @param implClass The implementation class of the service. Must not be null.
     * @return The priority value from the @Service annotation, or 0 if not specified.
     */
    private int getServicePriority(Class<? extends IService> implClass) {
        Service serviceAnnotation = implClass.getAnnotation(Service.class);
        return (serviceAnnotation != null) ? serviceAnnotation.priority() : 0;
    }

    /**
     * Creates or retrieves a service instance, handling constructor dependencies.
     *
     * @param serviceInterface The interface class of the service. Must not be null.
     * @param serviceImplClass The implementation class of the service. Must not be null.
     * @param currentRegistry  The current registry of services.
     * @param <T>              The type of the service interface.
     * @return The created or retrieved service instance.
     * @throws ServiceException if an error occurs during instantiation or setup.
     */
    private <T extends IService> T getOrCreateServiceInstance(
            Class<T> serviceInterface,
            Class<? extends IService> serviceImplClass,
            Map<Class<? extends IService>, Class<? extends IService>> currentRegistry) throws ServiceException {

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
            throw new ServiceException("Cannot create service " + serviceInterface.getName() +
                    " in unexpected state: " + currentState + ". Service might have failed previously or there's a logic error.");
        }

        serviceStates.put(serviceInterface, ServiceState.CONSTRUCTING);
        LoggerUtil.info(LOG_PREFIX, "Constructing service: " + serviceInterface.getSimpleName() + " (" + serviceImplClass.getSimpleName() + ")");

        try {
            Constructor<?> constructor = ReflectionUtil.getServiceConstructor(serviceImplClass);
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if (IService.class.isAssignableFrom(paramType) && paramType.isInterface()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends IService> dependencyInterface = (Class<? extends IService>) paramType;

                    Class<? extends IService> dependencyImplClass = currentRegistry.get(dependencyInterface);
                    if (dependencyImplClass == null) {
                        throw new ServiceException("Could not find registered implementation for dependency interface: " +
                                dependencyInterface.getName() + ", required by " + serviceImplClass.getName());
                    }
                    args[i] = getOrCreateServiceInstance(dependencyInterface, dependencyImplClass, currentRegistry);
                } else {
                    throw new ServiceException("Unsupported constructor dependency type [" + paramType.getName() +
                            "] in service implementation " + serviceImplClass.getName() + ". Only IService interfaces allowed.");
                }
            }

            Object newInstanceObj = constructor.newInstance(args);

            if (!serviceInterface.isInstance(newInstanceObj)) {
                throw new ServiceException("Instantiated service " + serviceImplClass.getName() +
                        " is not an instance of expected interface " + serviceInterface.getName());
            }

            @SuppressWarnings("unchecked")
            T newInstance = (T) newInstanceObj;

            serviceInstances.put(serviceInterface, newInstance);
            serviceStates.put(serviceInterface, ServiceState.CONSTRUCTED);
            LoggerUtil.info(LOG_PREFIX, "Constructed service: " + serviceInterface.getSimpleName() + " successfully.");
            return newInstance;

        } catch (Exception e) {
            serviceStates.put(serviceInterface, ServiceState.FAILED);
            LoggerUtil.error(LOG_PREFIX, "Failed to construct service: " + serviceInterface.getName(), e);
            throw new ServiceException("Construction failed for service " + serviceInterface.getName(), e);
        }
    }

    /**
     * Calls the setup() method on a service instance.
     *
     * @param serviceInterface The interface class of the service. Must not be null.
     * @throws ServiceException if setup fails.
     */
    private void setupService(Class<? extends IService> serviceInterface) throws ServiceException {
        IService instance = Objects.requireNonNull(serviceInstances.get(serviceInterface),
                "Instance not found for setup: " + serviceInterface.getSimpleName());
        ServiceState state = serviceStates.get(serviceInterface);

        if (state == ServiceState.SETUP || state == ServiceState.INITIALIZING || state == ServiceState.INITIALIZED) {
            LoggerUtil.debug(LOG_PREFIX, "Skipping setup for already processed service: " + serviceInterface.getSimpleName());
            return;
        }
        if (state != ServiceState.CONSTRUCTED) {
            throw new ServiceException("Cannot setup service " + serviceInterface.getSimpleName() +
                    " - unexpected state: " + state + ". Expected CONSTRUCTED.");
        }

        LoggerUtil.info(LOG_PREFIX, "Setting up service: " + serviceInterface.getSimpleName());
        try {
            instance.setup(this); // Pass context
            serviceStates.put(serviceInterface, ServiceState.SETUP);
            LoggerUtil.info(LOG_PREFIX, "Setup service: " + serviceInterface.getSimpleName() + " successfully.");
        } catch (Exception e) {
            serviceStates.put(serviceInterface, ServiceState.FAILED);
            LoggerUtil.error(LOG_PREFIX, "Failed to setup service: " + serviceInterface.getSimpleName(), e);
            throw new ServiceException("Setup failed for service " + serviceInterface.getName(), e);
        }
    }

    /**
     * Calls the initialize() method on a service instance.
     *
     * @param serviceInterface The interface class of the service. Must not be null.
     * @throws ServiceException if initialization fails.
     */
    private void initializeService(Class<? extends IService> serviceInterface) throws ServiceException {
        IService instance = Objects.requireNonNull(serviceInstances.get(serviceInterface),
                "Instance not found for initialize: " + serviceInterface.getSimpleName());
        ServiceState state = serviceStates.get(serviceInterface);

        if (state == ServiceState.INITIALIZING || state == ServiceState.INITIALIZED) {
            LoggerUtil.debug(LOG_PREFIX, "Skipping initialization for already processed service: " + serviceInterface.getSimpleName());
            return;
        }
        if (state != ServiceState.SETUP) {
            throw new ServiceException("Cannot initialize service " + serviceInterface.getSimpleName() +
                    " - must be in SETUP state (Current: " + state + ")");
        }

        serviceStates.put(serviceInterface, ServiceState.INITIALIZING);
        LoggerUtil.info(LOG_PREFIX, "Initializing service: " + serviceInterface.getSimpleName());
        try {
            instance.initialize(this); // Pass context
            serviceStates.put(serviceInterface, ServiceState.INITIALIZED);
            LoggerUtil.info(LOG_PREFIX, "Initialized service: " + serviceInterface.getSimpleName() + " successfully.");
        } catch (Exception e) {
            serviceStates.put(serviceInterface, ServiceState.FAILED);
            LoggerUtil.error(LOG_PREFIX, "Failed to initialize service: " + serviceInterface.getSimpleName(), e);
            throw new ServiceException("Initialization failed for service " + serviceInterface.getName(), e);
        }
    }

    /**
     * Retrieves a service instance by its interface class.
     *
     * <p><b>WARNING FOR CABB0003:</b> To accommodate dependencies needed during the setup phase (like in ModuleManagerService),
     * this method returns a service instance as long as it has been constructed (i.e., state is CONSTRUCTED,
     * SETUP, INITIALIZING, or INITIALIZED). Callers outside the core initialization logic should ideally
     * wait until the context is fully initialized or use this method with extreme caution, as the
     * returned service might not be fully functional yet.</p>
     *
     * @param serviceInterface The interface class of the service. Must not be null.
     * @param <T>              The type of the service interface.
     * @return An Optional containing the service instance if found and constructed (or beyond), otherwise empty.
     */
    public <T extends IService> Optional<T> getService(Class<T> serviceInterface) {
        Objects.requireNonNull(serviceInterface, "serviceInterface cannot be null");

        IService instance = serviceInstances.get(serviceInterface);
        if (instance == null) {
            LoggerUtil.debug(LOG_PREFIX, "Service instance not found in map for: " + serviceInterface.getSimpleName());
            return Optional.empty();
        }

        ServiceState state = serviceStates.get(serviceInterface);

        if (state == ServiceState.CONSTRUCTED || state == ServiceState.SETUP || state == ServiceState.INITIALIZING || state == ServiceState.INITIALIZED) {
            @SuppressWarnings("unchecked")
            T castInstance = (T) instance;
            return Optional.of(castInstance);
        } else {
            LoggerUtil.debug(LOG_PREFIX, "Service " + serviceInterface.getSimpleName() + " requested but not in an injectable state (Current State: " + state + ")");
            return Optional.empty();
        }
    }

    /**
     * Shuts down all managed services gracefully in reverse priority order.
     */
    public void shutdown() {
        shutdownInternal(false);
    }

    /**
     * Emergency shutdown, bypassing normal shutdown procedures.
     *
     * <p>This method is intended for use in critical situations where a normal shutdown
     * is not possible or has failed. It will attempt to forcefully shut down all services
     * without regard for their current state.</p>
     *
     * @param emergency Indicates that this is an emergency shutdown.
     */
    private void shutdownInternal(boolean emergency) {
        LoggerUtil.info(LOG_PREFIX, "--- ClientContext Shutdown Start " + (emergency ? "(Emergency)" : "") + " ---");

        boolean canSkipShutdown = !emergency && serviceStates.values().stream()
                .allMatch(s -> s == ServiceState.SHUTDOWN || s == ServiceState.FAILED || s == ServiceState.PENDING);

        if (canSkipShutdown) {
            LoggerUtil.info(LOG_PREFIX, "Shutdown requested, but services appear already shutdown, failed, or were never initialized.");
            serviceInstances.clear();
            serviceStates.clear();
            serviceRegistry = Collections.emptyMap();
            LoggerUtil.info(LOG_PREFIX, "--- ClientContext Shutdown Complete (Skipped) ---");
            return;
        }

        List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> shutdownOrder =
                this.serviceRegistry.entrySet().stream()
                        .sorted((e1, e2) -> Integer.compare(
                                getServicePriority(e2.getValue()),
                                getServicePriority(e1.getValue())
                        ))
                        .collect(Collectors.toList());


        LoggerUtil.info(LOG_PREFIX, "Service shutdown order: " + formatServiceOrder(shutdownOrder));

        for (Map.Entry<Class<? extends IService>, Class<? extends IService>> entry : shutdownOrder) {
            Class<? extends IService> iface = entry.getKey();
            IService instance = serviceInstances.get(iface);
            ServiceState currentState = serviceStates.getOrDefault(iface, ServiceState.PENDING);

            boolean shouldAttemptShutdown = currentState != ServiceState.SHUTDOWN
                    && currentState != ServiceState.FAILED
                    && currentState != ServiceState.PENDING
                    && currentState != ServiceState.CONSTRUCTING;


            if (instance != null && shouldAttemptShutdown) {
                LoggerUtil.info(LOG_PREFIX, "Shutting down service: " + iface.getSimpleName() + " (State: " + currentState + ")");
                serviceStates.put(iface, ServiceState.SHUTTING_DOWN);
                try {
                    instance.shutdown(this);
                    serviceStates.put(iface, ServiceState.SHUTDOWN);
                    LoggerUtil.info(LOG_PREFIX, "Shutdown service: " + iface.getSimpleName() + " successfully.");
                } catch (Throwable t) {
                    serviceStates.put(iface, ServiceState.FAILED);
                    LoggerUtil.error(LOG_PREFIX, "Error shutting down service " + iface.getName() + ": " + t.getMessage(), t);
                }
            } else {
                if (instance == null && currentState != ServiceState.PENDING) {
                    LoggerUtil.info(LOG_PREFIX, "Skipping shutdown for service: " + iface.getSimpleName() + " (Instance was null, State: " + currentState + ")");
                } else {
                    LoggerUtil.info(LOG_PREFIX, "Skipping shutdown for service: " + iface.getSimpleName() + " (Not in appropriate state for shutdown, State: " + currentState + ")");
                }

                if (currentState != ServiceState.FAILED && currentState != ServiceState.SHUTDOWN) {
                    serviceStates.putIfAbsent(iface, ServiceState.FAILED);
                }
            }
        }

        serviceInstances.clear();
        serviceStates.clear();
        serviceRegistry = Collections.emptyMap();
        LoggerUtil.info(LOG_PREFIX, "--- ClientContext Shutdown Complete ---");
    }

    private String formatServiceOrder(List<Map.Entry<Class<? extends IService>, Class<? extends IService>>> services) {
        return services.stream()
                .map(e -> e.getKey().getSimpleName() + " [" + getServicePriority(e.getValue()) + "](" + e.getValue().getSimpleName() + ")")
                .collect(Collectors.joining(" -> "));
    }
}