package dev.revere.valance.ui.draggable;

import dev.revere.valance.module.api.IModule;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Draggable {

    private final IModule module;
    private final String name;

    // --- Internal State (PIXEL coordinates/dimensions for the ENTIRE element) ---
    private float internalX;
    private float internalY;
    @Setter
    private float contentWidth;
    @Setter
    private float contentHeight;

    // --- Style Properties (PIXEL values) ---
    private final float headerHeight;
    private final float cornerRadius;
    private final int headerColor;
    private final int contentColor;
    private final int textColor;

    // --- Dragging State (PIXEL values) ---
    private boolean dragging;
    private float startX;
    private float startY;

    /**
     * Constructor for the Draggable.
     * initialX/Y are the desired initial PIXEL position of the CONTENT area's top-left.
     */
    public Draggable(IModule module, String name, float initialContentX, float initialContentY) {
        this.module = module;
        this.name = name;

        int defaultScaleFactor = 2;
        this.headerHeight = 18f * defaultScaleFactor;
        this.cornerRadius = 8f * defaultScaleFactor;
        this.headerColor = 0xAA1C1C1E;
        this.contentColor = 0xAA1C1C1E;
        this.textColor = 0xFFE5E5E5;

        this.internalX = initialContentX;
        this.internalY = initialContentY - this.headerHeight;

        this.contentWidth = 150f * defaultScaleFactor;
        this.contentHeight = (100f * defaultScaleFactor) - this.headerHeight;
    }

    /**
     * @return Pixel X coordinate of the CONTENT area's top-left.
     */
    public float getX() {
        return this.internalX;
    }

    /**
     * @return Pixel Y coordinate of the CONTENT area's top-left.
     */
    public float getY() {
        return this.internalY + this.headerHeight;
    }

    /**
     * Sets the CONTENT area's top-left X position (adjusts internalX).
     */
    public void setX(float contentX) {
        this.internalX = contentX;
    }

    /**
     * Sets the CONTENT area's top-left Y position (adjusts internalY).
     */
    public void setY(float contentY) {
        this.internalY = contentY - this.headerHeight;
    }

    /**
     * @return Pixel width of the CONTENT area.
     */
    public float getWidth() {
        return this.contentWidth;
    }

    /**
     * @return Pixel height of the CONTENT area.
     */
    public float getHeight() {
        return this.contentHeight;
    }

    /**
     * @return Total pixel width of the element (header width = content width).
     */
    public float getTotalWidth() {
        return this.contentWidth;
    }

    /**
     * @return Total pixel height of the element (header + content).
     */
    public float getTotalHeight() {
        return this.headerHeight + this.contentHeight;
    }

    /**
     * Updates internal position during dragging. Receives PIXEL mouse coordinates.
     */
    public final void draw(float mouseX_pixels, float mouseY_pixels) {
        if (!this.module.isEnabled()) return;
        if (dragging) {
            internalX = (mouseX_pixels - startX);
            internalY = (mouseY_pixels - startY);
        }
    }

    /**
     * Handles the click event. Receives PIXEL mouse coordinates. Uses TOTAL bounds.
     */
    public final void onClick(float mouseX_pixels, float mouseY_pixels, int button) {
        if (!this.module.isEnabled()) return;
        if (hovered(mouseX_pixels, mouseY_pixels) && button == 0 && !dragging) {
            dragging = true;
            startX = (mouseX_pixels - internalX);
            startY = (mouseY_pixels - internalY);
        }
    }

    /**
     * Handles the release event.
     */
    public final void onRelease(int button) {
        if (!this.module.isEnabled()) return;
        if (button == 0 && dragging) {
            dragging = false;
        }
    }

    /**
     * Checks if the PIXEL mouse coordinates are hovering over the TOTAL bounds.
     */
    public boolean hovered(float mouseX_pixels, float mouseY_pixels) {
        return mouseX_pixels >= internalX && mouseX_pixels <= internalX + getTotalWidth() && mouseY_pixels >= internalY && mouseY_pixels <= internalY + getTotalHeight();
    }

    /**
     * Draws the entire draggable frame (header + content) with optional blur
     * that respects the internal seam. Call from module's Render2DEvent handler.
     *
     * @param canvas     The Skija canvas.
     * @param blur       Whether to apply blur to outer edges.
     * @param blurSigmaX Pixel blur strength X.
     * @param blurSigmaY Pixel blur strength Y.
     * @param opacity    Content background opacity (0-255).
     */
    public void drawFrame(Canvas canvas, boolean blur, float blurSigmaX, float blurSigmaY, int opacity) {
        float pixX = this.internalX;
        float pixY = this.internalY;
        float pixTotalWidth = this.getTotalWidth();
        float pixHeaderHeight = this.headerHeight;
        float pixContentHeight = this.contentHeight;
        float pixTotalHeight = this.getTotalHeight();
        float pixCornerRadius = this.cornerRadius;

        if (pixTotalWidth <= 0 || pixTotalHeight <= 0) return;

        // --- 1. Define the Combined Shape Path ---
        try (Path combinedPath = new Path()) {
            combinedPath.addRRect(RRect.makeXYWH(pixX, pixY, pixTotalWidth, pixHeaderHeight, pixCornerRadius, pixCornerRadius, 0, 0));
            float contentY = pixY + pixHeaderHeight;
            if (pixContentHeight > 0) {
                combinedPath.addRRect(RRect.makeComplexXYWH(pixX, contentY, pixTotalWidth, pixContentHeight, new float[]{0, 0, 0, 0, pixCornerRadius, pixCornerRadius, pixCornerRadius, pixCornerRadius}));
            }

            // --- 2. Draw Blurred Background ---
            if (blur && blurSigmaX > 0 && blurSigmaY > 0) {
                try (Paint blurPaint = new Paint()) {
                    try (ImageFilter blurFilter = ImageFilter.makeBlur(blurSigmaX, blurSigmaY, FilterTileMode.CLAMP)) {
                        blurPaint.setImageFilter(blurFilter);
                    }
                    Rect totalBounds = Rect.makeXYWH(pixX, pixY, pixTotalWidth, pixTotalHeight);
                    Rect layerBounds = totalBounds.inflate(blurSigmaX * 2);
                    canvas.saveLayer(layerBounds, blurPaint);

                    try (Paint blurShapePaint = new Paint().setColor(this.contentColor).setAntiAlias(true)) {
                        canvas.drawPath(combinedPath, blurShapePaint);
                    }
                    canvas.restore();
                }
            }

            // --- 3. Draw Sharp Foreground Shapes ---
            int finalContentColor = (contentColor & 0x00FFFFFF) | ((opacity & 0xFF) << 24);

            canvas.save();
            canvas.clipRect(Rect.makeXYWH(pixX, pixY, pixTotalWidth, pixHeaderHeight));
            try (Paint headerSharpPaint = new Paint().setColor(this.headerColor).setAntiAlias(true)) {
                canvas.drawPath(combinedPath, headerSharpPaint);
            }
            canvas.restore();

            if (pixContentHeight > 0) {
                canvas.save();
                canvas.clipRect(Rect.makeXYWH(pixX, contentY, pixTotalWidth, pixContentHeight));
                try (Paint contentSharpPaint = new Paint().setColor(finalContentColor).setAntiAlias(true)) {
                    canvas.drawPath(combinedPath, contentSharpPaint);
                }
                canvas.restore();
            }

            // --- 4. Draw Header Text ---
            try (Paint textPaint = new Paint().setColor(this.textColor).setAntiAlias(true)) {
                int baseHeaderFontSize = 24;
                try (Typeface boldTypeface = Typeface.makeFromName("Sans Serif", FontStyle.BOLD)) {
                    Font font = new Font(boldTypeface, baseHeaderFontSize);
                    float textWidthPixels = font.measureText(this.name).getWidth();
                    float textX = pixX + (pixTotalWidth - textWidthPixels) / 2f;
                    FontMetrics fm = font.getMetrics();
                    float textCenterY = pixY + pixHeaderHeight / 2f;
                    float baselineOffsetY = (fm.getAscent() + fm.getDescent()) / 2f;
                    float textY = textCenterY - baselineOffsetY;
                    canvas.drawString(this.name, textX, textY, font, textPaint);
                } catch (Exception e) {
                    System.err.println("Error drawing header text: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Error drawing header text: " + e.getMessage());
            }
        }
    }
}