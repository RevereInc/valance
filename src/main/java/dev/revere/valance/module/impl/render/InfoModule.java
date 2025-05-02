package dev.revere.valance.module.impl.render;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.game.ClientTickEvent;
import dev.revere.valance.event.type.render.Render2DEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IDraggableService;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.ui.draggable.Draggable;
import io.github.humbleui.skija.*;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import static dev.revere.valance.util.MinecraftUtil.getPlayer;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@ModuleInfo(name = "InfoDisplay", displayName = "Info Display", description = "Shows client/game info on screen.", category = Category.RENDER)
public class InfoModule extends AbstractModule {
    private final IModuleManager moduleManager;
    private final ISkijaService skijaService;
    private final Draggable draggable;

    private String displayVersion = "v" + ClientLoader.CLIENT_VERSION;
    private String displayPlayerName = "N/A";
    private String displayServer = "Singleplayer";
    private String displayFps = "0 FPS";
    private String displayModules = "0 / 0";

    private int tickCounter = 0;
    private final int INFO_INTERVAL = 20 * 5;

    public InfoModule(IEventBusService eventBusService, IModuleManager moduleManager, ISkijaService skijaService, IDraggableService draggableService) {
        super(eventBusService);
        this.skijaService = skijaService;
        this.moduleManager = moduleManager;

        this.draggable = new Draggable(this, "Info", 10, 100);
        draggableService.register(this.draggable);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tickCounter = 0;
        updateInfo();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onRender(Render2DEvent event) {
        float cardPixelWidth = 310f;
        float cardPixelHeight = 215f;
        draggable.setContentWidth(cardPixelWidth);
        draggable.setContentHeight(cardPixelHeight);

        skijaService.runFrame(() -> {
            Canvas canvas = skijaService.getCanvas();
            if (canvas == null) return;

            float pixX = draggable.getX();
            float pixY = draggable.getY();

            boolean blur = true;
            int opacity = 235;
            float blurX = 7f;
            float blurY = 7f;

            draggable.drawFrame(canvas, blur, blurX, blurY, opacity);

            float paddingPixels = 20f;
            float fontSizePixels = 13f;
            float lineHeightPixels = fontSizePixels + 4f;

            float contentX = pixX + paddingPixels;
            float contentY = pixY + paddingPixels;

            try (Font font = new Font(Typeface.makeDefault(), fontSizePixels);
                 Paint textPaint = new Paint().setColor(java.awt.Color.WHITE.getRGB()).setAntiAlias(true)) {

                float textBaselineOffset = font.getMetrics().getAscent() * -1;
                float currentTextY = contentY;

                canvas.drawString(ClientLoader.CLIENT_NAME + " " + displayVersion, contentX, currentTextY + textBaselineOffset, font, textPaint);
                currentTextY += lineHeightPixels;
                canvas.drawString("Player: " + displayPlayerName, contentX, currentTextY + textBaselineOffset, font, textPaint);
                currentTextY += lineHeightPixels;
                canvas.drawString("Server: " + displayServer, contentX, currentTextY + textBaselineOffset, font, textPaint);
                currentTextY += lineHeightPixels;
                canvas.drawString("FPS: " + displayFps, contentX, currentTextY + textBaselineOffset, font, textPaint);
                currentTextY += lineHeightPixels;
                canvas.drawString("Modules: " + displayModules, contentX, currentTextY + textBaselineOffset, font, textPaint);
            }
        });
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (!isEnabled()) return;

        tickCounter++;
        if (tickCounter >= INFO_INTERVAL) {
            tickCounter = 0;
            updateInfo();
        }
    }

    private void updateInfo() {
        long enabledCount = moduleManager.getModules().stream().filter(IModule::isEnabled).count();
        long totalCount = moduleManager.getModules().size();

        this.displayPlayerName = getPlayer().map(EntityPlayer::getName).orElse("N/A");
        this.displayServer = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
        this.displayFps = Minecraft.getDebugFPS() + " FPS";
        this.displayModules = enabledCount + " / " + totalCount;
    }
}