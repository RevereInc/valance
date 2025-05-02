package dev.revere.valance.event.type.player;

import dev.revere.valance.event.Cancellable;
import dev.revere.valance.event.IEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@Setter
@Getter
public class PostMotionEvent implements IEvent {
    private float yaw;
    private float pitch;
    private boolean ground;
    private double x;
    private double y;
    private double z;

    /**
     * PreUpdateEvent constructor to initialize the event.
     *
     * @param yaw    the yaw of the player
     * @param pitch  the pitch of the player
     * @param x      the x position of the player
     * @param y      the y position of the player
     * @param z      the z position of the player
     * @param ground the ground state of the player
     */
    public PostMotionEvent(float yaw, float pitch, double x, double y, double z, boolean ground) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }
}
