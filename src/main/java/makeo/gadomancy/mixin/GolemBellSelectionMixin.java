package makeo.gadomancy.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import thaumcraft.item.golem.GolemBellItem;

@Mixin(GolemBellItem.class)
abstract class GolemBellSelectionMixin {
    @ModifyVariable(method = "interactLivingEntity", at = @At("HEAD"), argsOnly = true)
    private ItemStack gadomancy$selectOnHeldBell(ItemStack stack, ItemStack argument, Player player,
        LivingEntity target, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Creative entity interaction passes a copy. Bell selection must persist on the held item.
        return held.getItem() == stack.getItem() ? held : stack;
    }
}
