package dev.revere.valance.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import static dev.revere.valance.util.MinecraftUtil.mc;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
public final class PlayerUtil {

    private PlayerUtil() {
    }

    /**
     * Checks if the player is currently inside any liquid block.
     */
    public static boolean isInLiquid() {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return false;
        for (int x = MathHelper.floor_double(player.getEntityBoundingBox().minX); x < MathHelper.floor_double(player.getEntityBoundingBox().maxX) + 1; ++x) {
            for (int z = MathHelper.floor_double(player.getEntityBoundingBox().minZ); z < MathHelper.floor_double(player.getEntityBoundingBox().maxZ) + 1; ++z) {
                BlockPos pos = new BlockPos(x, (int) player.getEntityBoundingBox().minY, z);
                Block block = mc().theWorld.getBlockState(pos).getBlock();
                if (block != null && !(block instanceof BlockAir)) {
                    if (!(block instanceof BlockLiquid)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the block relative to the player's position.
     */
    public static Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null || mc().theWorld == null) return null;
        return mc().theWorld.getBlockState(new BlockPos(player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ)).getBlock();
    }

    /**
     * Checks if there is a solid block directly over the player's head within a certain height.
     */
    public static boolean isBlockOver(double maxHeight, boolean checkLiquid) {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null || mc().theWorld == null) return false;

        AxisAlignedBB bb = player.getEntityBoundingBox();
        for (double yOff = bb.maxY + 0.01; yOff < player.posY + maxHeight; yOff += 0.5) {
            for (double xOff = bb.minX; xOff < bb.maxX; xOff += (bb.maxX - bb.minX) / 2.0) {
                for (double zOff = bb.minZ; zOff < bb.maxZ; zOff += (bb.maxZ - bb.minZ) / 2.0) {
                    BlockPos checkPos = new BlockPos(xOff, yOff, zOff);
                    Block block = mc().theWorld.getBlockState(checkPos).getBlock();
                    if (block != null && !(block instanceof BlockAir)) {
                        if (!checkLiquid && block instanceof BlockLiquid) {
                            continue;
                        }
                        AxisAlignedBB blockBB = block.getCollisionBoundingBox(mc().theWorld, checkPos, mc().theWorld.getBlockState(checkPos));
                        if (blockBB != null && blockBB.maxY >= yOff) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
