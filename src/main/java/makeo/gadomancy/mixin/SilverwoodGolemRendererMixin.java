package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import makeo.gadomancy.SilverwoodGolems;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.render.entity.golem.GolemRenderState;
import thaumcraft.client.render.entity.golem.GolemRenderer;
import thaumcraft.entity.golem.GolemBase;

@Mixin(GolemRenderer.class)
abstract class SilverwoodGolemRendererMixin {
    @ModifyExpressionValue(method = "extractRenderState(Lthaumcraft/entity/golem/GolemBase;Lthaumcraft/client/render/entity/golem/GolemRenderState;F)V",
        at = @At(value = "FIELD", target = "Lthaumcraft/entity/golem/GolemType;upgrades:I"))
    private int gadomancy$showSilverwoodSlots(int original, @Local(argsOnly = true) GolemBase golem) {
        return SilverwoodGolems.isSilverwood(golem) ? 3 : original;
    }

    @Inject(method = "extractRenderState(Lthaumcraft/entity/golem/GolemBase;Lthaumcraft/client/render/entity/golem/GolemRenderState;F)V",
        at = @At("TAIL"))
    private void gadomancy$applySilverwoodTexture(GolemBase golem, GolemRenderState state, float partialTicks, CallbackInfo ci) {
        if (SilverwoodGolems.isSilverwood(golem)) {
            state.texture = Identifier.parse("gadomancy:textures/entity/golem_silverwood.png");
            state.decoration = state.decoration.replace("~", "");
        }
    }
}
