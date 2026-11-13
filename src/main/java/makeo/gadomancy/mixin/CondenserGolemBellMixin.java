package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.item.golem.GolemBellItem;

@Mixin(GolemBellItem.class)
abstract class CondenserGolemBellMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void gadomancy$rejectUnassembledCondenser(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci) {
        if (isAlchemyCondenserClick(context)
            && CondenserGolemAccess.controller(context.getLevel(), context.getClickedPos()) == null) {
            ci.setReturnValue(InteractionResult.FAIL);
        }
    }

    @ModifyVariable(method = "useOn", at = @At("HEAD"), argsOnly = true)
    private UseOnContext gadomancy$selectCondenserController(UseOnContext context) {
        if (!isAlchemyCondenserClick(context)) {
            return context;
        }
        BlockPos controller = CondenserGolemAccess.controller(context.getLevel(), context.getClickedPos());
        return controller == null ? context : CondenserGolemAccess.clickAt(context, controller);
    }

    @WrapOperation(method = "useOn", at = @At(value = "INVOKE",
        target = "Lthaumcraft/platform/Transfer;hasItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean gadomancy$allowCondenserHome(Level level, BlockPos pos, Direction side, Operation<Boolean> original,
        @Local GolemBase golem) {
        return golem.getCore() == CondenserGolemAccess.ALCHEMY_CORE
            && (CondenserGolemAccess.canBind(level, pos, side) || CondenserGolemAccess.isEssentiaSource(level, pos))
            || original.call(level, pos, side);
    }

    @WrapOperation(method = "useOn", at = @At(value = "INVOKE",
        target = "Lthaumcraft/entity/golem/GolemBase;setHome(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"))
    private void gadomancy$standAtCondenserBase(GolemBase golem, BlockPos home, Direction side, Operation<Void> original) {
        if (golem.getCore() == CondenserGolemAccess.ALCHEMY_CORE
            && CondenserGolemAccess.canBind(golem.level(), home.relative(side.getOpposite()), side)) {
            home = home.below();
        }
        original.call(golem, home, side);
    }

    private static boolean isAlchemyCondenserClick(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null
            || !CondenserGolemAccess.isCondenser(level, context.getClickedPos())) {
            return false;
        }
        var selection = GolemKeys.BELL.get(context.getItemInHand());
        return selection != null && level.getEntity(selection.entity()) instanceof GolemBase golem
            && golem.getCore() == CondenserGolemAccess.ALCHEMY_CORE;
    }
}
