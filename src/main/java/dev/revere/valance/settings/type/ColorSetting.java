package dev.revere.valance.settings.type;

import dev.revere.valance.settings.Setting;

import java.awt.*;
import java.util.function.Supplier;

public class ColorSetting extends Setting<Integer> {

    private final boolean hasAlpha;

    public ColorSetting(String name, Integer defaultValue, boolean hasAlpha, String description, Supplier<Boolean> visibilitySupplier) {
        super(name, defaultValue, description, visibilitySupplier);
        this.hasAlpha = hasAlpha;
    }

    public ColorSetting(String name, Integer defaultValue, boolean hasAlpha) {
        this(name, defaultValue, hasAlpha, "", () -> true);
    }

    public ColorSetting(String name, Color defaultValue, boolean hasAlpha) {
        this(name, defaultValue.getRGB(), hasAlpha, "", () -> true);
    }

    public ColorSetting(String name, Color defaultValue) {
        this(name, defaultValue.getRGB(), true, "", () -> true);
    }

    public ColorSetting(String name, Integer defaultValue) {
        this(name, defaultValue, true, "", () -> true);
    }

    /**
     * Checks if this color setting includes an alpha component.
     * @return true if alpha is used, false otherwise.
     */
    public boolean hasAlpha() {
        return hasAlpha;
    }

    /**
     * Gets the current color value as a java.awt.Color object.
     * @return The Color object.
     */
    public Color getColor() {
        return new Color(getValue(), this.hasAlpha);
    }

    /**
     * Sets the color value using a java.awt.Color object.
     * @param color The new Color object.
     */
    public void setColor(Color color) {
        setValue(color.getRGB()); // Store as integer
    }

    /**
     * Sets the color using individual RGBA components (0-255).
     */
    public void setColor(int r, int g, int b, int a) {
        int rgba = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF));
        setValue(rgba);
    }

    /**
     * Sets the color using individual RGB components (0-255), assuming full alpha.
     */
    public void setColor(int r, int g, int b) {
        setColor(r, g, b, 255);
    }


    // Override setValueFromObject for config loading
    @Override
    public boolean setValueFromObject(Object loadedValue) {
        // Config likely saves color as an integer (or maybe hex string)
        if (loadedValue instanceof Number) {
            // Handle integer representation directly
            setValue(((Number) loadedValue).intValue());
            return true;
        } else if (loadedValue instanceof String) {
            // Handle hex string representation (e.g., "#AARRGGBB" or "#RRGGBB")
            try {
                String hex = ((String) loadedValue).trim();
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                }
                // Parse long first to handle potential unsigned values, then cast to int
                long longVal = Long.parseLong(hex, 16);
                int intVal;
                if (hex.length() == 8) { // AARRGGBB
                    intVal = (int) longVal;
                } else if (hex.length() == 6) { // RRGGBB (assume full alpha)
                    intVal = 0xFF000000 | (int) longVal;
                } else {
                    System.err.println("[Setting:" + getName() + "] Config load error: Invalid hex string length '" + loadedValue + "'. Expected #RRGGBB or #AARRGGBB.");
                    return false;
                }
                setValue(intVal);
                return true;
            } catch (NumberFormatException e) {
                System.err.println("[Setting:" + getName() + "] Config load error: Could not parse hex string '" + loadedValue + "'.");
                return false;
            }
        }

        System.err.println("[Setting:" + getName() + "] Config load error: Expected Number or Hex String for Color, got " + (loadedValue == null ? "null" : loadedValue.getClass().getSimpleName()));
        return false;
    }
}