package dev.revere.valance.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Mouse;

/**
 * @author Remi
 * @project valance
 * @date 5/5/2025
 */
@Getter
@Setter
public class ScrollUtil {
    public float scroll;
    public float scrollY;
    public float sensibility;
    public float maxHeight;

    public float getScroll(float sensibility, float maxHeight, boolean isInBounds) {
        float offset = isInBounds ? (float) (-Mouse.getDWheel() * (sensibility * 0.01)) : 0;
        if (this.maxHeight != maxHeight) {
            this.maxHeight = maxHeight;
        }
        if (offset != 0) {
            scroll += offset;
        }
        scroll = Math.min((maxHeight), scroll);
        scroll = Math.max(0.0F, scroll);

        return scroll;
    }

    public float getScrollY(float scrollY) {
        if (this.scrollY != scrollY) {
            this.scrollY = scrollY;
        }
        return scrollY;
    }

    public void drawScroll(float x, float maxHeight, float width, float height, int mouseX, int mouseY, int color) {
        Gui.drawRect(x, (getScrollY() + ((maxHeight - height + 5) / getMaxHeight()) * getScroll()), x + width, (getScrollY() + ((maxHeight - height + 5) / getMaxHeight()) * getScroll()) + (height), -1);
    }

    public void drawScroll(float x, float maxHeight, float width, float height, float elements, int mouseX, int mouseY, int color) {
        Gui.drawRect(x, (getScrollY() + ((maxHeight - (height - elements) + 5) / getMaxHeight()) * getScroll()), x + width, (getScrollY() + ((maxHeight - (height - elements) + 5) / getMaxHeight()) * getScroll()) + ((height - elements)), -1);
    }
}