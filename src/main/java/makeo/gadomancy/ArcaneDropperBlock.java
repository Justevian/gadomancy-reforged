package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.Wands;

final class ArcaneDropperBlock extends Block implements WandInteractable {
    static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    ArcaneDropperBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        if (level instanceof ServerLevel server && !previous.is(this)) {
            server.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.updateNeighbourForOutputSignal(pos, this);
        level.scheduleTick(pos, this, 20);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        List<ItemEntity> items = items(level, pos, state.getValue(FACING));
        double fullness = 0;
        int stacks = 0;
        for (ItemEntity item : items) {
            if (!item.getItem().isEmpty()) {
                fullness += (double) item.getItem().getCount() / item.getItem().getMaxStackSize();
                stacks++;
            }
        }
        return stacks == 0 ? 0 : 1 + (int) (14 * fullness / (stacks + ArcaneDropperTransfer.OUTPUT_SLOTS));
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack wand, Direction side) {
        if (Wands.getFocus(wand) != null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Direction facing = level.getBlockState(pos).getValue(FACING);
            Vec3 center = Vec3.atCenterOf(pos.relative(facing, 2));
            for (int corner = 0; corner < 8; corner++) {
                double x = center.x + ((corner & 1) == 0 ? -1.5 : 1.5);
                double y = center.y + ((corner & 2) == 0 ? -1.5 : 1.5);
                double z = center.z + ((corner & 4) == 0 ? -1.5 : 1.5);
                server.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
        }
        return InteractionResult.SUCCESS;
    }

    static List<ItemEntity> items(Level level, BlockPos pos, Direction facing) {
        Vec3 center = Vec3.atCenterOf(pos.relative(facing, 2));
        return level.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(center, 3, 3, 3), ItemEntity::isAlive);
    }
}
