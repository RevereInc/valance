package dev.revere.valance.service.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.core.lifecycle.IService;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IModuleManager;
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
@Service(provides = IModuleManager.class)
public class ModuleManagerService implements IModuleManager {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":ModuleManager]";

    // --- Configuration ---
    private static final String MODULE_IMPL_PACKAGE = "dev.revere.valance.module.impl";

    // --- Injected Dependencies ---
    private final IEventBusService eventBusService;

    // --- State ---
    private final Map<String, IModule> modulesByName = new ConcurrentHashMap<>();
    private final Map<Class<? extends IModule>, IModule> modulesByClass = new ConcurrentHashMap<>();
    private ClientContext context;

    @Inject
    public ModuleManagerService(IEventBusService eventBusService) {
        LoggerUtil.info(LOG_PREFIX, "Constructed.");
        this.eventBusService = Objects.requireNonNull(eventBusService, "EventBusService cannot be null");
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Setting up...");
        this.context = Objects.requireNonNull(context, "ClientContext cannot be null during ClientContext setup");
        discoverAndRegisterModules();
        LoggerUtil.info(LOG_PREFIX, "Setup complete. Registered " + modulesByName.size() + " modules.");
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Initializing...");
        long enabledCount = getModules().stream().filter(IModule::isEnabled).count();
        long totalCount = getModules().size();
        LoggerUtil.info(LOG_PREFIX, "Initialization complete. " + enabledCount + " of " + totalCount + " modules are currently enabled.");
    }

    /**
     * Discovers and registers modules using ClassGraph for performance.
     */
    private void discoverAndRegisterModules() throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Discovering and registering modules via ClassGraph in package: " + MODULE_IMPL_PACKAGE + "...");
        long startTimeNs = System.nanoTime();
        int potentialModuleCount = 0;
        int registeredModuleCount = 0;

        try (ScanResult scanResult = new ClassGraph()
                // .verbose()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(MODULE_IMPL_PACKAGE)
                .scan()) {

            List<ClassInfo> moduleClassInfoList = scanResult
                    .getClassesImplementing(IModule.class.getName())
                    .filter(classInfo ->
                            classInfo.hasAnnotation(ModuleInfo.class.getName()) &&
                                    !classInfo.isInterface() &&
                                    !classInfo.isAbstract()
                    );

            potentialModuleCount = moduleClassInfoList.size();
            LoggerUtil.info(LOG_PREFIX, "Found " + potentialModuleCount + " potential module classes.");

            for (ClassInfo moduleClassInfo : moduleClassInfoList) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends IModule> moduleClass = (Class<? extends IModule>) moduleClassInfo.loadClass();

                    ModuleInfo info = moduleClass.getAnnotation(ModuleInfo.class);
                    if (info == null) {
                        LoggerUtil.warn(LOG_PREFIX, "Module class " + moduleClass.getSimpleName() + " missing @ModuleInfo despite passing filter. Skipping.");
                        continue;
                    }

                    // --- Instantiate Module with Dependency Injection ---
                    Constructor<?> constructor = ReflectionUtil.getModuleConstructor(moduleClass);
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];

                    // Resolve constructor dependencies (Services from ClientContext)
                    for (int i = 0; i < paramTypes.length; i++) {
                        args[i] = resolveModuleDependency(paramTypes[i], moduleClass);
                    }

                    // Instantiate
                    IModule moduleInstance = (IModule) constructor.newInstance(args);

                    // --- Register Module ---
                    String moduleNameLower = moduleInstance.getName().toLowerCase();
                    if (modulesByName.containsKey(moduleNameLower) || modulesByClass.containsKey(moduleClass)) {
                        LoggerUtil.warn(LOG_PREFIX, "Duplicate module name/class detected, skipping registration: " + moduleInstance.getName());
                    } else {
                        modulesByName.put(moduleNameLower, moduleInstance);
                        modulesByClass.put(moduleClass, moduleInstance);
                        registeredModuleCount++;
                    }

                } catch (Exception e) {
                    LoggerUtil.error(LOG_PREFIX, "Failed to instantiate or register module " + moduleClassInfo.getName() + ".", e);
                    throw new ServiceException("Failed to process module: " + moduleClassInfo.getName(), e);
                }
            }
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Error during ClassGraph scanning for modules.", e);
            throw new ServiceException("Module discovery failed.", e);
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs);
        LoggerUtil.info(LOG_PREFIX, "Module registration complete (" + durationMs + "ms). Registered " + registeredModuleCount + "/" + potentialModuleCount + " modules.");
    }

    /**
     * Resolves a dependency required by a module's constructor.
     * Relies on ClientContext.getService() which (as modified previously)
     * can return services that are constructed but not fully initialized.
     *
     * @param dependencyType  The Class type of the dependency needed.
     * @param requiringModule The Class of the module requesting the dependency.
     * @return The resolved dependency instance.
     * @throws ServiceException if the dependency cannot be resolved.
     */
    private Object resolveModuleDependency(Class<?> dependencyType, Class<?> requiringModule) throws ServiceException {
        if (this.context == null) {
            throw new ServiceException("ClientContext is null within resolveModuleDependency. ModuleManagerService setup incomplete.");
        }

        if (IService.class.isAssignableFrom(dependencyType) && dependencyType.isInterface()) {
            @SuppressWarnings("unchecked")
            Class<? extends IService> serviceInterface = (Class<? extends IService>) dependencyType;

            return context.getService(serviceInterface)
                    .orElseThrow(() -> new ServiceException(
                            String.format("Failed to inject required service dependency [%s] into module [%s]. Service might not be constructed/registered or failed initialization.",
                                    serviceInterface.getSimpleName(), requiringModule.getSimpleName())
                    ));
        }

        throw new ServiceException(
                String.format("Unsupported dependency type [%s] requested by module [%s]. Only IService interfaces are currently injectable.",
                        dependencyType.getSimpleName(), requiringModule.getSimpleName())
        );
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Shutting down...");
        final int[] disableCount = {0};
        getModules().stream()
                .filter(IModule::isEnabled)
                .forEach(module -> {
                    try {
                        module.setEnabled(false);
                        disableCount[0]++;
                    } catch (Exception e) {
                        LoggerUtil.error(LOG_PREFIX, "Error disabling module " + module.getName() + " during shutdown:", e);
                    }
                });
        if (disableCount[0] > 0) {
            LoggerUtil.info(LOG_PREFIX, "Called setEnabled(false) on " + disableCount[0] + " previously active modules.");
        }

        modulesByName.clear();
        modulesByClass.clear();
        LoggerUtil.info(LOG_PREFIX, "Module registries cleared. Shutdown complete.");
    }

    @Override
    public Optional<IModule> getModule(String name) {
        return Optional.ofNullable(modulesByName.get(name == null ? "" : name.toLowerCase()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IModule> Optional<T> getModule(Class<T> moduleClass) {
        return Optional.ofNullable((T) modulesByClass.get(moduleClass));
    }

    @Override
    public List<IModule> getModules() {
        return List.copyOf(modulesByName.values());
    }

    @Override
    public List<IModule> getModulesInCategory(Category category) {
        if (category == null) return Collections.emptyList();
        return modulesByName.values().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IModule> List<T> getModulesOfType(Class<T> type) {
        return modulesByClass.values().stream()
                .filter(Objects::nonNull)
                .filter(type::isInstance)
                .map(m -> (T) m)
                .collect(Collectors.toUnmodifiableList());
    }
}