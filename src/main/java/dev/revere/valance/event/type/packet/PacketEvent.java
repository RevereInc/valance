package dev.revere.valance.event.type.packet;

import dev.revere.valance.event.Cancellable;
import dev.revere.valance.event.IEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Getter
@Setter
public class PacketEvent implements IEvent, Cancellable {
    /**
     * Represents the direction or state of the packet event.
     */
    public enum EventState {
        SENDING,
        RECEIVING
    }

    private final EventState eventState;

    private Packet<?> packet;
    private boolean cancelled = false;

    /**
     * Constructs a PacketEvent.
     *
     * @param eventState The state (direction) of the packet.
     * @param packet     The network packet involved.
     */
    public PacketEvent(EventState eventState, Packet<?> packet) {
        this.eventState = eventState;
        this.packet = packet;
    }

    /**
     * Gets the packet associated with this event, cast to the specified type.
     * Use with caution, ensure the type is correct.
     *
     * @param <T> The expected Packet subtype.
     * @return The packet instance, cast to type T.
     * @throws ClassCastException if the packet is not assignable to type T.
     */
    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T getPacket() {
        // Perform the cast. This can throw ClassCastException if misused.
        return (T) packet;
    }

    /**
     * Gets the raw Packet<?> object without casting.
     * @return The Packet instance.
     */
    public Packet<?> getRawPacket() {
        return packet;
    }
}