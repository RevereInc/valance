package dev.revere.valance.settings.type;

import dev.revere.valance.settings.Setting;

import java.util.function.Supplier;

public class StringSetting extends Setting<String> {

    public StringSetting(String name, String defaultValue, String description, Supplier<Boolean> visibilitySupplier) {
        super(name, defaultValue == null ? "" : defaultValue, description, visibilitySupplier);
    }

    public StringSetting(String name, String defaultValue) {
        this(name, defaultValue, "", () -> true);
    }

    public StringSetting(String name, String defaultValue, Supplier<Boolean> visibilitySupplier) {
        this(name, defaultValue, "", visibilitySupplier);
    }

    public StringSetting(String name, String defaultValue, String description) {
        this(name, defaultValue, description, () -> true);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value == null ? "" : value);
    }

    @Override
    public boolean setValueFromObject(Object loadedValue) {
        if (loadedValue instanceof String) {
            setValue((String) loadedValue);
            return true;
        } else if (loadedValue != null) {
            System.err.println("[Setting:" + getName() + "] Config load error: Expected String, got " + loadedValue.getClass().getSimpleName() + ". Using default.");
            resetToDefault();
            return false;
        } else {
            System.out.println("[Setting:" + getName() + "] Config load warning: Loaded value was null, using default.");
            resetToDefault();
            return true;
        }
    }
}