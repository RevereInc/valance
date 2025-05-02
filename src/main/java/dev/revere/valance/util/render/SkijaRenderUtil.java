package dev.revere.valance.util.render;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.util.ColorUtil;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.paragraph.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.Optional;

public final class SkijaRenderUtil {

    private SkijaRenderUtil() {}

    /**
     * Internal helper to safely get the current Skija canvas from the service.
     * @return Optional containing the Canvas if available, empty otherwise.
     */
    public static Optional<Canvas> getCanvas() {
        return ClientLoader.getService(ISkijaService.class)
                .filter(ISkijaService::isInitialized)
                .map(ISkijaService::getCanvas);
    }

    /**
     * Internal helper to safely get the current FontCollection from the service.
     * @return Optional containing the FontCollection if available, empty otherwise.
     */
    public static Optional<FontCollection> getFontCollection() {
        return ClientLoader.getService(ISkijaService.class)
                .filter(ISkijaService::isInitialized)
                .map(ISkijaService::getFontCollection);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setMode(PaintMode.FILL)) {
                canvas.drawRect(Rect.makeXYWH(x, y, width, height), paint);
            }
        });
    }

    public static void drawRoundRect(float x, float y, float width, float height, float radius, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.FILL)) {
                canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
            }
        });
    }

    public static void drawOutlineRect(float x, float y, float width, float height, float strokeWidth, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth)) {
                float halfStroke = strokeWidth / 2f;
                canvas.drawRect(Rect.makeXYWH(x + halfStroke, y + halfStroke, width - strokeWidth, height - strokeWidth), paint);
            }
        });

    }

    public static void drawOutlineRoundRect(float x, float y, float width, float height, float radius, float strokeWidth, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth)) {
                float halfStroke = strokeWidth / 2f;
                canvas.drawRRect(RRect.makeXYWH(x + halfStroke, y + halfStroke, width - strokeWidth, height - strokeWidth, Math.max(0f, radius - halfStroke)), paint);
            }
        });
    }

    public static void drawText(String text, float x, float y, float size, int color) {
        getCanvas().ifPresent(canvas -> {
            Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.FILL);
            Font font = new Font(Typeface.makeDefault(), size);
            canvas.drawString(text, x, y, font, paint);
        });
    }

    /**
     * Applies standard opacity (0-255) to a color integer.
     * @param color The ARGB color integer.
     * @param opacity The opacity value (0-255).
     * @return The new ARGB color integer with the specified opacity.
     */
    public static int applyOpacity(int color, int opacity) {
        opacity = Math.max(0, Math.min(255, opacity));
        return (color & 0x00FFFFFF) | (opacity << 24);
    }

    /**
     * Helper to draw a single colored segment with optional effects.
     */
    private static void drawSegment(Canvas canvas, Font font, String segment, float x, float y, Paint textPaint, Paint shadowPaint, Paint glowPaint) {
        if (glowPaint != null) {
            glowPaint.setColor(textPaint.getColor());
            canvas.drawString(segment, x, y, font, glowPaint);
        }
        if (shadowPaint != null) {
            canvas.drawString(segment, x + 1, y + 1f, font, shadowPaint);
        }
        canvas.drawString(segment, x, y, font, textPaint);
    }

    /**
     * Draws a string potentially containing Minecraft § color codes with Skija.
     * NOTE: This basic version only handles colors, not formatting like bold/italic.
     * Effects (shadow, glow) apply to the entire string based on initial flags.
     *
     * @param canvas The Skija canvas to draw on (obtained via getCanvas() or passed).
     * @param font   The font to use.
     * @param text   The string containing potential color codes.
     * @param x      Initial X position (pixels).
     * @param y      Baseline Y position (pixels).
     * @param defaultColor Default text color (ARGB integer).
     * @param shadow Whether to apply a simple shadow.
     * @param glow   Whether to apply a simple glow.
     * @param shadowOpacity Opacity for the shadow (0-255).
     */
    public static void drawColoredString(Canvas canvas, Font font, String text, float x, float y, int defaultColor, boolean shadow, boolean glow, int shadowOpacity) {
        if (text == null || text.isEmpty() || canvas == null || font == null) {
            return;
        }

        float currentX = x;
        int currentColor = defaultColor;

        try (Paint textPaint = new Paint().setColor(currentColor).setAntiAlias(true);
             Paint shadowPaint = shadow ? new Paint().setColor(applyOpacity(0x000000, shadowOpacity)).setAntiAlias(true) : null; // Black shadow base
             Paint glowPaint = glow ? new Paint().setColor(currentColor).setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.NORMAL, 2.0f, false)).setAntiAlias(true) : null
        ) {

            StringBuilder currentSegment = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == ColorUtil.SECTION_SIGN && i + 1 < text.length()) {
                    char formatCode = text.charAt(i + 1);
                    int newColor = ColorUtil.getColor(formatCode, -1);

                    if (newColor != -1) {
                        if (currentSegment.length() > 0) {
                            String segment = currentSegment.toString();
                            drawSegment(canvas, font, segment, currentX, y, textPaint, shadowPaint, glowPaint);
                            currentX += font.measureTextWidth(segment);
                            currentSegment.setLength(0);
                        }
                        currentColor = newColor;
                        textPaint.setColor(currentColor);
                        i++;
                        continue;
                    } else if (ColorUtil.isResetCode(formatCode)) {
                        if (currentSegment.length() > 0) {
                            String segment = currentSegment.toString();
                            drawSegment(canvas, font, segment, currentX, y, textPaint, shadowPaint, glowPaint);
                            currentX += font.measureTextWidth(segment);
                            currentSegment.setLength(0);
                        }
                        currentColor = ColorUtil.getDefaultColor();
                        textPaint.setColor(currentColor);

                        i++;
                        continue;
                    } else if (ColorUtil.isFormatCode(formatCode)) {
                        i++;
                        continue;
                    }
                }
                currentSegment.append(c);
            }

            // Draw final segment
            if (currentSegment.length() > 0) {
                drawSegment(canvas, font, currentSegment.toString(), currentX, y, textPaint, shadowPaint, glowPaint);
            }
        } // Auto-close paints
    }

    /**
     * Measures the rendered width of a string containing Minecraft color codes, ignoring the codes themselves.
     *
     * @param font The font used for rendering.
     * @param text The string potentially containing color codes.
     * @return The width in pixels that the string will occupy when drawn by drawColoredString.
     */
    public static float measureColoredStringWidth(Font font, String text) {
        if (text == null || text.isEmpty() || font == null) {
            return 0f;
        }

        float totalWidth = 0f;
        StringBuilder currentSegment = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ColorUtil.SECTION_SIGN && i + 1 < text.length()) {
                char formatCode = text.charAt(i + 1);

                if (ColorUtil.isColorCode(formatCode) || ColorUtil.isResetCode(formatCode) || ColorUtil.isFormatCode(formatCode)) {
                    if (currentSegment.length() > 0) {
                        totalWidth += font.measureTextWidth(currentSegment.toString());
                        currentSegment.setLength(0);
                    }
                    i++;
                    continue;
                }
            }
            currentSegment.append(c);
        }

        if (currentSegment.length() > 0) {
            totalWidth += font.measureTextWidth(currentSegment.toString());
        }

        return totalWidth;
    }

    public static float getTextWidth(String text, float size) {
        Font font = new Font(Typeface.makeDefault(), size);
        return font.measureText(text).getWidth();
    }

    public static void drawGradientRect(float x, float y, float width, float height, int colorStart, int colorEnd, boolean horizontal) {
        getCanvas().ifPresent(canvas -> {
            Point startPoint = new Point(x, y);
            Point endPoint = horizontal ? new Point(x + width, y) : new Point(x, y + height);
            try (Paint paint = new Paint().setShader(Shader.makeLinearGradient(startPoint, endPoint, new int[]{colorStart, colorEnd}))) {
                canvas.drawRect(Rect.makeXYWH(x, y, width, height), paint);
            }
        });
    }

    public static void drawCircle(float cx, float cy, float radius, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.FILL)) {
                canvas.drawCircle(cx, cy, radius, paint);
            }
        });
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float strokeWidth, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth)) {
                canvas.drawLine(x1, y1, x2, y2, paint);
            }
        });
    }

    public static void drawCheckmark(float x, float y, float size, float strokeWidth, int color) {
        getCanvas().ifPresent(canvas -> {
            try (Paint paint = new Paint().setColor(color).setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth).setStrokeCap(PaintStrokeCap.ROUND)) {
                float pad = size * 0.2f;
                float x1 = x + pad;
                float y1 = y + size * 0.5f;
                float x2 = x + size * 0.4f;
                float y2 = y + size - pad;
                float x3 = x + size - pad;
                float y3 = y + pad;
                canvas.drawLine(x1, y1, x2, y2, paint);
                canvas.drawLine(x2, y2, x3, y3, paint);
            }
        });
    }

    public static void applyScissor(float x, float y, float width, float height) {
        getCanvas().ifPresent(canvas -> {
            canvas.save();
            canvas.clipRect(Rect.makeXYWH(x, y, width, height));
        });
    }

    public static void restoreScissor() {
        getCanvas().ifPresent(Canvas::restore);
    }
}
