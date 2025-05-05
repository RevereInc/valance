package dev.revere.valance.ui.screen.alt.components;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.util.LoggerUtil;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 5/5/2025
 */
@Getter
@Setter
public class GuiMainButton extends Gui {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":GuiMainButton]";
    private final ISkijaService skijaService;

    private String name;
    private int id;
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean hovered;

    private static final float BASE_FONT_SIZE = 10f;

    /**
     * Initializes the button.
     *
     * @param name   The display name of the button.
     * @param id     The identifier for the button.
     * @param width  The width in scaled coordinates.
     * @param height The height in scaled coordinates.
     */
    public GuiMainButton(String name, int id, float width, float height) {
        this.name = name;
        this.id = id;
        this.width = width;
        this.height = height;

        Optional<ISkijaService> skijaOpt = ClientLoader.getService(ISkijaService.class);
        if (skijaOpt.isEmpty()) {
            LoggerUtil.error(LOG_PREFIX, "Failed to get SkijaService instance for button '" + name + "'.");
            throw new IllegalStateException("SkijaService is not available for GuiMainButton.");
        }
        this.skijaService = skijaOpt.get();
    }

    /**
     * Gets the scaled X coordinate of the right edge.
     *
     * @return Scaled X + Scaled Width.
     */
    public float getScaledWidthEnd() {
        return getX() + getWidth();
    }

    /**
     * Gets the scaled Y coordinate of the bottom edge.
     *
     * @return Scaled Y + Scaled Height.
     */
    public float getScaledHeightEnd() {
        return getY() + getHeight();
    }

    /**
     * Draws the button's text using Skija, converting coordinates.
     * Assumes the parent (AltManager) calls this within a skijaService.runFrame().
     *
     * @param mouseX      Current scaled mouse X coordinate.
     * @param mouseY      Current scaled mouse Y coordinate.
     * @param scaleFactor The current GUI scale factor.
     * @param canvas      The Skija canvas to draw on.
     */
    public void drawButton(int mouseX, int mouseY, float scaleFactor, io.github.humbleui.skija.Canvas canvas) {
        setHovered(mouseX >= getX() && mouseX < getScaledWidthEnd() && mouseY >= getY() && mouseY < getScaledHeightEnd());

        float pixelX = this.x * scaleFactor;
        float pixelY = this.y * scaleFactor;
        float pixelWidth = this.width * scaleFactor;
        float pixelHeight = this.height * scaleFactor;

        int bgColor = isHovered() ? 0x99505050 : 0x77303030;
        try (var paint = new Paint().setColor(bgColor).setAntiAlias(true)) {
            canvas.drawRect(Rect.makeXYWH(pixelX, pixelY, pixelWidth, pixelHeight), paint);
        }

        float pixelFontSize = BASE_FONT_SIZE * scaleFactor;
        try (Typeface typeface = Typeface.makeDefault();
             Font font = new Font(typeface, pixelFontSize);
             var textPaint = new Paint().setColor(Color.WHITE.getRGB()).setAntiAlias(true)) {

            float textWidth = font.measureTextWidth(getName());
            float textX = pixelX + (pixelWidth - textWidth) / 2f;

            var fm = font.getMetrics();
            float textY = pixelY + (pixelHeight / 2f) - (fm.getAscent() + fm.getDescent()) / 2f;

            canvas.drawString(getName(), textX, textY, font, textPaint);
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Error drawing Skija text for button '" + getName() + "': " + e.getMessage());
        }
    }

    /**
     * Fired when a button is clicked (uses scaled coordinates).
     *
     * @param mouseX Scaled mouse X.
     * @param mouseY Scaled mouse Y.
     * @return true if the click was inside the button's scaled bounds.
     */
    public boolean mousePressed(int mouseX, int mouseY) {
        boolean pressed = mouseX >= getX() && mouseX < getScaledWidthEnd() && mouseY >= getY() && mouseY < getScaledHeightEnd();
        if (pressed) {
            playPressSound(Minecraft.getMinecraft().getSoundHandler());
        }
        return pressed;
    }

    /**
     * Plays the standard button press sound.
     *
     * @param soundHandlerIn The sound handler instance.
     */
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}