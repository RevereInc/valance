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
public class StrafeEvent implements IEvent, Cancellable {
    private float forward;
    private float strafe;
    private float friction;
    private float yaw;
    private boolean cancelled = false;

    public StrafeEvent(float forward, float strafe, float friction, float yaw) {
        this.forward = forward;
        this.strafe = strafe;
        this.friction = friction;
        this.yaw = yaw;
    }
}