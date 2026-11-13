package makeo.gadomancy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

final class ArcanePackageItem extends Item {
    ArcanePackageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || contents.size() == 0) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            stack.shrink(1);
            contents.nonEmptyItemCopyStream().forEach(item -> {
                if (!player.addItem(item)) {
                    Containers.dropItemStack(server, player.getX(), player.getY(), player.getZ(), item);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
