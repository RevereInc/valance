package dev.revere.valance.module.impl.combat;

import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.packet.PacketEvent;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@ModuleInfo(name = "Velocity", displayName = "Velocity", description = "Reduces knockback.", category = dev.revere.valance.module.Category.COMBAT)
public class VelocityModule extends AbstractModule {
    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public VelocityModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Subscribe
    private void onPacket(PacketEvent event) {
        if (event.getEventState() == PacketEvent.EventState.RECEIVING) {
            Packet<INetHandlerPlayClient> packet = event.getPacket();
            if (packet instanceof S12PacketEntityVelocity || packet instanceof S27PacketExplosion) {
                event.setCancelled(true);
            }
        }
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
