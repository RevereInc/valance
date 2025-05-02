package dev.revere.valance.service;

import dev.revere.valance.core.lifecycle.IService;
import dev.revere.valance.ui.draggable.Draggable;
import io.github.humbleui.skija.Canvas;

import java.util.List;

/**
 * @author Remi
 * @project valance
 * @date 4/30/2025
 */

public interface IDraggableService extends IService {
    /**
     * Registers a draggable component.
     *
     * @param draggable The draggable component to register.
     */
    void register(Draggable draggable);

    /**
     * Unregisters a draggable component.
     *
     * @param draggable The draggable component to unregister.
     */
    void unregister(Draggable draggable);

    /**
     * Retrieves a list of all registered draggable components.
     *
     * @return A list of registered draggables.
     */
    List<Draggable> getDraggables();

    /**
     * Draws all registered draggable components.
     *
     * @param mouseX The current mouse X position.
     * @param mouseY The current mouse Y position.
     * @param canvas  The canvas to draw on.
     */
    void draw(float mouseX, float mouseY, Canvas canvas);

    /**
     * Handles mouse click events for all registered draggable components.
     *
     * @param mouseX The current mouse X position.
     * @param mouseY The current mouse Y position.
     */
    void handleMouseClick(float mouseX, float mouseY, int button);

    /**
     * Handles mouse release events for all registered draggable components.
     *
     * @param button The button that was released.
     */
    void handleMouseRelease(int button);
}