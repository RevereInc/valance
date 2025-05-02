package dev.revere.valance.util;

import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
public class ColorUtil {
    private static final Map<Character, Integer> colorMap = new HashMap<>();
    public static final char SECTION_SIGN = '\u00A7';

    static {
        colorMap.put('0', 0xFF000000); // BLACK
        colorMap.put('1', 0xFF0000AA); // DARK_BLUE
        colorMap.put('2', 0xFF00AA00); // DARK_GREEN
        colorMap.put('3', 0xFF00AAAA); // DARK_AQUA
        colorMap.put('4', 0xFFAA0000); // DARK_RED
        colorMap.put('5', 0xFFAA00AA); // DARK_PURPLE
        colorMap.put('6', 0xFFFFAA00); // GOLD
        colorMap.put('7', 0xFFAAAAAA); // GRAY
        colorMap.put('8', 0xFF555555); // DARK_GRAY
        colorMap.put('9', 0xFF5555FF); // BLUE
        colorMap.put('a', 0xFF55FF55); // GREEN
        colorMap.put('b', 0xFF55FFFF); // AQUA
        colorMap.put('c', 0xFFFF5555); // RED
        colorMap.put('d', 0xFFFF55FF); // LIGHT_PURPLE
        colorMap.put('e', 0xFFFFFF55); // YELLOW
        colorMap.put('f', 0xFFFFFFFF); // WHITE
    }

    /**
     * Gets the ARGB color for a Minecraft formatting code character.
     *
     * @param formattingCode The character code (e.g., '7', 'a', 'c'). Case-insensitive.
     * @param defaultColor   The ARGB color to return if the code is not a valid color.
     * @return The ARGB color integer.
     */
    public static int getColor(char formattingCode, int defaultColor) {
        return colorMap.getOrDefault(Character.toLowerCase(formattingCode), defaultColor);
    }

    /**
     * Gets the default color (White).
     * @return ARGB color for white.
     */
    public static int getDefaultColor() {
        return colorMap.getOrDefault('f', 0xFFFFFFFF);
    }

    /**
     * Checks if a character is a known Minecraft color code.
     * @param code The character code.
     * @return True if it's a color code (0-9, a-f).
     */
    public static boolean isColorCode(char code) {
        return colorMap.containsKey(Character.toLowerCase(code));
    }

    /**
     * Checks if a character is the reset code ('r').
     * @param code The character code.
     * @return True if it's the reset code.
     */
    public static boolean isResetCode(char code) {
        return Character.toLowerCase(code) == 'r';
    }

    /**
     * Checks if a character is any known formatting code (color, bold, etc., excluding 'r').
     * @param code The character code.
     * @return True if it's a formatting code like k, l, m, n, o.
     */
    public static boolean isFormatCode(char code) {
        char lowerCode = Character.toLowerCase(code);
        return lowerCode == 'k' || lowerCode == 'l' || lowerCode == 'm' || lowerCode == 'n' || lowerCode == 'o';
    }

    public static Color fadeBetween(int speed, int index, Color start, Color end) {
        speed = Math.max(1, speed);
        long timeMillis = System.currentTimeMillis();
        int tick = (int) (((timeMillis / speed) + index) % 360L);
        tick = (tick >= 180 ? 360 - tick : tick);
        return interpolateColorC(start, end, tick / 180f);
    }

    public static int rainbow(int delay) {
        double rainbowState = Math.ceil((System.currentTimeMillis() + delay) / 10.0);
        rainbowState %= 360.0;
        return Color.HSBtoRGB((float) (rainbowState / 360.0f), 1.0f, 1.0f);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end) {
        speed = Math.max(1, speed);
        int angle = (int) ((System.currentTimeMillis() / (long) speed + (long) index) % 360L);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(start, end, (float) angle / 360.0f);
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount), interpolateInt(color1.getGreen(), color2.getGreen(), amount), interpolateInt(color1.getBlue(), color2.getBlue(), amount), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

}
