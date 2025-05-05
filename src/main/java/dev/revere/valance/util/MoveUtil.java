package dev.revere.valance.util;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

import static dev.revere.valance.util.MinecraftUtil.mc;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
public final class MoveUtil {

    private MoveUtil() {}

    public static final double WALK_SPEED = 0.221;
    public static final double MOD_SPRINTING = 1.3;
    public static final double JUMP_HEIGHT = 0.42F;

    /** Checks if the player has significant movement input. */
    public static boolean isMoving() {
        EntityPlayerSP player = mc().thePlayer;
        return player != null && (player.moveForward != 0 || player.moveStrafing != 0);
    }

    /** Stops horizontal motion. */
    public static void stop() {
        EntityPlayerSP player = mc().thePlayer;
        if (player != null) {
            player.motionX = 0;
            player.motionZ = 0;
        }
    }

    /** Gets the player's current movement direction in radians. */
    public static double direction() {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return 0.0;

        float rotationYaw = player.rotationYaw; // Use player's actual yaw

        // Adjust yaw based on movement input (forward/backward/strafe)
        if (player.moveForward < 0) {
            rotationYaw += 180;
        }
        float forward = 1;
        if (player.moveForward < 0) {
            forward = -0.5F;
        } else if (player.moveForward > 0) {
            forward = 0.5F;
        }
        if (player.moveStrafing > 0) {
            rotationYaw -= 90 * forward;
        }
        if (player.moveStrafing < 0) {
            rotationYaw += 90 * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static void strafe() {
        strafe(speed(), mc().thePlayer);
    }

    /**
     * Makes the player strafe at the specified speed
     */
    public static void strafe(final double speed, Entity entity) {
        if (!isMoving()) {
            return;
        }

        final double yaw = direction();
        entity.motionX = -MathHelper.sin((float) yaw) * speed;
        entity.motionZ = MathHelper.cos((float) yaw) * speed;
    }

    /** Sets the player's motion based on direction and speed. */
    public static void strafe(final double speed) {
        EntityPlayerSP player = mc().thePlayer;
        if (!isMoving() || player == null) {
            return;
        }
        final double yaw = direction();
        player.motionX = -MathHelper.sin((float) yaw) * speed;
        player.motionZ = MathHelper.cos((float) yaw) * speed;
    }

    /** Sets the player's motion based on a specific yaw and speed. */
    public static void strafe(final double speed, float yaw) {
        EntityPlayerSP player = mc().thePlayer;
        if (!isMoving() || player == null) {
            return;
        }
        yaw = (float) Math.toRadians(yaw);
        player.motionX = -MathHelper.sin(yaw) * speed;
        player.motionZ = MathHelper.cos(yaw) * speed;
    }


    /** Gets the player's current horizontal speed. */
    public static double speed() {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return 0.0;
        return Math.hypot(player.motionX, player.motionZ);
    }

    /** Gets the base movement speed considering sprinting and potion effects. */
    public static double getBaseMoveSpeed() {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return 0.0;

        double baseSpeed = player.capabilities.getWalkSpeed() * 2.873;
        if (player.isPotionActive(Potion.moveSpeed)) {
            baseSpeed *= 1.0 + 0.2 * (player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return baseSpeed;
    }

    /**
     * Calculates the default player jump motion
     *
     * @return player jump motion
     */
    public static double jumpMotion() {
        return jumpBoostMotion(JUMP_HEIGHT);
    }

    /**
     * Modifies a selected motion with jump boost
     *
     * @param motionY input motion
     * @return modified motion
     */
    public static double jumpBoostMotion(final double motionY) {
        if (mc().thePlayer.isPotionActive(Potion.jump)) {
            return motionY + (mc().thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
        }

        return motionY;
    }

    /** Calculates the allowed horizontal distance per tick (approximates NCP). */
    public static double getAllowedHorizontalDistance() {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return 0.0;

        double horizontalDistance;
        // Simplified version - check common conditions
        if (player.isInWeb) {
            horizontalDistance = 0.022; // Roughly web speed
        } else if (PlayerUtil.isInLiquid()) { // Assumes PlayerUtil.isInLiquid() exists
            horizontalDistance = 0.1; // Rough swim speed (adjust based on depth strider later)
        } else if (player.isSneaking()) {
            horizontalDistance = 0.0663; // Rough sneak speed
        } else {
            horizontalDistance = getBaseMoveSpeed() * (player.isSprinting() ? MOD_SPRINTING : 1.0);
            // Adjust base speed instead of multiplying walk speed directly
            // Vanilla walk speed is closer to 0.1, sprinting makes it ~0.13
            // Speed pots multiply this. Let's refine getBaseMoveSpeed slightly.
            double base = player.isSprinting() ? 0.2873 : 0.221; // More accurate base values
            if (player.isPotionActive(Potion.moveSpeed)) {
                base *= 1.0 + 0.2 * (player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
            }
            if (player.isPotionActive(Potion.moveSlowdown)) {
                base *= 1.0 - 0.15 * (player.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() + 1);
            }
            horizontalDistance = base;
        }

        // This is still an approximation, true NCP allowed distance depends on friction, etc.
        return horizontalDistance;
    }

    /** Applies strafing adjustments based on partial percentage. */
    public static void partialStrafePercent(double percentage) {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return;

        percentage /= 100.0;
        percentage = Math.min(1.0, Math.max(0.0, percentage)); // Clamp 0-1

        double originalMotionX = player.motionX;
        double originalMotionZ = player.motionZ;

        // Calculate target strafe motion
        strafe(speed()); // Temporarily set motion based on current direction/speed

        // Apply percentage of the change
        player.motionX = originalMotionX + (player.motionX - originalMotionX) * percentage;
        player.motionZ = originalMotionZ + (player.motionZ - originalMotionZ) * percentage;
    }
}
