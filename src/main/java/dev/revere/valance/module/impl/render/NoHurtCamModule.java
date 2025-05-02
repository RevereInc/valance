package dev.revere.valance.module.impl.render;

import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@ModuleInfo(name = "NoHurtCam", displayName = "No Hurt Cam", description = "Disable hurt camera", category = dev.revere.valance.module.Category.RENDER)
public class NoHurtCamModule extends AbstractModule {
    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public NoHurtCamModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
