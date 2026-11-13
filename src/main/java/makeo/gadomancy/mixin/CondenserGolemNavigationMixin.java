package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.entity.golem.GolemBase;

@Mixin(targets = "thaumcraft.entity.golem.LogisticsGoals$EssentiaSourceGoal")
abstract class CondenserGolemNavigationMixin {
    @Shadow @Final private GolemBase golem;

    @WrapOperation(method = "start", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"))
    private boolean gadomancy$pathAlongCondenserBase(PathNavigation navigation, double x, double y, double z, double speed,
        Operation<Boolean> original) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockPos approach = CondenserGolemAccess.approach(golem, pos);
        if (!approach.equals(pos)) {
            return original.call(navigation, approach.getX() + 0.5, (double) approach.getY(), approach.getZ() + 0.5, speed);
        }
        return original.call(navigation, x, y, z, speed);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
        target = "Lthaumcraft/entity/golem/GolemLogistics;closeTo(Lthaumcraft/entity/golem/GolemBase;Lnet/minecraft/core/BlockPos;D)Z"))
    private boolean gadomancy$stopAtCondenserApproach(GolemBase worker, BlockPos pos, double distanceSquared,
        Operation<Boolean> original) {
        return original.call(worker, CondenserGolemAccess.approach(worker, pos), distanceSquared);
    }
}