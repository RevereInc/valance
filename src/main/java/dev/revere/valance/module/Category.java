package dev.revere.valance.module;

import lombok.Getter;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Miscellaneous"),
    CLIENT("Client")

    ;

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}