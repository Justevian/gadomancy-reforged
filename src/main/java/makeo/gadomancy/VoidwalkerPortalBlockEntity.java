package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class VoidwalkerPortalBlockEntity extends BlockEntity {
    private int openTicks;

    VoidwalkerPortalBlockEntity(BlockPos pos, BlockState state) {
        super(NodeManipulators.PORTAL_TYPE.get(), pos, state);
    }

    void animate() {
        if (openTicks < 30) openTicks++;
    }

    int openTicks() { return openTicks; }
}
