package dev.revere.valance.module.api;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.type.client.ModuleStateChangedEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.util.MinecraftUtil;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public abstract class AbstractModule implements IModule {
    protected static final String LOG_PREFIX_MODULE = "[" + ClientLoader.CLIENT_NAME + ":Module] ";

    public final Minecraft mc = Minecraft.getMinecraft();

    private List<Setting<?>> discoveredSettings = null;

    // --- Injected Services (Common) ---
    protected final IEventBusService eventBusService;

    // --- Module Metadata & State ---
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public AbstractModule(IEventBusService eventBusService /*, todo: other services */) {
        this.eventBusService = eventBusService;

        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info == null) {
            throw new IllegalStateException("Module class " + getClass().getSimpleName() + " is missing @ModuleInfo annotation!");
        }
        this.name = info.name();
        this.description = info.description();
        this.category = info.category();
    }

    /**
     * Base implementation registers the module with the EventBus.
     * Concrete modules can override to add specific setup AFTER calling super.onEnable().
     */
    @Override
    public void onEnable() {
        this.eventBusService.register(this);
        // System.out.println(LOG_PREFIX_MODULE + "[DEBUG] Listener registered for: " + getName());
    }

    /**
     * Base implementation unregisters the module from the EventBus.
     * Concrete modules can override to add specific cleanup BEFORE calling super.onDisable().
     */
    @Override
    public void onDisable() {
        this.eventBusService.unregister(this);
        // System.out.println(LOG_PREFIX_MODULE + "[DEBUG] Listener unregistered for: " + getName());
    }

    // --- Getters ---
    @Override public final String getName() { return name; }
    @Override public final String getDescription() { return description; }
    @Override public final Category getCategory() { return category; }
    @Override public final boolean isEnabled() { return enabled; }

    // --- State Management ---
    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            IModule moduleInstance = this;
            boolean previousState = this.enabled;
            this.enabled = enabled;

            try {
                if (enabled) {
                    onEnable();
                    System.out.println(LOG_PREFIX_MODULE + "Enabled: " + getName());
                } else {
                    onDisable();
                    System.out.println(LOG_PREFIX_MODULE + "Disabled: " + getName());
                }
                this.eventBusService.post(new ModuleStateChangedEvent(moduleInstance, enabled));
            } catch (Exception e) {
                System.err.println(LOG_PREFIX_MODULE + "[ERROR] Exception during state change for " + getName() + " to " + enabled + ":");
                e.printStackTrace();
                this.enabled = previousState;

                try {
                    if (enabled) {
                        onDisable();
                    } else {
                        onEnable();
                    }
                } catch (Exception cleanupError) {
                    System.err.println(LOG_PREFIX_MODULE + "[ERROR] Further exception during state change cleanup for " + getName());
                    cleanupError.printStackTrace();
                }
            }
        }
    }

    @Override
    public final void toggle() {
        setEnabled(!isEnabled());
    }

    /**
     * Discovers and returns settings declared as fields within this module instance.
     * Uses reflection and caches the result.
     * @return An unmodifiable list of settings found in this module.
     */
    @Override
    public final List<Setting<?>> getSettings() {
        if (discoveredSettings == null) {
            discoveredSettings = findSettingsViaReflection();
        }
        return discoveredSettings;
    }

    private List<Setting<?>> findSettingsViaReflection() {
        System.out.println(LOG_PREFIX_MODULE + "[DEBUG] Discovering settings for: " + getName());
        List<Setting<?>> foundSettings = new ArrayList<>();
        Class<?> currentClass = this.getClass();

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (Setting.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Object settingObject = field.get(this);
                        if (settingObject instanceof Setting<?>) {
                            Setting<?> setting = (Setting<?>) settingObject;
                            if(foundSettings.stream().noneMatch(s -> s.getName().equalsIgnoreCase(setting.getName()))){
                                foundSettings.add(setting);
                            } else {
                                System.err.println(LOG_PREFIX_MODULE + "[WARN] Duplicate setting name '" + setting.getName() + "' found in " + getName() + ". Check field declarations.");
                            }
                        }
                    } catch (IllegalAccessException e) {
                        System.err.println(LOG_PREFIX_MODULE + "[ERROR] Failed to access setting field '" + field.getName() + "' in " + getName());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.err.println(LOG_PREFIX_MODULE + "[ERROR] Error accessing field '" + field.getName() + "' in " + getName() + ": " + e.getMessage());
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        System.out.println(LOG_PREFIX_MODULE + "[DEBUG] Found " + foundSettings.size() + " settings for: " + getName());
        return Collections.unmodifiableList(foundSettings);
    }

    protected final MinecraftUtil mc() { return null; }
}
