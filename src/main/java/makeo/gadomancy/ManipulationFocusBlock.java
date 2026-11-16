package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.wand.WandInteractable;

final class ManipulationFocusBlock extends Block implements WandInteractable {
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 6, 13);

    ManipulationFocusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos,
        Player player, ItemStack stack, Direction side) {
        return NodeManipulatorBlock.useManipulator(level, pos.below(), player, stack);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
