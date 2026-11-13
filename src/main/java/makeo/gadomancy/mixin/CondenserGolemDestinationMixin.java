package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.entity.golem.GolemBase;

@Mixin(targets = {
    "thaumcraft.entity.golem.LogisticsGoals$EssentiaDepositGoal",
    "thaumcraft.entity.golem.LogisticsGoals$EssentiaSourceGoal"
})
abstract class CondenserGolemDestinationMixin {
    @WrapOperation(method = "canUse", at = @At(value = "INVOKE",
        target = "Lthaumcraft/entity/golem/GolemLogistics;closeTo(Lthaumcraft/entity/golem/GolemBase;Lnet/minecraft/core/BlockPos;D)Z"))
    private boolean gadomancy$reachCondenserFromGround(GolemBase golem, BlockPos pos, double distanceSquared,
        Operation<Boolean> original) {
        return original.call(golem, CondenserGolemAccess.approach(golem, pos), distanceSquared);
    }
}
