package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import thaumcraft.infusion.PedestalBlockEntity;
import thaumcraft.registry.ModItems;

final class KnowledgeBookBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 1, 1, 15, 8, 15);

    KnowledgeBookBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof PedestalBlockEntity && level.getBlockState(pos.above()).isAir();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
        Direction side, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (side.getAxis().isVertical() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, side, neighborPos, neighbor, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnowledgeBookBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != KnowledgeBooks.TYPE.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (world, pos, current, entity) -> ((KnowledgeBookBlockEntity) entity).animateBook(world);
        }
        return (world, pos, current, entity) -> ((KnowledgeBookBlockEntity) entity).tick((ServerLevel) world);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof KnowledgeBookBlockEntity book && book.isWorking()) {
            if (level instanceof ServerLevel server && server.mayInteract(player, pos)) {
                book.returnNote(server, false);
            }
            return InteractionResult.SUCCESS;
        }
        return ModItems.THAUMONOMICON.get().use(level, player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof KnowledgeBookBlockEntity book ? book.signal() : 0;
    }
}
