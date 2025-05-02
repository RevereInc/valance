package dev.revere.valance.service.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.event.Listener;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.input.KeyDownEvent;
import dev.revere.valance.event.type.input.KeyUpEvent;
import dev.revere.valance.input.BindType;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IKeybindService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Objects;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@Service(provides = IKeybindService.class, priority = 300)
public class KeybindService implements IKeybindService, Listener {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":KeybindService]";

    private final IModuleManager moduleManager;
    private final IEventBusService eventBusService;

    @Inject
    public KeybindService(IModuleManager moduleManager, IEventBusService eventBusService) {
        this.moduleManager = Objects.requireNonNull(moduleManager, "ModuleManager cannot be null");
        this.eventBusService = Objects.requireNonNull(eventBusService, "EventBusService cannot be null");
        Logger.info(LOG_PREFIX, "Constructed.");
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        this.eventBusService.register(this);
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        this.eventBusService.unregister(this);
    }

    @Subscribe
    public void onKeyDown(KeyDownEvent event) {
        if (isInvalidScreen(Minecraft.getMinecraft().currentScreen)) {
            return;
        }
        int keyCode = event.getKey();
        if (keyCode == Keyboard.KEY_NONE) return;

        List<AbstractModule> modules = moduleManager.getModulesOfType(AbstractModule.class);
        for (AbstractModule module : modules) {
            if (module.isBound() && module.getKey() == keyCode) {
                if (module.getBindType() == BindType.TOGGLE) {
                    module.toggle();
                } else if (module.getBindType() == BindType.HOLD) {
                    if (!module.isEnabled()) {
                        module.setEnabled(true);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onKeyUp(KeyUpEvent event) {
        if (isInvalidScreen(Minecraft.getMinecraft().currentScreen)) {
            return;
        }
        int keyCode = event.getKey();
        if (keyCode == Keyboard.KEY_NONE) return;

        List<AbstractModule> modules = moduleManager.getModulesOfType(AbstractModule.class);
        for (AbstractModule module : modules) {
            if (module.isBound() && module.getBindType() == BindType.HOLD && module.getKey() == keyCode) {
                if (module.isEnabled()) {
                    module.setEnabled(false);
                }
            }
        }
    }

    @Override
    public boolean isInvalidScreen(GuiScreen screen) {
        return screen != null;
    }
}
