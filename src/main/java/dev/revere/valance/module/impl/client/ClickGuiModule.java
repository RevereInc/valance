package dev.revere.valance.module.impl.client;

import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;

@ModuleInfo(name = "ClickGUI", displayName = "Click GUI", description = "Opens the main client configuration GUI.", category = Category.CLIENT, isHidden = true)
public class ClickGuiModule extends AbstractModule {
    public ClickGuiModule(IEventBusService eventBusService) {
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
