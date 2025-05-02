package dev.revere.valance.module.impl.render;

import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@ModuleInfo(name = "Fullbright", displayName = "Fullbright", description = "Increases brightness in the game.", category = Category.RENDER)
public class FullbrightModule extends AbstractModule {
    private float oldGamma;

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public FullbrightModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings.gammaSetting != 1000) {
            oldGamma = mc.gameSettings.gammaSetting;
        }

        mc.gameSettings.gammaSetting = 1000;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = oldGamma;
        super.onDisable();
    }
}
