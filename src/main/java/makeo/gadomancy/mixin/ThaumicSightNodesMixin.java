package makeo.gadomancy.mixin;

import makeo.gadomancy.ThaumicSight;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.client.render.NodeVisibility;

@Mixin(NodeVisibility.class)
abstract class ThaumicSightNodesMixin {
    @Inject(method = "wearsRevealer", at = @At("HEAD"), cancellable = true)
    private static void gadomancy$revealNodes(LivingEntity viewer, CallbackInfoReturnable<Boolean> ci) {
        if (ThaumicSight.reveals(viewer.getItemBySlot(EquipmentSlot.HEAD))) {
            ci.setReturnValue(true);
        }
    }
}
