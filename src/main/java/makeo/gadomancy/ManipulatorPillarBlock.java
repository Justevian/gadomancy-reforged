package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import thaumcraft.registry.ModBlocks;

final class ManipulatorPillarBlock extends Block {
    private final boolean top;

    ManipulatorPillarBlock(Properties properties, boolean top) {
        super(properties);
        this.top = top;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server) server.scheduleTick(pos, this, 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos foot = top ? pos.below() : pos;
        boolean unloaded = false;
        for (BlockPos corner : NodeManipulatorBlockEntity.CORNERS) {
            BlockPos center = foot.subtract(corner);
            if (!level.hasChunkAt(center)) unloaded = true;
            else if (level.getBlockEntity(center) instanceof NodeManipulatorBlockEntity manipulator && manipulator.isAssembled()) {
                level.scheduleTick(pos, this, 120);
                return;
            }
        }
        if (unloaded) level.scheduleTick(pos, this, 120);
        else level.setBlockAndUpdate(pos, originalBlock().defaultBlockState());
    }

    private Block originalBlock() {
        return top ? ModBlocks.ANCIENT_STONE.get() : ModBlocks.ANCIENT_STONE_PEDESTAL.get();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(originalBlock()));
    }
}
