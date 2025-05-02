package dev.revere.valance.event.type.player;

import dev.revere.valance.event.Cancellable;
import dev.revere.valance.event.IEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@Getter
@Setter
public class MoveEvent implements IEvent, Cancellable {
    private double posX, posY, posZ;
    private boolean cancelled = false;

    /**
     * Constructs the MoveEvent.
     *
     * @param posX Player's X position to be sent.
     * @param posY Player's Y position (bounding box minimum Y) to be sent.
     * @param posZ Player's Z position to be sent.
     */
    public MoveEvent(double posX, double posY, double posZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }
}
