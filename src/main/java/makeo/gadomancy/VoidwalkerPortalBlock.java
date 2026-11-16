package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.eldritch.EldritchPortalTravel;
import thaumcraft.research.ResearchGate;

final class VoidwalkerPortalBlock extends Block implements EntityBlock {
    VoidwalkerPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VoidwalkerPortalBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || type != NodeManipulators.PORTAL_TYPE.get()) return null;
        return (world, pos, current, entity) -> ((VoidwalkerPortalBlockEntity) entity).animate();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) server.scheduleTick(pos, this, 5);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Gates saved before the renderer was added have no block entity.
        if (level.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) == null) {
            level.getBlockEntity(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(0.5, 1, 0.5))) {
            if (tryEnter(level, pos, player)) return;
        }
        level.scheduleTick(pos, this, 5);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effects, boolean intersects) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            tryEnter(server, pos, player);
        }
    }

    boolean tryEnter(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (player.getPortalCooldown() != 0 || !level.getBlockState(pos).is(this)
            || (!player.isCreative() && !ResearchGate.isComplete(player, NodeManipulators.GATE))) return false;
        if (!EldritchPortalTravel.tryTravel(level, pos, player)) return false;
        level.removeBlock(pos, false);
        return true;
    }
}
