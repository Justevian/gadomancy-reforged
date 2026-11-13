package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.essentia.EssentiaTransport;

abstract class AuraStructureBlockEntity extends TcBlockEntity implements EssentiaTransport {
    AuraStructureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    abstract void serverTick(ServerLevel server);

    abstract void onDisassembled(ServerLevel server);

    final boolean isActive(AuraStructureBlock.Kind kind) {
        BlockState state = getBlockState();
        return level != null && state.getValue(AuraStructureBlock.PART) == kind.controllerPart
            && AuraStructureBlock.valid(level, AuraStructureBlock.base(worldPosition, state), kind);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server && state.getBlock() instanceof AuraStructureBlock block
            && state.getValue(AuraStructureBlock.PART) == block.kind.controllerPart) {
            onDisassembled(server);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setSuction(@Nullable Aspect aspect, int amount) {
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
