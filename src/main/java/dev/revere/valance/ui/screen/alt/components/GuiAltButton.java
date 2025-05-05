package dev.revere.valance.ui.screen.alt.components;

import dev.revere.valance.alt.Alt;
import dev.revere.valance.util.render.SkijaRenderUtil;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Remi
 * @project valance
 * @date 5/5/2025
 */
@Getter
@Setter
public class GuiAltButton {
    protected int id;
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean selected;
    public Alt alt;

    private static final float PADDING_SCALED = 3f;
    private static final float HEAD_AREA_SIZE_SCALED = 24f;
    private static final float TEXT_OFFSET_X_SCALED = HEAD_AREA_SIZE_SCALED + PADDING_SCALED * 2f;
    private static final float USERNAME_OFFSET_Y_SCALED = 14f;
    private static final float INFO_OFFSET_Y_SCALED = 22f;
    private static final float BASE_FONT_SIZE = 9f;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public GuiAltButton(int id, float x, float y, float width, float height, Alt alt) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.alt = Objects.requireNonNull(alt, "Alt object cannot be null for GuiAltButton");
        this.selected = false;
    }

    /**
     * Draws the entire button using Skija primitives.
     * Converts internal scaled coordinates to pixels for drawing.
     * Call this INSIDE the main Skija runFrame block in AltManager.
     *
     * @param canvas      The Skija canvas to draw on.
     * @param scaleFactor The current GUI scale factor.
     */
    public void drawButton(Canvas canvas, float scaleFactor) {
        if (alt == null) return;

        float pixelX = this.x * scaleFactor;
        float pixelY = this.y * scaleFactor;
        float pixelWidth = this.width * scaleFactor;
        float pixelHeight = this.height * scaleFactor;
        float pixelPadding = PADDING_SCALED * scaleFactor;
        float pixelHeadAreaSize = HEAD_AREA_SIZE_SCALED * scaleFactor;

        // --- 1. Draw Selection Background (if selected) ---
        if (selected) {
            try (Paint selectionPaint = new Paint().setColor(0xAA000000)) {
                Rect selectionRect = Rect.makeXYWH(pixelX, pixelY, pixelWidth, pixelHeight);
                canvas.drawRect(selectionRect, selectionPaint);
            }
        }

        // --- 2. Draw Head Placeholder ---
        float headPixelX = pixelX + pixelPadding;
        float headPixelY = pixelY + pixelPadding;
        float actualHeadPixelSize = Math.max(1f, pixelHeight - (pixelPadding * 2f));
        actualHeadPixelSize = Math.min(actualHeadPixelSize, pixelHeadAreaSize);

        Rect headPlaceholderRect = Rect.makeXYWH(headPixelX, headPixelY, actualHeadPixelSize, actualHeadPixelSize);
        try (Paint headPaint = new Paint().setColor(getAltTypeColor().getRGB())) {
            canvas.drawRect(headPlaceholderRect, headPaint);
        }
        try (Typeface typeface = Typeface.makeDefault();
             Font initialFont = new Font(typeface, actualHeadPixelSize * 0.6f);
             Paint initialPaint = new Paint().setColor(Color.BLACK.getRGB()).setAntiAlias(true)) {
            String initial = alt.getUsername() != null && !alt.getUsername().isEmpty() ? alt.getUsername().substring(0, 1).toUpperCase() : "?";
            float textWidth = initialFont.measureTextWidth(initial);
            float textX = headPixelX + (actualHeadPixelSize - textWidth) / 2f;
            var fm = initialFont.getMetrics();
            float textY = headPixelY + (actualHeadPixelSize / 2f) - (fm.getAscent() + fm.getDescent()) / 2f;
            canvas.drawString(initial, textX, textY, initialFont, initialPaint);
        }


        // --- 3. Draw Text Information ---
        float pixelTextOffsetX = (headPixelX + actualHeadPixelSize + pixelPadding) - pixelX;
        float pixelUsernameOffsetY = USERNAME_OFFSET_Y_SCALED * scaleFactor;
        float pixelInfoOffsetY = INFO_OFFSET_Y_SCALED * scaleFactor;
        float pixelFontSize = BASE_FONT_SIZE * scaleFactor;

        try (Typeface typeface = Typeface.makeDefault();
             Font font = new Font(typeface, pixelFontSize)) {

            String username = alt.getUsername() != null ? alt.getUsername() : "Error";
            String dateStr = "Unknown Date";
            if (alt.getCreationDate() > 0) {
                try {
                    dateStr = DATE_FORMAT.format(new Date(alt.getCreationDate()));
                } catch (Exception ignored) {
                }
            }
            String typeStr = alt.getType() != null ? alt.getType().toUpperCase(Locale.ROOT) : "N/A";
            int typeColor = getAltTypeColor().getRGB();
            int defaultColor = Color.WHITE.getRGB();

            SkijaRenderUtil.drawColoredString(
                    canvas,
                    font,
                    username,
                    pixelX + pixelTextOffsetX,
                    pixelY + pixelUsernameOffsetY,
                    defaultColor,
                    false,
                    false,
                    150
            );

            SkijaRenderUtil.drawColoredString(
                    canvas,
                    font,
                    dateStr,
                    pixelX + pixelTextOffsetX,
                    pixelY + pixelInfoOffsetY,
                    defaultColor,
                    false,
                    false,
                    150
            );

            float dateTextWidthPixels = font.measureTextWidth(dateStr);
            float typePixelOffsetX = 5 * scaleFactor;

            SkijaRenderUtil.drawColoredString(
                    canvas,
                    font,
                    typeStr,
                    pixelX + pixelTextOffsetX + dateTextWidthPixels + typePixelOffsetX,
                    pixelY + pixelInfoOffsetY,
                    typeColor,
                    false,
                    false,
                    150
            );

        } catch (Exception e) {
            System.err.println("Error drawing Skija text for alt button: " + e.getMessage());
        }
    }

    private Color getAltTypeColor() {
        if (alt == null || alt.getType() == null) {
            return Color.GRAY;
        }
        switch (alt.getType().toLowerCase(Locale.ROOT)) {
            case "cracked":
                return new Color(85, 255, 85);
            case "cookie":
                return new Color(85, 170, 255);
            case "microsoft":
                return new Color(255, 85, 85);
            default:
                return Color.WHITE;
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < (this.x + this.width) &&
                mouseY >= this.y && mouseY < (this.y + this.height);
    }
}