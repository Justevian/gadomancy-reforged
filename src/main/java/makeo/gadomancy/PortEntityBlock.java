package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Runs removal effects only when a block is replaced, never on chunk unload. */
class PortEntityBlock extends Block {
    PortEntityBlock(Properties properties) { super(properties); }
    interface Removal { void onBlockRemoved(BlockPos pos, BlockState state); }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof Removal removal) {
            removal.onBlockRemoved(pos, state);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
