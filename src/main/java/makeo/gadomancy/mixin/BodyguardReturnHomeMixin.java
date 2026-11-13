package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import makeo.gadomancy.GolemBodyguardGoals;
import makeo.gadomancy.GolemCores;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.entity.golem.GolemBase;

@Mixin(GolemBase.class)
abstract class BodyguardReturnHomeMixin {
    @WrapOperation(method = "rebuildGoals", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V"))
    private void gadomancy$skipBodyguardHomeGoal(GoalSelector selector, int priority, Goal goal, Operation<Void> operation) {
        GolemBase golem = (GolemBase) (Object) this;
        if (priority != 6 || golem.getCore() != GolemCores.BODYGUARD) {
            operation.call(selector, priority, goal);
        }
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lthaumcraft/entity/golem/GolemBase;returnHome()V"))
    private void gadomancy$stayWithOwner(GolemBase golem, Operation<Void> operation) {
        if (golem.getCore() != GolemCores.BODYGUARD || golem.isInWall() || !GolemBodyguardGoals.followingOwner(golem)) {
            operation.call(golem);
        }
    }
}
