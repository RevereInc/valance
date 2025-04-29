package dev.revere.valance.util;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.service.ISkijaService;
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

    public static void drawText(String text, float x, float y, int color, float fontSize) {
        getCanvas().ifPresent(canvas -> {
            getFontCollection().ifPresent(fontCollection -> {
                try (TextStyle style = new TextStyle().setColor(color).setFontSize(fontSize);
                     ParagraphStyle pStyle = new ParagraphStyle();
                     ParagraphBuilder builder = new ParagraphBuilder(pStyle, fontCollection)) {

                    builder.pushStyle(style);
                    builder.addText(text);
                    builder.popStyle();

                    try (Paragraph paragraph = builder.build()) {
                        paragraph.layout(Float.POSITIVE_INFINITY);
                        paragraph.paint(canvas, x, y);
                    }
                } catch(Exception e) {
                    System.err.println("[" + ClientLoader.CLIENT_NAME + ":SkijaUtil] Error drawing text: " + e.getMessage());
                }
            });
        });
    }

    public static float getTextWidth(String text, float fontSize) {
        Optional<FontCollection> fontCollectionOpt = getFontCollection();
        if (fontCollectionOpt.isPresent()) {
            FontCollection fontCollection = fontCollectionOpt.get();
            try (TextStyle style = new TextStyle().setFontSize(fontSize);
                 ParagraphStyle pStyle = new ParagraphStyle();
                 ParagraphBuilder builder = new ParagraphBuilder(pStyle, fontCollection)) {

                builder.pushStyle(style);
                builder.addText(text);
                builder.popStyle();

                try (Paragraph paragraph = builder.build()) {
                    paragraph.layout(Float.POSITIVE_INFINITY);
                    return paragraph.getMaxWidth();
                }
            } catch (Exception e) {
                System.err.println("[" + ClientLoader.CLIENT_NAME + ":SkijaUtil] Error getting text width: " + e.getMessage());
            }
        }
        return MinecraftUtil.mc().fontRendererObj.getStringWidth(text);
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
