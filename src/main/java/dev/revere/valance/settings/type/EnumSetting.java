package dev.revere.valance.settings.type;

import dev.revere.valance.settings.Setting;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] enumConstants;

    public EnumSetting(String name, E defaultValue, String description, Supplier<Boolean> visibilitySupplier) {
        super(name, defaultValue, description, visibilitySupplier);
        this.enumConstants = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public EnumSetting(String name, E defaultValue) {
        this(name, defaultValue, "", () -> true);
    }

    public EnumSetting(String name, E defaultValue, Supplier<Boolean> visibility) {
        this(name, defaultValue, "", visibility);
    }

    public E[] getConstants() {
        return enumConstants;
    }

    public void cycle() {
        int currentIndex = getValue().ordinal();
        int nextIndex = (currentIndex + 1) % enumConstants.length;
        setValue(enumConstants[nextIndex]);
    }

    @Override
    public boolean setValueFromObject(Object loadedValue) {
        if (loadedValue instanceof String) {
            String enumName = (String) loadedValue;
            try {
                E matchedEnum = Arrays.stream(enumConstants)
                        .filter(e -> e.name().equalsIgnoreCase(enumName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No enum constant " + getDefaultValue().getDeclaringClass().getSimpleName() + "." + enumName));
                setValue(matchedEnum);
                return true;
            } catch (IllegalArgumentException e) {
                System.err.println("[Setting:" + getName() + "] Config load error: Invalid enum value '" + enumName + "' for type " + getDefaultValue().getDeclaringClass().getSimpleName());
                return false;
            }
        } else if (getDefaultValue() != null && getDefaultValue().getClass().isInstance(loadedValue)) {
            try {
                setValue((E) loadedValue);
                return true;
            } catch (ClassCastException ignored) {
            }
        }

        System.err.println("[Setting:" + getName() + "] Config load error: Expected String for Enum, got " + (loadedValue == null ? "null" : loadedValue.getClass().getSimpleName()));
        return false;
    }

}
