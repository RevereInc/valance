package dev.revere.valance.module.impl.render;

import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.render.Render2DEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.ISkijaService;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@ModuleInfo(name = "Hotbar", displayName = "Hotbar", description = "Customizes the hotbar.", category = Category.RENDER)
public class HotbarModule extends AbstractModule {
    private final ISkijaService skijaService;

    private final int backgroundColor = 0x77101015;
    private final int selectionColor = 0x99303035;
    private final float cornerRadius = 4f;

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     * @param skijaService The injected Skija service instance.
     */
    public HotbarModule(IEventBusService eventBusService, ISkijaService skijaService) {
        super(eventBusService);
        this.skijaService = skijaService;
    }

    @Subscribe
    private void onRender(Render2DEvent event) {
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer) || skijaService == null || !skijaService.isInitialized()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float scaleFactor = sr.getScaleFactor();

        float hotbarWidthScaled = 182f;
        float hotbarHeightScaled = 22f;
        float slotSizeScaled = 20f;
        float selectionBoxWidthScaled = 22f;

        float scaledWidth = sr.getScaledWidth();
        float scaledHeight = sr.getScaledHeight();

        float hotbarX = (scaledWidth / 2.0F - hotbarWidthScaled / 2.0f) * scaleFactor;
        float hotbarY = (scaledHeight - hotbarHeightScaled) * scaleFactor - 10;
        float hotbarW = hotbarWidthScaled * scaleFactor;
        float hotbarH = hotbarHeightScaled * scaleFactor;

        int currentItem = mc.thePlayer.inventory.currentItem;
        float selectionX = (scaledWidth / 2.0F - 91f + currentItem * slotSizeScaled) * scaleFactor;
        float selectionW = selectionBoxWidthScaled * scaleFactor;

        boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        if (!wasBlendEnabled) {
            GlStateManager.enableBlend();
        }
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);


        skijaService.runFrame(() -> {
            Canvas canvas = skijaService.getCanvas();
            if (canvas == null) return;

            try (Paint backgroundSharpPaint = new Paint().setColor(backgroundColor).setAntiAlias(true);
                 Paint selectionPaint = new Paint().setColor(selectionColor).setAntiAlias(true)) {
                RRect backgroundRRect = RRect.makeXYWH(hotbarX, hotbarY, hotbarW, hotbarH, cornerRadius);

                try (Paint blurPaint = new Paint()) {
                    try (ImageFilter blurFilter = ImageFilter.makeBlur(7, 7, FilterTileMode.CLAMP)) {
                        blurPaint.setImageFilter(blurFilter);
                    }
                    Rect totalBounds = Rect.makeXYWH(hotbarX, hotbarY, hotbarW, hotbarH);
                    Rect layerBounds = totalBounds.inflate(7 * 2);
                    canvas.saveLayer(layerBounds, blurPaint);

                    try (Paint blurShapePaint = new Paint().setColor(backgroundColor).setAntiAlias(true)) {
                        canvas.drawRRect(backgroundRRect, blurShapePaint);
                    }
                    canvas.restore();
                }

                canvas.drawRRect(backgroundRRect, backgroundSharpPaint);
                RRect selectionRRect = RRect.makeXYWH(selectionX, hotbarY, selectionW, hotbarH, cornerRadius);
                canvas.drawRRect(selectionRRect, selectionPaint);

            }
        });

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        RenderHelper.enableGUIStandardItemLighting();

        for (int index = 0; index < 9; ++index) {
            int itemX_vanillaPos = sr.getScaledWidth() / 2 - 90 + index * 20 + 3;
            int itemY_vanillaPos = sr.getScaledHeight() - 16 - 3 - 5;
            mc.ingameGUI.renderHotbarItem(index, itemX_vanillaPos, itemY_vanillaPos, event.getPartialTicks(), mc.thePlayer);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }
}