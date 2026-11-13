package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.entity.golem.GolemBase;

@Mixin(targets = "thaumcraft.entity.golem.LogisticsGoals$EssentiaGatherGoal")
abstract class CondenserGolemGatherMixin {
    @WrapOperation(method = {"canUse", "start"}, at = @At(value = "FIELD",
        target = "Lnet/minecraft/core/Direction;UP:Lnet/minecraft/core/Direction;"))
    private Direction gadomancy$useMountedJarPort(Operation<Direction> original, @Local BlockEntity entity) {
        return CondenserGolemAccess.jarPort(entity, original.call());
    }

    @WrapOperation(method = "canUse", at = @At(value = "INVOKE",
        target = "Lthaumcraft/entity/golem/GolemLogistics;homeContainer(Lthaumcraft/entity/golem/GolemBase;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos gadomancy$resolveCondenserHome(GolemBase golem, Operation<BlockPos> original) {
        BlockPos home = original.call(golem);
        BlockPos controller = CondenserGolemAccess.controller(golem.level(), home);
        return controller != null && CondenserGolemAccess.canBind(golem.level(), controller, golem.getHomeFacing())
            ? controller : home;
    }

    @WrapOperation(method = "canUse", at = @At(value = "INVOKE",
        target = "Lthaumcraft/entity/golem/GolemLogistics;closeTo(Lthaumcraft/entity/golem/GolemBase;Lnet/minecraft/core/BlockPos;D)Z"))
    private boolean gadomancy$reachRaisedCondenser(GolemBase golem, BlockPos home, double distanceSquared,
        Operation<Boolean> original) {
        if (CondenserGolemAccess.canBind(golem.level(), home, golem.getHomeFacing())
            || CondenserGolemAccess.isEssentiaSource(golem.level(), home)) {
            return original.call(golem, golem.getHomePosition(), distanceSquared);
        }
        return original.call(golem, home, distanceSquared);
    }
}
