package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import makeo.gadomancy.RunicGolemShields;
import makeo.gadomancy.SilverwoodGolems;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemSnapshot;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.item.golem.GolemPlacerItem;

@Mixin(GolemPlacerItem.class)
abstract class RunicGolemPlacerMixin {
    @WrapOperation(method = "useOn", at = @At(value = "INVOKE",
        target = "Lthaumcraft/entity/golem/GolemBase;configure(Lthaumcraft/entity/golem/GolemType;Lthaumcraft/entity/golem/GolemSnapshot;)V"))
    private void gadomancy$restoreShield(GolemBase golem, GolemType type, GolemSnapshot snapshot, Operation<Void> original,
        @Local(argsOnly = true) UseOnContext context) {
        original.call(golem, type, SilverwoodGolems.placedSnapshot(context.getItemInHand(), snapshot));
        RunicGolemShields.equip(golem, context.getItemInHand());
    }
}
