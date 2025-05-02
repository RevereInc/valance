package dev.revere.valance.module.impl.render;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.render.Render2DEvent;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.settings.type.BooleanSetting;
import dev.revere.valance.settings.type.ColorSetting;
import dev.revere.valance.settings.type.EnumSetting;
import dev.revere.valance.settings.type.NumberSetting;
import dev.revere.valance.util.ColorUtil;
import dev.revere.valance.util.render.SkijaRenderUtil;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@ModuleInfo(name = "HUD", displayName = "HUD", description = "Displays information on your screen", category = dev.revere.valance.module.Category.RENDER)
public class HUDModule extends AbstractModule {
    private final ISkijaService skijaService;
    private final IModuleManager moduleManager;

    public final Setting<Boolean> arraylist = new BooleanSetting("ArrayList", true, "Render the module list?");
    public final Setting<Boolean> lowercase = new BooleanSetting("Lowercase", false, "Force ArrayList text lowercase?");
    public final Setting<Boolean> watermark = new BooleanSetting("Watermark", true, "Render the watermark?");
    public final Setting<Boolean> background = new BooleanSetting("Background", false, "Render backgrounds for elements?");
    public final Setting<Boolean> shaderBg = new BooleanSetting("BackgroundBlur", false, "Apply blur effect to backgrounds?");
    public final Setting<Boolean> textGlow = new BooleanSetting("TextGlow", true, "Apply subtle glow to text?");
    public final Setting<Boolean> fps = new BooleanSetting("FPS", true, "Render FPS counter?");
    public final Setting<Boolean> bps = new BooleanSetting("BPS", true, "Render BPS counter?");
    public final Setting<Boolean> textShadow = new BooleanSetting("TextShadow", true, "Apply shadow to text?");
    public final Setting<BackgroundMode> backgroundMode = new EnumSetting<>("BackgroundMode", BackgroundMode.CUSTOM, "The background mode to use for the Array List", () -> true);
    public final Setting<WatermarkMode> watermarkMode = new EnumSetting<>("WatermarkType", WatermarkMode.CSGO, "The watermark mode to use for the HUD", () -> true);
    public final Setting<ColorMode> colorMode = new EnumSetting<>("ColorMode", ColorMode.CLIENT, "Color theme for HUD elements", () -> true);
    public final Setting<FontType> fontType = new EnumSetting<>("Font", FontType.DEFAULT, "Font type for the HUD", () -> true);
    public final Setting<OutlineMode> arrayListOutline = new EnumSetting<>("Outline", OutlineMode.TOP, "How to outline the arraylist", () -> true);
    public final Setting<SortingSetting> sortingSetting = new EnumSetting<>("Sorting", SortingSetting.LENGTH, "How to sort the arraylist", () -> true);
    public final Setting<MetaData> arrayListMetaData = new EnumSetting<>("Metadata", MetaData.SIMPLE, "How to draw the module metadata", () -> true);
    public final Setting<Integer> customColor1 = new ColorSetting("FirstColor", new Color(0xFF1B1B).getRGB(), true, "Custom color for static or fade", () -> true);
    public final Setting<Integer> customColor2 = new ColorSetting("SecondColor", new Color(0xFFFFFF).getRGB(), true, "Second custom color for fade", () -> true);
    private final Setting<Integer> opacity = new NumberSetting<>("Opacity", 100, 0, 255, 1);
    public final Setting<Float> rainbowSpeed = new NumberSetting<>("RainbowSpeed", 12f, 1f, 20f, 1f);
    public final Setting<Float> fadeSpeed = new NumberSetting<>("FadeSpeed", 10.0f, 1f, 20f, 0.1f);
    public final Setting<Float> colorSpacing = new NumberSetting<>("ColorSpacing", 1.0f, 1f, 10f, 0.1f);
    public final Setting<Float> elementHeight = new NumberSetting<>("ArrayListHeight", 1f, 1f, 25f, 0.5f);
    public final Setting<Float> fontSize = new NumberSetting<>("FontSize", 20f, 8f, 40f, 1f);
    public final Setting<Float> arraylistXPos = new NumberSetting<>("ArrayListX", 6.5f, 0f, 1000f, 1f);
    public final Setting<Float> arraylistYPos = new NumberSetting<>("ArrayListY", 5f, 0f, 1000f, 1f);

    private static final float TEXT_V_PADDING = 2.0f;

    // --- Fixed Positions ---
    private final float watermarkX = 3;
    private final float watermarkY = 3;

    // --- Style Settings ---
    private final float blurSigmaX = 4f;
    private final float blurSigmaY = 4f;

    // Internal state
    private float arraylistY;

    public HUDModule(IEventBusService eventBusService, ISkijaService skijaService, IModuleManager moduleManager) {
        super(eventBusService);
        this.skijaService = skijaService;
        this.moduleManager = moduleManager;
        setEnabled(true);

        lowercase.setVisibility(arraylist::getValue);
        shaderBg.setVisibility(background::getValue);
        arrayListOutline.setVisibility(arraylist::getValue);
        sortingSetting.setVisibility(arraylist::getValue);
        customColor1.setVisibility(() -> colorMode.getValue() == ColorMode.CUSTOM || colorMode.getValue() == ColorMode.STATIC);
        customColor2.setVisibility(() -> colorMode.getValue() == ColorMode.CUSTOM);
        opacity.setVisibility(background::getValue);
        rainbowSpeed.setVisibility(() -> colorMode.getValue() == ColorMode.RAINBOW || colorMode.getValue() == ColorMode.RAINBOW_PULSE);
        fadeSpeed.setVisibility(() -> colorMode.getValue() == ColorMode.CUSTOM || colorMode.getValue() == ColorMode.CLIENT || colorMode.getValue() == ColorMode.RAINBOW_PULSE);
        colorSpacing.setVisibility(() -> colorMode.getValue() == ColorMode.CUSTOM || colorMode.getValue() == ColorMode.RAINBOW || colorMode.getValue() == ColorMode.CLIENT || colorMode.getValue() == ColorMode.RAINBOW_PULSE);
        elementHeight.setVisibility(arraylist::getValue);
    }

    @Subscribe
    public void onRender(Render2DEvent event) {
        if (!isEnabled() || mc.gameSettings.showDebugInfo) return;

        skijaService.runFrame(() -> {
            Canvas canvas = skijaService.getCanvas();
            if (canvas == null) return;

            float screenWidth = mc.displayWidth;
            float screenHeight = mc.displayHeight;
            float baseFontSize = fontSize.getValue();

            try (Font font = selectSkijaFont(fontType.getValue(), baseFontSize)) {
                if (watermark.getValue()) renderWatermark(canvas, font, this);

                if (arraylist.getValue()) {
                    List<IModule> modules = getSortedEnabledModules(font);
                    renderModules(canvas, font, screenWidth, screenHeight, modules, this);
                }
            }
        });
    }

    private Font selectSkijaFont(FontType type, float size) {
        Typeface typeface = Typeface.makeDefault();
        size = Math.max(8f, size);
        return new Font(typeface, size);
    }

    private int getColor(boolean animate) {
        return generateColorForIndex(animate ? (int) this.arraylistY : 0);
    }

    private int generateColorForIndex(int index) {
        ColorMode mode = colorMode.getValue();
        int speed = fadeSpeed.getValue().intValue();
        float spacing = colorSpacing.getValue();
        Color color1 = new Color(customColor1.getValue(), true);
        Color color2 = new Color(customColor2.getValue(), true);
        switch (mode) {
            case CUSTOM:
                return ColorUtil.fadeBetween(speed, (int) (spacing * index * 10), color1, color2).getRGB();
            case STATIC:
                return customColor1.getValue();
            case CLIENT:
                return ColorUtil.fadeBetween(speed, (int) (spacing * index * 10), new Color(255, 11, 82), Color.WHITE).getRGB();
            case RAINBOW_PULSE:
                return ColorUtil.fadeBetween(speed, (int) (spacing * index * 10), new Color(ColorUtil.rainbow(1000)), Color.WHITE).getRGB();
            case RAINBOW:
                return ColorUtil.rainbow((int) (rainbowSpeed.getValue() * index * 10));
        }
        return Color.WHITE.getRGB();
    }

    private void renderWatermark(Canvas canvas, Font font, HUDModule hud) {
        String finalText = ClientLoader.CLIENT_NAME + " v" + ClientLoader.CLIENT_VERSION + " | " + (mc.getSession() != null ? mc.getSession().getUsername() : "Player") + " | " + (mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer");
        float textWidth = font.measureText(finalText).getWidth();
        FontMetrics fm = font.getMetrics();
        float actualTextHeight = -fm.getAscent() + fm.getDescent();
        float boxHeight = actualTextHeight + TEXT_V_PADDING * 2 + 4;
        float boxWidth = textWidth + 8;
        float x = hud.watermarkX;
        float y = hud.watermarkY;
        int bgColor = hud.applyOpacity(0xAA101010);
        int mainColor = hud.getColor(true);

        if (hud.background.getValue()) {
            drawSkijaRectWithBlur(canvas, x, y, boxWidth, boxHeight, hud.cornerRadius(), bgColor, hud.shaderBg.getValue(), hud);
        }
        if (hud.watermarkMode.getValue() == WatermarkMode.CSGO) {
            try (Paint accentPaint = new Paint().setColor(mainColor).setAntiAlias(true)) {
                canvas.drawRect(Rect.makeXYWH(x, y, boxWidth, 1.5f), accentPaint);
            }
        }
        float textDrawY = y - fm.getAscent() + TEXT_V_PADDING + 2;
        drawSkijaString(canvas, font, finalText, x + 4, textDrawY, mainColor, hud.textShadow.getValue(), hud.textGlow.getValue(), hud);
    }

    private List<IModule> getSortedEnabledModules(Font font) {
        return moduleManager.getModules().stream()
                .filter(mod -> !isModuleHidden(mod) && mod.isEnabled())
                .sorted(Comparator.comparingDouble((IModule mod) -> -font.measureText(generateModuleData(mod)).getWidth()))
                .collect(Collectors.toList());
    }

    private boolean isModuleHidden(IModule module) {
        return module.isHidden();
    }

    private void renderModules(Canvas canvas, Font font, float screenWidth, float screenHeight, List<IModule> modules, HUDModule hud) {
        hud.arraylistY = arraylistYPos.getValue();
        int index = 0;
        float currentElementHeight = hud.elementHeight.getValue();
        FontMetrics fm = font.getMetrics();
        float minRequiredHeight = (-fm.getAscent() + fm.getDescent()) + elementHeight.getValue() * 2 - 5;
        currentElementHeight = Math.max(currentElementHeight, Math.max(1f, minRequiredHeight));

        for (IModule module : modules) {
            renderModule(canvas, font, screenWidth, module, hud.arraylistY, index, currentElementHeight, modules, hud);
            hud.arraylistY += currentElementHeight;
            index++;
        }
    }

    private void renderModule(Canvas canvas, Font font, float screenWidth, IModule module, float currentY, int index, float height, List<IModule> modules, HUDModule hud) {
        String moduleData = hud.generateModuleData(module);
        float moduleWidth = SkijaRenderUtil.measureColoredStringWidth(font, moduleData);
        float x = Math.max(0, screenWidth - moduleWidth - hud.arraylistXPos.getValue());
        int primaryColor = hud.generateColorForIndex(index);
        int secondaryColor = hud.generateColorForIndex(index + 1);
        int bgColor = hud.applyOpacity(0xAA0A0A0A);

        if (hud.background.getValue()) {
            float bgX = Math.max(0, screenWidth - moduleWidth - 1 - hud.arraylistXPos.getValue());
            float bgWidth = moduleWidth + 4f;
            if (hud.arrayListOutline.getValue() == OutlineMode.LEFT || hud.arrayListOutline.getValue() == OutlineMode.FULL) {
                bgX -= 1.5f;
                bgWidth += 1.5f;
            }
            if (hud.arrayListOutline.getValue() == OutlineMode.RIGHT || hud.arrayListOutline.getValue() == OutlineMode.FULL) {
                bgWidth += 1.5f;
            }

            if (hud.backgroundMode.getValue() == BackgroundMode.OPACITY) {
                drawSkijaRectWithBlur(canvas, bgX, currentY, bgWidth, height, 0, bgColor, hud.shaderBg.getValue(), hud);
            } else {
                int gradBgColor1 = hud.applyOpacity(primaryColor);
                int gradBgColor2 = hud.applyOpacity(secondaryColor);
                drawSkijaGradientRectWithBlur(canvas, bgX, currentY, bgWidth, height + 0.1f, gradBgColor1, gradBgColor2, true, hud.shaderBg.getValue(), hud);
            }
        }

        renderBar(canvas, screenWidth, x, currentY, height, moduleWidth, index, primaryColor, secondaryColor, modules, hud);

        FontMetrics fm = font.getMetrics();
        float textDrawY = currentY - fm.getAscent() + elementHeight.getValue() - 5;

        textDrawY = Math.max(currentY - fm.getAscent(), textDrawY);
        textDrawY = Math.min(currentY + height - fm.getDescent(), textDrawY);

        SkijaRenderUtil.drawColoredString(canvas, font, moduleData, x, textDrawY, primaryColor,
                hud.textShadow.getValue(), hud.textGlow.getValue(), 153);
    }


    private void renderBar(Canvas canvas, float screenWidth, float moduleStartX, float moduleY, float height, float moduleWidth, int index, int color1, int color2, List<IModule> modules, HUDModule hud) {
        OutlineMode mode = hud.arrayListOutline.getValue();
        if (mode == OutlineMode.NONE) return;
        float barThickness = 2.5f;
        float rightBarX = moduleStartX + moduleWidth + 2.5f;
        float leftBarX = moduleStartX - barThickness + 1f;
        rightBarX = Math.min(rightBarX, screenWidth);
        leftBarX = Math.max(0, leftBarX);
        Rect topRect = Rect.makeXYWH(leftBarX, moduleY - barThickness, mode == OutlineMode.TOP ? (rightBarX - leftBarX) : (rightBarX - leftBarX) + barThickness, barThickness);
        Rect rightRect = Rect.makeXYWH(rightBarX, moduleY, barThickness, height);
        Rect leftRect = Rect.makeXYWH(leftBarX, moduleY, barThickness, height);
        Rect bottomRect = Rect.makeXYWH(leftBarX, moduleY + height, (rightBarX - leftBarX) + barThickness, barThickness);
        try (Paint paint1 = new Paint().setColor(color1); Paint paint2 = new Paint().setColor(color2)) {
            boolean drawTop = (mode == OutlineMode.TOP || mode == OutlineMode.TOP_RIGHT || mode == OutlineMode.FULL) && index == 0;
            boolean drawRight = mode == OutlineMode.RIGHT || mode == OutlineMode.TOP_RIGHT || mode == OutlineMode.FULL;
            boolean drawLeft = mode == OutlineMode.LEFT || mode == OutlineMode.FULL;
            boolean drawBottom = mode == OutlineMode.FULL && index == modules.size() - 1;
            if (drawTop && topRect.getHeight() > 0 && topRect.getWidth() > 0) canvas.drawRect(topRect, paint1);
            if (drawRight && rightRect.getHeight() > 0 && rightRect.getWidth() > 0)
                drawVerticalGradientSkija(canvas, rightRect, color1, color2);
            if (drawLeft && leftRect.getHeight() > 0 && leftRect.getWidth() > 0)
                drawVerticalGradientSkija(canvas, leftRect, color1, color2);
            if (drawBottom && bottomRect.getHeight() > 0 && bottomRect.getWidth() > 0)
                canvas.drawRect(bottomRect, paint2);
        }
    }

    private void drawSkijaRectWithBlur(Canvas canvas, float x, float y, float w, float h, float radius, int color, boolean blur, HUDModule hud) {
        Rect bounds = Rect.makeXYWH(x, y, w, h);
        RRect rrect = RRect.makeXYWH(x, y, w, h, radius);
        if (blur) {
            try (Paint blurPaint = new Paint()) {
                try (ImageFilter blurFilter = ImageFilter.makeBlur(hud.blurSigmaX, hud.blurSigmaY, FilterTileMode.CLAMP)) {
                    blurPaint.setImageFilter(blurFilter);
                }
                Rect layerBounds = bounds.inflate(hud.blurSigmaX * 2);
                canvas.saveLayer(layerBounds, blurPaint);
                try (Paint bgPaint = new Paint().setColor(color).setAntiAlias(true)) {
                    if (radius > 0) canvas.drawRRect(rrect, bgPaint);
                    else canvas.drawRect(bounds, bgPaint);
                }
                canvas.restore();
            }
        } else {
            try (Paint bgPaint = new Paint().setColor(color).setAntiAlias(true)) {
                if (radius > 0) canvas.drawRRect(rrect, bgPaint);
                else canvas.drawRect(bounds, bgPaint);
            }
        }
    }

    private void drawSkijaGradientRectWithBlur(Canvas canvas, float x, float y, float w, float h, int color1, int color2, boolean vertical, boolean blur, HUDModule hud) {
        Rect bounds = Rect.makeXYWH(x, y, w, h);
        try (Shader gradientShader = vertical ? Shader.makeLinearGradient(x, y, x, y + h, new int[]{color1, color2}) : Shader.makeLinearGradient(x, y, x + w, y, new int[]{color1, color2})) {
            if (blur) {
                try (Paint blurPaint = new Paint()) {
                    try (ImageFilter blurFilter = ImageFilter.makeBlur(hud.blurSigmaX, hud.blurSigmaY, FilterTileMode.CLAMP)) {
                        blurPaint.setImageFilter(blurFilter);
                    }
                    Rect layerBounds = bounds.inflate(hud.blurSigmaX * 2);
                    canvas.saveLayer(layerBounds, blurPaint);
                    try (Paint gradPaint = new Paint().setShader(gradientShader).setAntiAlias(true)) {
                        canvas.drawRect(bounds, gradPaint);
                    }
                    canvas.restore();
                }
            } else {
                try (Paint gradPaint = new Paint().setShader(gradientShader).setAntiAlias(true)) {
                    canvas.drawRect(bounds, gradPaint);
                }
            }
        }
    }

    private void drawSkijaString(Canvas canvas, Font font, String text, float x, float y, int color, boolean shadow, boolean glow, HUDModule hud) {
        if (glow) {
            try (Paint glowPaint = new Paint().setColor(color).setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.NORMAL, 2.0f, false)).setAntiAlias(true)) {
                canvas.drawString(text, x, y, font, glowPaint);
            }
        }
        if (shadow) {
            int shadowColor = hud.applyOpacity(0x99000000);
            try (Paint shadowPaint = new Paint().setColor(shadowColor).setAntiAlias(true)) {
                canvas.drawString(text, x + 1, y + 0.75f, font, shadowPaint);
            }
        }
        try (Paint textPaint = new Paint().setColor(color).setAntiAlias(true)) {
            canvas.drawString(text, x, y, font, textPaint);
        }
    }

    private void drawVerticalGradientSkija(Canvas canvas, Rect rect, int color1, int color2) {
        try (Shader shader = Shader.makeLinearGradient(rect.getLeft(), rect.getTop(), rect.getLeft(), rect.getBottom(), new int[]{color1, color2})) {
            try (Paint paint = new Paint().setShader(shader)) {
                canvas.drawRect(rect, paint);
            }
        }
    }

    private int applyOpacity(int color) {
        int alpha = this.opacity.getValue() & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private float cornerRadius() {
        return 4f;
    }

    private String generateModuleData(IModule module) {
        StringBuilder text = new StringBuilder().append(module.getDisplayName());
        String meta = getModuleMetadataString(module);
        if (!meta.isEmpty()) {
            switch (this.arrayListMetaData.getValue()) {
                case SIMPLE:
                    text.append(" ").append(EnumChatFormatting.GRAY).append(meta);
                    break;
                case SQUARE:
                    text.append(EnumChatFormatting.GRAY).append(" [").append(EnumChatFormatting.WHITE).append(meta).append(EnumChatFormatting.GRAY).append("]");
                    break;
                case DASH:
                    text.append(" ").append(EnumChatFormatting.GRAY).append("- ").append(meta);
                    break;
                case NONE:
                    break;
            }
        }
        String finalText = text.toString();
        if (this.lowercase.getValue()) finalText = finalText.toLowerCase(Locale.getDefault());
        return finalText;
    }

    private String getModuleMetadataString(IModule module) {
        return module.getSettings().stream().filter(s -> s.getValue() instanceof Enum && s.getName().equalsIgnoreCase("Mode")).map(s -> ((Enum<?>) s.getValue()).name()).findFirst().orElse("");
    }

    public double getSpeed() {
        if (mc.thePlayer == null) return 0.0;
        float timerSpeed = (mc.timer != null) ? mc.timer.timerSpeed : 1.0f;
        double bps = (Math.hypot(mc.thePlayer.posX - mc.thePlayer.prevPosX, mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * timerSpeed) * 20;
        return Math.round(bps * 100.0) / 100.0;
    }

    public enum ColorMode {CLIENT, CUSTOM, RAINBOW, RAINBOW_PULSE, STATIC}


    // TODO: implement these fuckers
    public enum FontType {DEFAULT, PRODUCT_SANS, ENCHANTMENT, GREYCLIFF, MINECRAFT, JETBRAINS, URBANIST, MANROPE, POPPINS, SF_PRO}

    public enum BackgroundMode {CUSTOM, OPACITY}

    public enum WatermarkMode {TEXT, CSGO, LOGO}

    public enum OutlineMode {TOP, RIGHT, TOP_RIGHT, LEFT, FULL, NONE}

    public enum SortingSetting {LENGTH, ALPHABETICAL}

    public enum MetaData {SIMPLE, SQUARE, DASH, NONE}

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}