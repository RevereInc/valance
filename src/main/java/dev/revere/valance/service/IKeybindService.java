package dev.revere.valance.service;

import dev.revere.valance.core.lifecycle.IService;
import net.minecraft.client.gui.GuiScreen;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
public interface IKeybindService extends IService {
    /**
     * Checks if the given screen is invalid.
     *
     * @param screen The screen to check.
     * @return true if the screen is invalid, false otherwise.
     */
    boolean isInvalidScreen(GuiScreen screen);
}