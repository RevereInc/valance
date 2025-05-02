package dev.revere.valance.module.impl.render;

import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.settings.type.NumberSetting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
        name = "MotionBlur",
        displayName = "Motion Blur",
        description = "Adds a motion blur effect to the game.",
        category = Category.RENDER
)
public class MotionBlurModule extends AbstractModule {
    private final Setting<Integer> strength = new NumberSetting<>("Strength", 6, 1, 10, 1);

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public MotionBlurModule(IEventBusService eventBusService) {
        super(eventBusService);
        setKey(Keyboard.KEY_J);
    }

    public float value;

    public float getMultiplier() {
        return strength.getValue() * 10;
    }

    public float getAccumulationValue() {
        value = getMultiplier() * 10.0F;
        long lastTimestampInGame = System.currentTimeMillis();

        if (value > 996.0F) {
            value = 996.0F;
        }

        if (value > 990.0F) {
            value = 990.0F;
        }

        long i = System.currentTimeMillis() - lastTimestampInGame;

        if (i > 10000L) {
            return 0.0F;
        } else {
            if (value < 0.0F) {
                value = 0.0F;
            }

            return value / 1000.0F;
        }
    }

    public void createAccumulation() {
        if (!isEnabled()) return;
        float value = getAccumulationValue();
        GL11.glAccum(GL11.GL_MULT, value);
        GL11.glAccum(GL11.GL_ACCUM, 1.0F - value);
        GL11.glAccum(GL11.GL_RETURN, 1.0F);
    }
}