package dev.revere.valance.settings.type;

import dev.revere.valance.settings.Setting;

import java.util.function.Supplier;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, Boolean defaultValue, String description, Supplier<Boolean> visibilitySupplier) {
        super(name, defaultValue, description, visibilitySupplier);
    }

    public BooleanSetting(String name, Boolean defaultValue) {
        super(name, defaultValue);
    }

    public BooleanSetting(String name, Boolean defaultValue, Supplier<Boolean> visibilitySupplier) {
        super(name, defaultValue, visibilitySupplier);
    }

    public BooleanSetting(String name, Boolean defaultValue, String description) {
        super(name, defaultValue, description);
    }
}