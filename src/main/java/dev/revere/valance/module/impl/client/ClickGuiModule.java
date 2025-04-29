package dev.revere.valance.module.impl.client;

import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;

@ModuleInfo(name = "ClickGUI", description = "Opens the main client configuration GUI.", category = Category.CLIENT)
public class ClickGuiModule extends AbstractModule {

    public ClickGuiModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Override
    public void onEnable() {

        this.setEnabled(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
