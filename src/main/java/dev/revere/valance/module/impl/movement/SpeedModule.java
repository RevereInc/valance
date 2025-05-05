package dev.revere.valance.module.impl.movement;

import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.player.JumpEvent;
import dev.revere.valance.event.type.player.PreMotionEvent;
import dev.revere.valance.event.type.player.StrafeEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.properties.Property;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.util.MoveUtil;
import dev.revere.valance.util.PlayerUtil;
import dev.revere.valance.util.TimerUtil;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@ModuleInfo(name = "Speed", displayName = "Speed", description = "Modifies movement speed.", category = Category.MOVEMENT)
public class SpeedModule extends AbstractModule {
    private final Property<Mode> mode = new Property<>("Mode", Mode.LOW_STRAFE);
    private final Property<Boolean> applyAirStrafe = new Property<>("AirStrafe", true);
    private final Property<Boolean> applyGlide = new Property<>("GlideStrafe", true);
    private final Property<Boolean> frictionOverride = new Property<>("FrictionOverride", true);
    private final Property<Boolean> modifyJumpMotion = new Property<>("AlternateMotion", false);

    private boolean stopSpeed = false;
    private boolean landingBoostApplied = false;
    private boolean isCollisionCooldownActive = false;

    private int collisionCooldownEndTick = 0;
    private int jump = 0;

    private final TimerUtil timer;

    /**
     * Constructor for dependency injection.
     * Concrete modules MUST call this super constructor, providing the required services.
     *
     * @param eventBusService The injected Event Bus service instance.
     */
    public SpeedModule(IEventBusService eventBusService) {
        super(eventBusService);
        this.timer = new TimerUtil();

        setKey(Keyboard.KEY_V);
    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (mode.getValue() == Mode.MMC) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        } else {

            if (mc.thePlayer == null || mc.theWorld == null) return;

            if (mc.thePlayer.isInWater() || mc.thePlayer.isInWeb || mc.thePlayer.isInLava()) {
                stopSpeed = true;
                MoveUtil.stop();
                return;
            }

            if (mc.thePlayer.isCollidedHorizontally) {
                isCollisionCooldownActive = true;
                collisionCooldownEndTick = mc.thePlayer.ticksExisted + 9;
                MoveUtil.stop();
            } else if (mc.thePlayer.ticksExisted > collisionCooldownEndTick) {
                isCollisionCooldownActive = false;
            }

            if (mc.thePlayer.onGround) {
                landingBoostApplied = false;
            }

            if (PlayerUtil.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air) {
                stopSpeed = false;
            }

            if (mc.thePlayer.isCollidedVertically && !mc.thePlayer.onGround && mode.getValue() == Mode.LOW_STRAFE && PlayerUtil.isBlockOver(2.0, true)) {
                stopSpeed = true;
                MoveUtil.stop();
            }
        }
    }

    @Subscribe
    public void onStrafe(StrafeEvent event) {
        if (mode.getValue() == Mode.MMC) {
            if (jump % 2 == 1 && mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.16477328182606651;
            }

            if (mc.thePlayer.hurtTime <= 2) {
                MoveUtil.strafe();
            }
        } else {
            if (mc.thePlayer == null || mc.theWorld == null || stopSpeed) {
                MoveUtil.stop();
                return;
            }

            Mode currentMode = mode.getValue();
            boolean moving = MoveUtil.isMoving();
            boolean canSpeed = !isCollisionCooldownActive && !stopSpeed;
            double allowedSpeed = MoveUtil.getAllowedHorizontalDistance();

            if (mc.thePlayer.onGround && moving && canSpeed) {
                mc.thePlayer.motionY = MoveUtil.jumpMotion();
                double initialSpeed = allowedSpeed * (currentMode == Mode.LOW_STRAFE && !mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 1.0 : 0.994); // Slightly less for high strafe/speed pot?
                MoveUtil.strafe(initialSpeed);
                if (frictionOverride.getValue()) {
                    // mc.thePlayer.motionX /= 0.6;
                    // mc.thePlayer.motionZ /= 0.6;
                }
            } else if (mc.thePlayer.onGround) {
                // MoveUtil.stop();
            } else {
                int ticks = mc.thePlayer.offGroundTicks;
                double currentSpeed = MoveUtil.speed();

                switch (currentMode) {
                    case STRAFE:
                        if (ticks == 1 && canSpeed) {
                            double tick1Speed = mc.thePlayer.isPotionActive(Potion.moveSpeed) ? (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() >= 1 ? 0.48 : 0.40) : 0.33;
                            MoveUtil.strafe(tick1Speed);
                        }
                        if (ticks >= 2 && ticks <= 8 && applyAirStrafe.getValue() && canSpeed) {
                            // for maintaining speed:
                            // MoveUtil.partialStrafePercent(69);
                        }

                        // Glide strafe adjustments
                        if (ticks >= 9 && applyGlide.getValue() && canSpeed && !PlayerUtil.isBlockOver(2.0, true)) {
                            if (PlayerUtil.blockRelativeToPlayer(0, mc.thePlayer.motionY * 2, 0) != Blocks.air) {
                                mc.thePlayer.motionY += 0.075;
                                MoveUtil.strafe(allowedSpeed * (ticks == 9 ? 1.0 : 1.005));
                            }
                        }

                        break;
                    case LOW_STRAFE:
                        if (ticks == 1 && canSpeed) {
                            mc.thePlayer.motionY += 0.057f;
                            double tick1Speed = mc.thePlayer.isPotionActive(Potion.moveSpeed) ? (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() >= 1 ? 0.48 : 0.405) : 0.33;
                            MoveUtil.strafe(tick1Speed);
                        }
                        if (ticks == 2 && canSpeed && applyAirStrafe.getValue()) {
                            // MoveUtil.partialStrafePercent(66);
                        }
                        if (ticks == 3 && canSpeed) mc.thePlayer.motionY -= 0.1309f;
                        if (ticks == 4 && canSpeed) mc.thePlayer.motionY -= 0.2;

                        // glide strafe for low hop
                        if (ticks == 6 && canSpeed && applyGlide.getValue() && PlayerUtil.blockRelativeToPlayer(0, mc.thePlayer.motionY * 3, 0) != Blocks.air) {
                            mc.thePlayer.motionY += 0.075;
                            MoveUtil.strafe(allowedSpeed);
                        }

                        // landing speed boost
                        if (PlayerUtil.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air && !landingBoostApplied && ticks > 5 && canSpeed) {
                            MoveUtil.strafe(allowedSpeed * (applyGlide.getValue() ? 1.079 : 1.0));
                            landingBoostApplied = true;
                        }
                        break;
                }

                if (frictionOverride.getValue()) {
                    if (currentSpeed < 0.1) {
                        MoveUtil.strafe(0.1);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onJump(JumpEvent event) {
        if (!isEnabled() || stopSpeed) return;
        jump++;

        if (mode.getValue() == Mode.LOW_STRAFE && modifyJumpMotion.getValue()) {
            event.setJumpMotion(0.42001f);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        assert mc.thePlayer != null;

        landingBoostApplied = false;
        isCollisionCooldownActive = mc.thePlayer.isCollidedHorizontally;

        jump = 0;
        collisionCooldownEndTick = 0;
        stopSpeed = mc.thePlayer.offGroundTicks > 2;
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopSpeed = false;
    }

    private enum Mode {MMC, STRAFE, LOW_STRAFE}
}