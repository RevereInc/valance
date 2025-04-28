package dev.revere.valance.event.type.client;

import dev.revere.valance.event.IEvent;
import dev.revere.valance.module.api.IModule;
import lombok.Getter;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
public class ModuleStateChangedEvent implements IEvent {
    private final IModule module;
    private final boolean newState;

    /**
     * Constructor for ModuleStateChangedEvent.
     *
     * @param module   The module whose state has changed.
     * @param newState The new state of the module (enabled or disabled).
     */
    public ModuleStateChangedEvent(IModule module, boolean newState) {
        this.module = module;
        this.newState = newState;
    }

    /**
     * Checks if the module is enabled.
     *
     * @return true if the module is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return newState;
    }
}