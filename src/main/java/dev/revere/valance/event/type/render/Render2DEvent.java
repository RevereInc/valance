package dev.revere.valance.event.type.render;

import dev.revere.valance.event.IEvent;
import lombok.Getter;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
public class Render2DEvent implements IEvent {
    private final float screenWidth;
    private final float screenHeight;
    private final float partialTicks;

    /**
     * Constructs a Render2DEvent.
     *
     * @param width        The current scaled screen width.
     * @param height       The current scaled screen height.
     * @param partialTicks The render partial ticks.
     */
    public Render2DEvent(float width, float height, float partialTicks) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.partialTicks = partialTicks;
    }
}