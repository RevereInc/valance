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
import dev.revere.valance.module.impl.misc.AutoRegisterBotModule;
import dev.revere.valance.module.impl.misc.InfoModule;
import dev.revere.valance.module.impl.misc.NetworkStatsModule;
import dev.revere.valance.module.impl.misc.ProxyCrashModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Service(provides = IModuleManager.class)
public class ModuleManagerService implements IModuleManager {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":ModuleManager] ";

    // --- Injected Dependencies ---
    private final IEventBusService eventBusService;


    // --- State ---
    private final Map<String, IModule> modulesByName = new ConcurrentHashMap<>();
    private final Map<Class<? extends IModule>, IModule> modulesByClass = new ConcurrentHashMap<>();
    private ClientContext context;

    @Inject
    public ModuleManagerService(IEventBusService eventBusService /*, IConfigService configService */) {
        System.out.println(LOG_PREFIX + "Constructed.");
        this.eventBusService = Objects.requireNonNull(eventBusService, "EventBusService cannot be null");
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Setting up...");
        this.context = Objects.requireNonNull(context, "ClientContext cannot be null during setup");
        discoverAndRegisterModules();
        System.out.println(LOG_PREFIX + "Setup complete. Registered " + modulesByName.size() + " modules.");
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Initializing...");
        long enabledCount = getModules().stream().filter(IModule::isEnabled).count();
        long totalCount = getModules().size();
        System.out.println(LOG_PREFIX + "Initialization complete. " + enabledCount + " of " + totalCount + " modules are enabled.");
    }

    private void discoverAndRegisterModules() throws ServiceException {
        System.out.println(LOG_PREFIX + "Discovering and registering modules...");
        List<Class<? extends IModule>> moduleClassesToRegister = List.of(
                InfoModule.class,
                NetworkStatsModule.class,
                ProxyCrashModule.class,
                AutoRegisterBotModule.class
        );

        for (Class<? extends IModule> moduleClass : moduleClassesToRegister) {
            if (!IModule.class.isAssignableFrom(moduleClass) || moduleClass.isInterface() || java.lang.reflect.Modifier.isAbstract(moduleClass.getModifiers())) {
                System.err.println(LOG_PREFIX + "[WARN] Skipping non-concrete IModule class: " + moduleClass.getName());
                continue;
            }

            ModuleInfo info = moduleClass.getAnnotation(ModuleInfo.class);
            if (info == null) {
                System.err.println(LOG_PREFIX + "[WARN] Module class " + moduleClass.getSimpleName() + " is missing @ModuleInfo annotation, skipping.");
                continue;
            }

            try {
                // --- Dependency Injection for Modules ---
                Constructor<?> constructor = ReflectionUtil.getModuleConstructor(moduleClass);
                Class<?>[] paramTypes = constructor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];

                for (int i = 0; i < paramTypes.length; i++) {
                    args[i] = resolveModuleDependency(paramTypes[i], moduleClass);
                }

                // Instantiate the module with resolved dependencies
                IModule moduleInstance = (IModule) constructor.newInstance(args);

                // Register the instantiated module
                String moduleNameLower = moduleInstance.getName().toLowerCase();
                if (modulesByName.containsKey(moduleNameLower) || modulesByClass.containsKey(moduleClass)) {
                    System.err.println(LOG_PREFIX + "[WARN] Duplicate module name/class detected, skipping registration: " + moduleInstance.getName());
                } else {
                    modulesByName.put(moduleNameLower, moduleInstance);
                    modulesByClass.put(moduleClass, moduleInstance);
                    // System.out.println(LOG_PREFIX + "[DEBUG] Registered module: " + moduleInstance.getName());
                }

            } catch (Exception e) {
                System.err.println(LOG_PREFIX + "[ERROR] Failed to instantiate or register module " + moduleClass.getSimpleName() + ".");
                throw new ServiceException("Failed to process module: " + moduleClass.getSimpleName(), e);
            }
        }
        System.out.println(LOG_PREFIX + "Module registration complete.");
    }

    /**
     * Resolves a dependency required by a module's constructor.
     * Currently, supports IService interfaces provided by the context.
     *
     * @param dependencyType  The Class type of the dependency needed.
     * @param requiringModule The Class of the module requesting the dependency (for logging).
     * @return The resolved dependency instance.
     * @throws ServiceException if the dependency cannot be resolved.
     */
    private Object resolveModuleDependency(Class<?> dependencyType, Class<?> requiringModule) throws ServiceException {
        // Check if it's a known Service interface
        if (IService.class.isAssignableFrom(dependencyType) && dependencyType.isInterface()) {
            @SuppressWarnings("unchecked")
            Class<? extends IService> serviceInterface = (Class<? extends IService>) dependencyType;

            return context.getService(serviceInterface)
                    .orElseThrow(() -> new ServiceException("Failed to inject required service dependency [" + serviceInterface.getSimpleName() + "] into module [" + requiringModule.getSimpleName() + "]. Ensure the service is registered and initialized before modules."));
        }

        throw new ServiceException("Unsupported dependency type [" + dependencyType.getSimpleName() + "] requested by module [" + requiringModule.getSimpleName() + "]. Only IService interfaces are currently injectable.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Shutting down...");
        final int[] disableCount = {0};
        getModules().stream()
                .filter(IModule::isEnabled)
                .forEach(module -> {
                    try {
                        module.setEnabled(false);
                        disableCount[0]++;
                    } catch (Exception e) {
                        System.err.println(LOG_PREFIX + "[ERROR] Error disabling module " + module.getName() + " during shutdown:");
                        e.printStackTrace();
                    }
                });
        System.out.println(LOG_PREFIX + "Disabled " + disableCount[0] + " active modules.");
        modulesByName.clear();
        modulesByClass.clear();
        System.out.println(LOG_PREFIX + "Shutdown complete.");
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
}