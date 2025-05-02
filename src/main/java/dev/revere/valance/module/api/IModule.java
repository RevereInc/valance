package dev.revere.valance.module.api;

import dev.revere.valance.event.Listener;
import dev.revere.valance.module.Category;
import dev.revere.valance.settings.Setting;

import java.util.List;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface IModule extends Listener {
    /**
     * Base implementation registers the module with the EventBus.
     * Concrete modules can override to add specific setup AFTER calling super.onEnable().
     */
    void onEnable();

    /**
     * Base implementation unregisters the module from the EventBus.
     * Concrete modules can override to add specific teardown BEFORE calling super.onDisable().
     */
    void onDisable();

    /**
     * Returns the name of the module.
     *
     * @return The name of the module.
     */
    String getName();

    /**
     * Returns the display name of the module.
     *
     * @return The display name of the module.
     */
    String getDisplayName();

    /**
     * Returns the description of the module.
     *
     * @return The description of the module.
     */
    String getDescription();

    /**
     * Returns the category of the module.
     *
     * @return The category of the module.
     */
    Category getCategory();

    /**
     * Returns the enabled state of the module.
     *
     * @return True if the module is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Sets the enabled state, triggering onEnable/onDisable and event registration/unregistration.
     * Implementations should handle potential exceptions during enable/disable.
     *
     * @param enabled Target state.
     */
    void setEnabled(boolean enabled);

    /**
     * Toggles the current enabled state.
     */
    void toggle();

    /**
     * Checks if the module is bound to a key.
     *
     * @return true if the module is bound, false otherwise.
     */
    boolean isBound();

    /**
     * Checks if the module is hidden.
     *
     * @return true if the module is hidden, false otherwise.
     */
    boolean isHidden();

    /**
     * Returns the list of settings associated with this module.
     *
     * @return The list of settings for the module.
     */
    List<Setting<?>> getSettings();
}
