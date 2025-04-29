package dev.revere.valance.ui.test;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.util.SkijaRenderUtil;
import io.github.humbleui.skija.*;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Optional;

public class EmptyTestScreen extends GuiScreen {

    private ISkijaService skijaService;

    /**
     * Called when the GUI is displayed or resized.
     */
    @Override
    public void initGui() {
        Optional<ISkijaService> skijaOpt = ClientLoader.getService(ISkijaService.class);
        this.skijaService = skijaOpt.get();
        System.out.println("[EmptyTestScreen] Initializing...");
        super.initGui();
    }

    /**
     * Draws the screen content.
     * @param mouseX Current mouse X position.
     * @param mouseY Current mouse Y position.
     * @param partialTicks Render partial ticks.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (skijaService == null) {
            System.out.println("abc");
        }
        if (skijaService != null && skijaService.isInitialized()) {
            System.out.println("[EmptyTestScreen] Running Skija frame...");

            skijaService.runFrame(() -> {
                try {
                    Canvas canvas = skijaService.getCanvas();
                    if (canvas != null) {
                        float rectWidth = 200;
                        float rectHeight = 100;
                        float rectX = (this.skijaService.getSurface().getWidth() / 2f) - (rectWidth / 2f);
                        float rectY = (this.skijaService.getSurface().getHeight() / 2f) - (rectHeight / 2f);
                        float cornerRadius = 4;

                        int rectColor = 0xFF00FF00;
                        int outlineColor = 0xFFFF0000;
                        float outlineWidth = 2.0f;

                        SkijaRenderUtil.drawRoundRect(rectX, rectY, rectWidth, rectHeight, cornerRadius, rectColor);
                        SkijaRenderUtil.drawOutlineRoundRect(rectX, rectY, rectWidth, rectHeight, cornerRadius, outlineWidth, outlineColor);

                        String text = "Skija Test Rect";
                        float fontSize = 24.0f;
                        float textWidth = SkijaRenderUtil.getTextWidth(text, fontSize);
                        float textX = (this.width / 2f) - (textWidth / 2f);
                        float textY = rectY + rectHeight + 20;
                        SkijaRenderUtil.drawText(text, textX, textY, fontSize, 0xFFFFFFFF);

                        String debugText = "Skija Initialized - Width: " + this.width + ", Height: " + this.height;
                        SkijaRenderUtil.drawText(debugText, 10, 10, 12.0f, 0xFFFFFFFF);
                    } else {
                        System.err.println("[EmptyTestScreen] Canvas is null in drawScreen.");
                    }
                } catch (Exception e) {
                    System.err.println("[EmptyTestScreen] Error during Skija render: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            System.out.println("[EmptyTestScreen] Skija frame completed.");
        } else {
            String text = "Skija Service Not Ready!";
            int textColor = 0xFFFF5555; // Red
            this.fontRendererObj.drawStringWithShadow(
                    text,
                    (this.width / 2f) - (this.fontRendererObj.getStringWidth(text) / 2f),
                    (this.height / 2f) - (this.fontRendererObj.FONT_HEIGHT / 2f),
                    textColor
            );

            // Add more debug info
            this.fontRendererObj.drawStringWithShadow(
                    "Service null: " + (skijaService == null) + ", Width: " + this.width + ", Height: " + this.height,
                    10,
                    10,
                    0xFFFFFFFF
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Called when a key is typed.
     * @param typedChar The character typed.
     * @param keyCode The LWJGL key code.
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }

        // Add debug key to force reinitialize
        if (keyCode == Keyboard.KEY_F5) {
            System.out.println("[EmptyTestScreen] Forcing SkijaService reinit...");
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    /**
     * Determines if the GUI should pause the game in singleplayer.
     * @return false to keep the game running.
     */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Called when the GUI is closed.
     */
    @Override
    public void onGuiClosed() {
        System.out.println("[EmptyTestScreen] Closed.");
        super.onGuiClosed();
    }
}