package dev.revere.valance.settings.type;

import dev.revere.valance.settings.Setting;
import lombok.Getter;

import java.util.function.Supplier;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
public class NumberSetting<N extends Number & Comparable<N>> extends Setting<N> {
    private final N minimum;
    private final N maximum;
    private final N increment;

    public NumberSetting(String name, N defaultValue, N minimum, N maximum, N increment, String description, Supplier<Boolean> visibilitySupplier) {
        super(name, defaultValue, description, visibilitySupplier);
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
        if (defaultValue.compareTo(minimum) < 0 || defaultValue.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Default value (" + defaultValue + ") for setting '" + name + "' must be within min/max bounds [" + minimum + ", " + maximum + "]");
        }
    }

    public NumberSetting(String name, N defaultValue, N minimum, N maximum, N increment) {
        this(name, defaultValue, minimum, maximum, increment, "", () -> true);
    }

    public NumberSetting(String name, N defaultValue, N minimum, N maximum) {
        this(name, defaultValue, minimum, maximum, null, "", () -> true);
    }

    public NumberSetting(String name, N defaultValue, N minimum, N maximum, Supplier<Boolean> visibility) {
        this(name, defaultValue, minimum, maximum, null, "", visibility);
    }

    @Override
    public void setValue(N value) {
        N clampedValue = clamp(value);
        super.setValue(clampedValue);
    }

    private N clamp(N value) {
        if (value.compareTo(minimum) < 0) {
            return minimum;
        }
        if (value.compareTo(maximum) > 0) {
            return maximum;
        }
        return value;
    }

    @Override
    public boolean setValueFromObject(Object loadedValue) {
        if (!(loadedValue instanceof Number)) {

            System.err.println("[Setting:" + getName() + "] Config load error: Expected a Number, got " + (loadedValue == null ? "null" : loadedValue.getClass().getSimpleName()));
            return false;
        }

        Number num = (Number) loadedValue;
        N convertedValue;

        Class<?> defaultType = getDefaultValue().getClass();
        try {
            if (defaultType == Integer.class) convertedValue = (N) Integer.valueOf(num.intValue());
            else if (defaultType == Double.class) convertedValue = (N) Double.valueOf(num.doubleValue());
            else if (defaultType == Float.class) convertedValue = (N) Float.valueOf(num.floatValue());
            else if (defaultType == Long.class) convertedValue = (N) Long.valueOf(num.longValue());
            else if (defaultType == Short.class) convertedValue = (N) Short.valueOf(num.shortValue());
            else if (defaultType == Byte.class) convertedValue = (N) Byte.valueOf(num.byteValue());
            else {
                System.err.println("[Setting:" + getName() + "] Config load error: Unsupported Number type " + defaultType.getSimpleName());
                return false;
            }

            setValue(convertedValue);
            return true;
        } catch (ClassCastException | NumberFormatException e) {
            System.err.println("[Setting:" + getName() + "] Config load error: Failed to convert loaded value '" + loadedValue + "' to " + defaultType.getSimpleName());
            return false;
        }
    }
}