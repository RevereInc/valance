package dev.revere.valance.event.type.player;

import dev.revere.valance.event.IEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@Setter
@Getter
public class UpdateEvent implements IEvent {
    private boolean ground;
    private float yaw;
    private float pitch;
    private double y;
    private boolean pre;

    /**
     * UpdateEvent constructor to initialize the event.
     *
     * @param yaw      the yaw of the player
     * @param pitch    the pitch of the player
     * @param y        the y position of the player
     * @param ground the ground state of the player
     * @param pre      the pre-state of the player
     */
    public UpdateEvent(float yaw, float pitch, double y, boolean ground, boolean pre) {
        this.y = y;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
        this.pre = pre;
    }

    /**
     * UpdateEvent constructor to initialize the event.
     *
     * @param yaw      the yaw of the player
     * @param pitch    the pitch of the player
     * @param y        the y position of the player
     * @param ground the ground state of the player
     */
    public UpdateEvent(float yaw, float pitch, double y, boolean ground) {
        this.y = y;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }

    /**
     * Set the yaw of the player.
     *
     * @param yaw the yaw of the player
     */
    public void setYaw(float yaw) {
        Minecraft.getMinecraft().thePlayer.renderYawOffset = yaw;
        Minecraft.getMinecraft().thePlayer.rotationYawHead = yaw;
        this.yaw = yaw;
    }
}
