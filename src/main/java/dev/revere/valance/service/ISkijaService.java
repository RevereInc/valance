package dev.revere.valance.service;

import dev.revere.valance.core.lifecycle.IService;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.paragraph.FontCollection;

public interface ISkijaService extends IService {

    /**
     * Gets the Skija DirectContext associated with Minecraft's OpenGL context.
     * May be null if initialization failed.
     * @return The DirectContext or null.
     */
    DirectContext getContext();

    /**
     * Gets the Skija Surface linked to Minecraft's framebuffer.
     * May be null if not yet created or initialization failed.
     * Dimensions might need updating on resize.
     * @return The Surface or null.
     */
    Surface getSurface();

    /**
     * Gets the Skija Canvas for the current frame.
     * Ensures the surface is created/valid for the current dimensions.
     * This might trigger surface recreation if needed.
     * Should only be called between beginFrame/endFrame or within runFrame.
     * @return The Canvas to draw on, or null if unavailable.
     */
    Canvas getCanvas();

    /**
     * Gets the FontCollection for rendering text.
     * @return The FontCollection or null if unavailable.
     */
    FontCollection getFontCollection();

    /**
     * Ensures the Skija surface matches the current display dimensions.
     * Should be called if the screen size changes.
     */
    void ensureSurface();

    /**
     * Prepares Skija for rendering a frame.
     * Backs up Minecraft's GL state using UIState.
     */
    void beginFrame();

    /**
     * Finalizes Skija rendering for a frame.
     * Flushes the Skija context and restores Minecraft's GL state using UIState.
     */
    void endFrame();

    /**
     * Convenience method to wrap drawing logic within a begin/end frame block.
     * @param render The Runnable containing Skija drawing commands.
     */
    void runFrame(Runnable render);

    /**
     * Checks if the Skija service was initialized successfully.
     * @return true if ready, false otherwise.
     */
    boolean isInitialized();
}