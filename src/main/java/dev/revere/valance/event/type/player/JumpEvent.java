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
public class JumpEvent implements IEvent, Cancellable {
    private float jumpMotion;
    private float yaw;
    private boolean cancelled = false;

    /**
     * JumpEvent constructor to initialize the event.
     *
     * @param jumpMotion the jump motion of the player
     * @param yaw        the yaw of the player
     */
    public JumpEvent(float jumpMotion, float yaw) {
        this.jumpMotion = jumpMotion;
        this.yaw = yaw;
    }
}
