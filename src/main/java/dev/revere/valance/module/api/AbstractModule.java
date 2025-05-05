package dev.revere.valance.module.api;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.type.client.ModuleStateChangedEvent;
import dev.revere.valance.input.BindType;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.properties.Property;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.util.LoggerUtil;
import dev.revere.valance.util.MinecraftUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
@Setter
public abstract class AbstractModule implements IModule {
    protected static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":Module]";

    public final Minecraft mc = Minecraft.getMinecraft();

    private final List<Property<?>> properties = new ArrayList<>();

    // --- Injected Services (Common) ---
    protected final IEventBusService eventBusService;

    // --- Module Metadata & State ---
    private final String name;
    private final String displayName;
    private final String description;
    private final Category category;
    private final boolean hidden;

    private boolean enabled = false;
    private int key = Keyboard.CHAR_NONE;
    private BindType bindType = BindType.TOGGLE;

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public AbstractModule(IEventBusService eventBusService) {
        this.eventBusService = eventBusService;

        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info == null) {
            throw new IllegalStateException("Module class " + getClass().getSimpleName() + " is missing @ModuleInfo annotation!");
        }
        this.name = info.name();
        this.displayName = info.displayName();
        this.description = info.description();
        this.category = info.category();
        this.hidden = info.isHidden();
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

    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            IModule moduleInstance = this;
            boolean previousState = this.enabled;
            this.enabled = enabled;

            try {
                if (enabled) {
                    onEnable();
                    LoggerUtil.info(LOG_PREFIX, "Enabled: " + getName());
                } else {
                    onDisable();
                    LoggerUtil.info(LOG_PREFIX, "Disabled: " + getName());
                }
                this.eventBusService.post(new ModuleStateChangedEvent(moduleInstance, enabled));
            } catch (Exception e) {
                LoggerUtil.error(LOG_PREFIX, "Exception during state change for " + getName() + " to " + enabled + ":");
                e.printStackTrace();
                this.enabled = previousState;

                try {
                    if (enabled) {
                        onDisable();
                    } else {
                        onEnable();
                    }
                } catch (Exception cleanupError) {
                    LoggerUtil.error(LOG_PREFIX, "Further exception during state change cleanup for " + getName());
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
     * Checks if the module is bound to a key.
     *
     * @return true if the module is bound, false otherwise.
     */
    @Override
    public boolean isBound() {
        return key != Keyboard.CHAR_NONE;
    }

    /**
     * Retrieves the property hierarchy.
     *
     * @return The property hierarchy of the module.
     */
    @Override
    public List<Property<?>> getPropertyHierarchy() {
        List<Property<?>> hierarchy = new ArrayList<>();

        for (Property<?> property : properties) {
            hierarchy.add(property);
            hierarchy.addAll(property.getHierarchy());
        }

        return hierarchy;
    }

    protected final MinecraftUtil mc() {
        return null;
    }
}
