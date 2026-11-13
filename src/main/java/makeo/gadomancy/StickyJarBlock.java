package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import thaumcraft.essentia.JarBlockEntity;

final class StickyJarBlock extends RemoteJarBlock {
    static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final VoxelShape UP = Block.box(3, 0, 3, 13, 12, 13);
    private static final VoxelShape DOWN = Block.box(3, 4, 3, 13, 16, 13);
    private static final VoxelShape NORTH = Block.box(3, 3, 4, 13, 13, 16);
    private static final VoxelShape SOUTH = Block.box(3, 3, 0, 13, 13, 12);
    private static final VoxelShape WEST = Block.box(4, 3, 3, 16, 13, 13);
    private static final VoxelShape EAST = Block.box(0, 3, 3, 12, 13, 13);

    private final boolean remote;

    StickyJarBlock(Properties properties, boolean voidJar, boolean remote) {
        super(voidJar, properties);
        this.remote = remote;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction neck = state.getValue(FACING);
        BlockPos support = pos.relative(neck.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, neck);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
        Direction side, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (side == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, side, neighborPos, neighbor, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return remote ? new RemoteJarBlockEntity(StickyJars.REMOTE_TYPE.get(), pos, state)
            : new MountedJarBlockEntity(StickyJars.TYPE.get(), pos, state, isVoidJar());
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != (remote ? StickyJars.REMOTE_TYPE.get() : StickyJars.TYPE.get())) {
            return null;
        }
        return (world, pos, jarState, entity) -> JarBlockEntity.serverTick(world, pos, jarState, (JarBlockEntity) entity);
    }
}
