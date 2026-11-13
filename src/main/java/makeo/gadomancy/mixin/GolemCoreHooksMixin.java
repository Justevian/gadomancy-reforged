package makeo.gadomancy.mixin;

import makeo.gadomancy.GolemBodyguardGoals;
import makeo.gadomancy.GolemBreakGoal;
import makeo.gadomancy.GolemCores;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.WorkGoals;

@Mixin(WorkGoals.class)
abstract class GolemCoreHooksMixin {
    @Inject(method = "install", at = @At("TAIL"))
    private static void gadomancy$installCore(GolemBase golem, GoalSelector goals, GoalSelector targets, CallbackInfo ci) {
        if (golem.getCore() == GolemCores.BREAK) {
            goals.addGoal(2, new GolemBreakGoal(golem));
        } else if (golem.getCore() == GolemCores.BODYGUARD) {
            GolemBodyguardGoals.install(golem, goals, targets);
        }
    }
}
