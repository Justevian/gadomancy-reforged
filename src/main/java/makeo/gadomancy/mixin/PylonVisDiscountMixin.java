package makeo.gadomancy.mixin;

import makeo.gadomancy.PylonStatusEffects;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.aspect.Aspect;
import thaumcraft.wand.WandManager;

@Mixin(WandManager.class)
abstract class PylonVisDiscountMixin {
    @Inject(method = "getTotalVisDiscount", at = @At("RETURN"), cancellable = true)
    private static void gadomancy$applyVisDiscount(@Nullable Player player, @Nullable Aspect aspect,
        CallbackInfoReturnable<Float> ci) {
        var effect = player == null ? null : player.getEffect(PylonStatusEffects.VIS_DISCOUNT);
        if (effect != null) {
            ci.setReturnValue(ci.getReturnValueF() + (effect.getAmplifier() + 1) * 0.08F);
        }
    }
}
