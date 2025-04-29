package dev.revere.valance.module.impl.misc;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.game.ClientTickEvent;
import dev.revere.valance.event.type.render.Render2DEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.service.ISkijaService;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import static dev.revere.valance.util.MinecraftUtil.getPlayer;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@ModuleInfo(name = "InfoDisplay", description = "Shows client/game info on screen.", category = Category.MISC)
public class InfoModule extends AbstractModule {
    private final IModuleManager moduleManager;
    private final ISkijaService skijaService;

    private String displayVersion = "v" + ClientLoader.CLIENT_VERSION;
    private String displayPlayerName = "N/A";
    private String displayServer = "Singleplayer";
    private String displayFps = "0 FPS";
    private String displayModules = "0 / 0";

    private int tickCounter = 0;
    private final int INFO_INTERVAL = 20 * 5;

    public InfoModule(IEventBusService eventBusService, IModuleManager moduleManager, ISkijaService skijaService) {
        super(eventBusService);
        this.skijaService = skijaService;
        this.moduleManager = moduleManager;
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
        if (!isEnabled()) return;

        skijaService.runFrame(() -> {
            Canvas canvas = skijaService.getCanvas();
            if (canvas == null) return;

            float cardX = 10;
            float cardY = 10;
            float cardWidth = 180;
            float cardHeight = 85;
            float cornerRadius = 8f;
            float padding = 8f;
            float fontSize = 10f;
            float lineHeight = fontSize + 3f;

            try (Paint blurPaint = new Paint();
                 ImageFilter blurFilter = ImageFilter.makeBlur(4f, 4f, FilterTileMode.CLAMP)) {
                blurPaint.setImageFilter(blurFilter);

                canvas.saveLayer(Rect.makeXYWH(cardX - 5, cardY - 5, cardWidth + 10, cardHeight + 10), blurPaint);

                try (Paint dummyBg = new Paint().setColor(0x20000000)) {
                    canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardWidth, cardHeight, cornerRadius), dummyBg);
                }

                canvas.restore();
            }

            try (Paint cardBgPaint = new Paint().setColor(0x60FFFFFF).setAntiAlias(true)) {
                canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardWidth, cardHeight, cornerRadius), cardBgPaint);
            }

            try (Paint borderPaint = new Paint().setColor(0x90FFFFFF)
                    .setMode(PaintMode.STROKE)
                    .setStrokeWidth(1.0f)
                    .setAntiAlias(true)) {
                float halfStroke = borderPaint.getStrokeWidth() / 2f;
                canvas.drawRRect(RRect.makeXYWH(cardX + halfStroke, cardY + halfStroke, cardWidth - borderPaint.getStrokeWidth(), cardHeight - borderPaint.getStrokeWidth(), Math.max(0f, cornerRadius - halfStroke)), borderPaint);
            }

            try (Font font = new Font(Typeface.makeDefault(), fontSize);
                 Paint textPaint = new Paint().setColor(0xFFFFFFFF).setAntiAlias(true)) {

                float currentY = cardY + padding;
                float textX = cardX + padding;

                canvas.drawString(ClientLoader.CLIENT_NAME + " " + displayVersion, textX, currentY + font.getMetrics().getAscent() * -1, font, textPaint);
                currentY += lineHeight;

                canvas.drawString("Player: " + displayPlayerName, textX, currentY + font.getMetrics().getAscent() * -1, font, textPaint);
                currentY += lineHeight;

                canvas.drawString("Server: " + displayServer, textX, currentY + font.getMetrics().getAscent() * -1, font, textPaint);
                currentY += lineHeight;

                canvas.drawString("FPS: " + displayFps, textX, currentY + font.getMetrics().getAscent() * -1, font, textPaint);
                currentY += lineHeight;

                canvas.drawString("Modules: " + displayModules, textX, currentY + font.getMetrics().getAscent() * -1, font, textPaint);
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