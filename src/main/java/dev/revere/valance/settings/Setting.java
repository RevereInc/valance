package dev.revere.valance.settings;

import lombok.Getter;

import java.util.function.Supplier;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
public class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private Supplier<Boolean> visibilitySupplier;

    public Setting(String name, T defaultValue, String description, Supplier<Boolean> visibilitySupplier) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.description = description;
        this.visibilitySupplier = visibilitySupplier;
    }

    public Setting(String name, T defaultValue) {
        this(name, defaultValue, "", () -> true);
    }

    public Setting(String name, T defaultValue, Supplier<Boolean> visibilitySupplier) {
        this(name, defaultValue, "", visibilitySupplier);
    }

    public Setting(String name, T defaultValue, String description) {
        this(name, defaultValue, description, () -> true);
    }

    /**
     * Checks if the setting should be visible based on its visibility supplier.
     *
     * @return true if visible, false otherwise.
     */
    public boolean isVisible() {
        return visibilitySupplier.get();
    }

    /**
     * Sets the current value of the setting.
     * Implementations (like NumberSetting) might override this to clamp values.
     *
     * @param value The new value.
     */
    public void setValue(T value) {
        if (value == null) {
            System.err.println("[Setting:" + name + "] Attempted to set null value!");
            return;
        }
        this.value = value;
    }

    /**
     * Sets the visibility condition dynamically.
     *
     * @param visibilitySupplier A supplier returning true if the setting should be visible.
     */
    public Setting<T> setVisibility(Supplier<Boolean> visibilitySupplier) {
        this.visibilitySupplier = visibilitySupplier;
        return this;
    }

    /**
     * Resets the setting to its default value.
     */
    public void resetToDefault() {
        this.value = this.defaultValue;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    /**
     * Internal method to set value from a loaded config, performing type checks.
     */
    @SuppressWarnings("unchecked")
    public boolean setValueFromObject(Object loadedValue) {
        if (defaultValue != null && loadedValue != null && defaultValue.getClass().isAssignableFrom(loadedValue.getClass())) {
            try {
                setValue((T) loadedValue);
                return true;
            } catch (ClassCastException e) {
                System.err.println("[Setting:" + name + "] Type mismatch during config load. Expected " + defaultValue.getClass().getSimpleName() + ", got " + loadedValue.getClass().getSimpleName());
                return false;
            }
        }
        // Handle specific cases like Double -> Integer conversion if needed, or Enum from String
        System.err.println("[Setting:" + name + "] Failed basic type check or null value during config load.");
        return false;
    }
}
