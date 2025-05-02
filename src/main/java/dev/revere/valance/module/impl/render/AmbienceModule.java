package dev.revere.valance.module.impl.render;

import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.packet.PacketEvent;
import dev.revere.valance.event.type.player.PreMotionEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.settings.type.NumberSetting;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@ModuleInfo(name = "Ambience", displayName = "Ambience", description = "Module to handle ambience effects.", category = Category.RENDER)
public class AmbienceModule extends AbstractModule {
    private final Setting<Integer> time = new NumberSetting<>("Time", 1000, 0, 22999);
    private final Setting<WeatherMode> weather = new Setting<>("Weather", WeatherMode.UNCHANGED);

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public AmbienceModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Subscribe
    private void onPreMotion(PreMotionEvent event) {
        mc.theWorld.setWorldTime(time.getValue());

        switch(this.weather.getValue()) {
            case CLEAR: {
                resetWeather();
                break;
            }
            case RAIN:
            case SNOW: {
                mc.theWorld.setRainStrength(1);
                mc.theWorld.getWorldInfo().setCleanWeatherTime(0);
                mc.theWorld.getWorldInfo().setRainTime(Integer.MAX_VALUE);
                mc.theWorld.getWorldInfo().setThunderTime(0);
                mc.theWorld.getWorldInfo().setRaining(true);
                mc.theWorld.getWorldInfo().setThundering(false);
                break;
            }
        }
    }

    @Subscribe
    private void onPacket(PacketEvent event) {
        if (event.getEventState() == PacketEvent.EventState.RECEIVING) {
            Packet<INetHandlerPlayClient> packet = event.getPacket();
            if (packet instanceof S03PacketTimeUpdate) {
                event.setCancelled(true);
            }
        }
    }

    private void resetWeather() {
        mc.theWorld.setRainStrength(0);
        mc.theWorld.getWorldInfo().setCleanWeatherTime(Integer.MAX_VALUE);
        mc.theWorld.getWorldInfo().setRainTime(0);
        mc.theWorld.getWorldInfo().setThunderTime(0);
        mc.theWorld.getWorldInfo().setRaining(false);
        mc.theWorld.getWorldInfo().setThundering(false);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetWeather();
        super.onDisable();
    }

    public enum WeatherMode {
        UNCHANGED,
        CLEAR,
        RAIN,
        SNOW
    }
}
