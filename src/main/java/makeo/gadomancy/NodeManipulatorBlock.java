package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import thaumcraft.research.ResearchGate;
import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;

final class NodeManipulatorBlock extends Block implements EntityBlock, WandInteractable {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 4, 16),
        box(2, 4, 2, 14, 8, 14), box(4, 8, 4, 12, 16, 12));

    NodeManipulatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NodeManipulatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != NodeManipulators.TYPE.get()) {
            return null;
        }
        return (world, pos, current, entity) -> ((NodeManipulatorBlockEntity) entity).tick((ServerLevel) world);
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack stack, Direction side) {
        if (!(stack.getItem() instanceof WandItem wand)
            || !(level.getBlockEntity(pos) instanceof NodeManipulatorBlockEntity manipulator) || manipulator.isAssembled()) {
            return InteractionResult.PASS;
        }
        boolean portal = level.getBlockState(pos.above()).is(NodeManipulators.REDIRECTION.get());
        if (!ResearchGate.isComplete(player, portal ? NodeManipulators.GATE : NodeManipulators.MUTATION)
            || !manipulator.matches(level, portal, false)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            if (!manipulator.mayOperate(server, player, portal)
                || !wand.consumeAllVisCrafting(stack, player, NodeManipulators.assemblyCost(portal), true)) {
                return InteractionResult.FAIL;
            }
            manipulator.assemble(player, portal);
            player.sendSystemMessage(Component.translatable("message.gadomancy.node_manipulator.assembled"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!Wands.isWand(held) || !(level.getBlockEntity(pos) instanceof NodeManipulatorBlockEntity manipulator)
            || !manipulator.canInsert(player)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && server.mayInteract(player, pos)) {
            manipulator.insert(held.copyWithCount(1));
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof NodeManipulatorBlockEntity manipulator) || !manipulator.canRemove(player)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && server.mayInteract(player, pos)) {
            ItemStack wand = manipulator.removeWand();
            if (!player.addItem(wand)) {
                Containers.dropItemStack(server, player.getX(), player.getY(), player.getZ(), wand);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
