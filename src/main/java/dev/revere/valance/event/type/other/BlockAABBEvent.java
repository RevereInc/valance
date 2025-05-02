package dev.revere.valance.event.type.other;

import dev.revere.valance.event.Cancellable;
import dev.revere.valance.event.IEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@Getter
@Setter
public class BlockAABBEvent implements IEvent, Cancellable {
    private final World world;
    private final Block block;
    private final BlockPos blockPos;
    private AxisAlignedBB boundingBox;
    private final AxisAlignedBB maskBoundingBox;
    private boolean cancelled = false;

    public BlockAABBEvent(World world, Block block, BlockPos blockPos, AxisAlignedBB boundingBox, AxisAlignedBB maskBoundingBox) {
        this.world = world;
        this.block = block;
        this.blockPos = blockPos;
        this.boundingBox = boundingBox;
        this.maskBoundingBox = maskBoundingBox;
    }
}