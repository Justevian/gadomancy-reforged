package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import makeo.gadomancy.SilverwoodGolems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.entity.golem.GolemBase;

@Mixin(GolemBase.class)
abstract class SilverwoodGolemStatsMixin {
    @ModifyExpressionValue(method = "configure", at = @At(value = "FIELD", target = "Lthaumcraft/entity/golem/GolemType;upgrades:I"))
    private int gadomancy$silverwoodSlots(int original) {
        return SilverwoodGolems.isSilverwood((GolemBase) (Object) this) ? 3 : original;
    }

    @ModifyExpressionValue(method = "configure", at = @At(value = "FIELD", target = "Lthaumcraft/entity/golem/GolemType;armor:I"))
    private int gadomancy$silverwoodArmor(int original) {
        return SilverwoodGolems.isSilverwood((GolemBase) (Object) this) ? 9 : original;
    }

    @ModifyExpressionValue(method = "getCarryLimit", at = @At(value = "FIELD", target = "Lthaumcraft/entity/golem/GolemType;carry:I"))
    private int gadomancy$silverwoodCapacity(int original) {
        return SilverwoodGolems.isSilverwood((GolemBase) (Object) this) ? 8 : original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "FIELD", target = "Lthaumcraft/entity/golem/GolemType;speed:F"))
    private float gadomancy$silverwoodSpeed(float original) {
        return SilverwoodGolems.isSilverwood((GolemBase) (Object) this) ? 0.38F : original;
    }
}
