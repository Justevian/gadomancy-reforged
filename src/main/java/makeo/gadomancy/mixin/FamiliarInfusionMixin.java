package makeo.gadomancy.mixin;

import makeo.gadomancy.FamiliarRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.crafting.InfusionCrafting;

@Mixin(InfusionCrafting.class)
abstract class FamiliarInfusionMixin {
    @Inject(method = "result", at = @At("HEAD"), cancellable = true)
    private static void gadomancy$upgradeFamiliar(Level level, int type, String key, ItemStack central,
        CallbackInfoReturnable<ItemStack> ci) {
        ItemStack upgraded = FamiliarRecipes.upgraded(key, central);
        if (upgraded != null) {
            ci.setReturnValue(upgraded);
        }
    }
}
