package makeo.gadomancy.mixin;

import makeo.gadomancy.GolemCores;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.item.golem.GolemItems;

@Mixin(GolemItems.class)
abstract class GolemCoreItemMixin {
    @Inject(method = "core", at = @At("HEAD"), cancellable = true)
    private static void gadomancy$recoverCore(int index, CallbackInfoReturnable<ItemStack> ci) {
        ItemStack core = GolemCores.coreStack(index);
        if (!core.isEmpty()) {
            ci.setReturnValue(core);
        }
    }
}
