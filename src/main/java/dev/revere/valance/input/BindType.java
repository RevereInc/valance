package dev.revere.valance.input;

import lombok.Getter;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@Getter
public enum BindType {
    HOLD("Hold"),
    TOGGLE("Toggle");

    private final String displayName;

    /**
     * Constructor for BindType enum.
     *
     * @param displayName The display name of the bind type.
     */
    BindType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retrieves the corresponding BindType enum for the given type string (case-insensitive).
     * Defaults to TOGGLE if no match is found.
     *
     * @param type The type string representing the bind type.
     * @return The BindType enum corresponding to the given type string.
     */
    public static BindType getBind(String type) {
        if (type == null) return TOGGLE;
        for (BindType bind : values()) {
            if (bind.displayName.equalsIgnoreCase(type) || bind.name().equalsIgnoreCase(type)) {
                return bind;
            }
        }
        return TOGGLE;
    }
}
