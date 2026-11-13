package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.infusion.PedestalBlock;
import thaumcraft.infusion.PedestalBlockEntity;

final class AncientPedestalBlock extends PedestalBlock {
    AncientPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AncientPedestalBlockEntity(pos, state);
    }

    static final class AncientPedestalBlockEntity extends PedestalBlockEntity {
        AncientPedestalBlockEntity(BlockPos pos, BlockState state) {
            super(AuraStructures.PEDESTAL_TYPE.get(), pos, state);
        }
    }
}
