package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.wand.Wands;

final class InfusionClawBlock extends Block implements EntityBlock {
    InfusionClawBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfusionClawBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != InfusionClaws.TYPE.get()) {
            return null;
        }
        return (world, pos, current, entity) -> ((InfusionClawBlockEntity) entity).tick((ServerLevel) world);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof InfusionClawBlockEntity claw) {
            claw.claim(player);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        if (!Wands.isWand(held) || !(level.getBlockEntity(pos) instanceof InfusionClawBlockEntity claw) || !claw.canInsert()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            claw.claim(player);
            claw.setItem(0, held.copyWithCount(1));
            held.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof InfusionClawBlockEntity claw)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            claw.claim(player);
            if (player.isShiftKeyDown()) {
                claw.toggleEmptyOnly(player);
            } else if (!claw.isRunning() && !claw.isEmpty()) {
                ItemStack wand = claw.removeItem(0, 1);
                if (!player.addItem(wand)) {
                    net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), wand);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos.below()) instanceof InfusionMatrixBlockEntity matrix && matrix.isCrafting() ? 15 : 0;
    }
}
