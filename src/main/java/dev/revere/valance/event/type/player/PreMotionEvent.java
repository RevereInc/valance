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
@Getter
@Setter
public class PreMotionEvent implements IEvent, Cancellable {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean ground;
    private boolean cancelled = false;

    /**
     * Constructs the PreMotionEvent.
     *
     * @param x        Player's X position to be sent.
     * @param y        Player's Y position (bounding box minimum Y) to be sent.
     * @param z        Player's Z position to be sent.
     * @param yaw      Player's yaw to be sent.
     * @param pitch    Player's pitch to be sent.
     * @param ground   Player's onGround state to be sent.
     */
    public PreMotionEvent(float yaw, float pitch, double x, double y, double z, boolean ground) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }

    /**
     * Convenience method to set both yaw and pitch.
     *
     * @param yaw   New yaw.
     * @param pitch New pitch.
     */
    public void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}