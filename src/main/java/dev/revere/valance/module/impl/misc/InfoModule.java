package dev.revere.valance.module.impl.misc;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.game.ClientTickEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.IModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import static dev.revere.valance.util.MinecraftUtil.getPlayer;
import static dev.revere.valance.util.MinecraftUtil.sendClientMessage;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */

@ModuleInfo(name = "InfoDisplay", description = "Shows client/game info periodically.", category = Category.MISC)
public class InfoModule extends AbstractModule {
    private final IModuleManager moduleManager;
    private int tickCounter = 0;

    public InfoModule(IEventBusService eventBusService, IModuleManager moduleManager) {
        super(eventBusService);
        this.moduleManager = moduleManager;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    public void onDisable(){
        super.onDisable();
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (!isEnabled()) return;

        tickCounter++;
        int INFO_INTERVAL = 20 * 15;
        if (tickCounter >= INFO_INTERVAL) {
            tickCounter = 0;

            long enabledModuleCount = moduleManager.getModules().stream().filter(IModule::isEnabled).count();
            String playerName = getPlayer().map(EntityPlayer::getName).orElse("N/A");
            String server = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
            int fps = Minecraft.getDebugFPS();


            sendClientMessage("--- Client Info ---");
            sendClientMessage(" Version: " + ClientLoader.CLIENT_VERSION);
            sendClientMessage(" Player: " + playerName);
            sendClientMessage(" Server: " + server);
            sendClientMessage(" FPS: " + fps);
            sendClientMessage(" Enabled Modules: " + enabledModuleCount + " / " + moduleManager.getModules().size());
            sendClientMessage("--------------------");
        }
    }
}