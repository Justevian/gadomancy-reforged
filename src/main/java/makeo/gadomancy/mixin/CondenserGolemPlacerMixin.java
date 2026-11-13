package makeo.gadomancy.mixin;

import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.item.golem.GolemPlacerItem;

@Mixin(GolemPlacerItem.class)
abstract class CondenserGolemPlacerMixin {
    @ModifyVariable(method = "useOn", at = @At("HEAD"), argsOnly = true)
    private UseOnContext gadomancy$placeAlchemyGolemAtBase(UseOnContext context) {
        if (!CondenserGolemAccess.isCondenser(context.getLevel(), context.getClickedPos())
            || GolemKeys.snapshot(context.getItemInHand()).core() != CondenserGolemAccess.ALCHEMY_CORE) {
            return context;
        }
        BlockPos controller = CondenserGolemAccess.controller(context.getLevel(), context.getClickedPos());
        return controller == null ? context : CondenserGolemAccess.clickAt(context, controller.below());
    }
}
