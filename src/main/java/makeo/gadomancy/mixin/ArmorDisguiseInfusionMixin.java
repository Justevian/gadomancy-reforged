package makeo.gadomancy.mixin;

import java.util.List;
import makeo.gadomancy.ArmorDisguises;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.crafting.InfusionRecipe;

@Mixin(InfusionCrafting.class)
abstract class ArmorDisguiseInfusionMixin {
    @Inject(method = "findRecipe", at = @At("HEAD"), cancellable = true)
    private static void gadomancy$findDisguise(List<ItemStack> items, ItemStack central, Player player,
        CallbackInfoReturnable<InfusionCrafting.Plan> ci) {
        InfusionCrafting.Plan plan = ArmorDisguises.findPlan(items, central, player);
        if (plan != null) {
            ci.setReturnValue(plan);
        }
    }

    @Inject(method = "matches(Lthaumcraft/crafting/InfusionRecipe;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z",
        at = @At("HEAD"), cancellable = true)
    private static void gadomancy$ignoreDisguisePreview(InfusionRecipe recipe, List<ItemStack> items, ItemStack central,
        Player player, CallbackInfoReturnable<Boolean> ci) {
        if (ArmorDisguises.isDisguiseRecipe(recipe.key())) {
            ci.setReturnValue(false);
        }
    }
}
