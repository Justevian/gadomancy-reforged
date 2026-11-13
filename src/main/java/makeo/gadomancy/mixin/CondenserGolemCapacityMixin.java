package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.EssentiaTransport;

@Mixin(targets = "thaumcraft.entity.golem.LogisticsGoals")
abstract class CondenserGolemCapacityMixin {
    @WrapOperation(method = "findEssentiaDestination", at = @At(value = "INVOKE",
        target = "Lthaumcraft/essentia/EssentiaTransport;canInputFrom(Lnet/minecraft/core/Direction;)Z"))
    private static boolean gadomancy$skipFullCondenser(EssentiaTransport transport, Direction side,
        Operation<Boolean> original, @Local Aspect aspect) {
        return original.call(transport, side) && CondenserGolemAccess.canAcceptEssentia(transport, aspect);
    }
}
