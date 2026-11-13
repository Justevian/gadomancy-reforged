package makeo.gadomancy.mixin;

import makeo.gadomancy.RunicGolemShields;
import makeo.gadomancy.SilverwoodGolems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.entity.golem.GolemBase;

@Mixin(GolemBase.class)
abstract class RunicGolemPickupMixin {
    @Inject(method = "toItem", at = @At("RETURN"), cancellable = true)
    private void gadomancy$keepShield(boolean dismantle, CallbackInfoReturnable<ItemStack> ci) {
        GolemBase golem = (GolemBase) (Object) this;
        ItemStack returned = SilverwoodGolems.pickup(golem, ci.getReturnValue());
        RunicGolemShields.preserve(golem, returned);
        if (returned != ci.getReturnValue()) {
            ci.setReturnValue(returned);
        }
    }
}
