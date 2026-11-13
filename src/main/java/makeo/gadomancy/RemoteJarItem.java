package makeo.gadomancy;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;


final class RemoteJarItem extends MountedJarItem {
    RemoteJarItem(Block block, Item.Properties properties) {
        super(block, properties);
    }


    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof RemoteJarBlockEntity jar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && !player.isShiftKeyDown()) {
            UUID network = jar.network();
            if (network == null) {
                network = UUID.randomUUID();
                jar.setNetwork(network);
            }
            stack.set(RemoteJars.NETWORK.get(), network);
            player.sendSystemMessage(Component.translatable("message.gadomancy.remote_jar.linked", shortName(network)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.has(RemoteJars.NETWORK.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.remove(RemoteJars.NETWORK.get());
            player.sendSystemMessage(Component.translatable("message.gadomancy.remote_jar.unlinked"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        UUID network = stack.get(RemoteJars.NETWORK.get());
        if (network != null) {
            lines.accept(Component.translatable("tooltip.gadomancy.remote_jar.network", shortName(network)).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, lines, flag);
    }

    private static String shortName(UUID network) {
        return network.toString().substring(0, 8);
    }
}
