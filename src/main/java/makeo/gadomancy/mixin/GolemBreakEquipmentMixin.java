package makeo.gadomancy.mixin;

import makeo.gadomancy.GolemCores;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.entity.golem.GolemBase;

@Mixin(GolemBase.class)
abstract class GolemBreakEquipmentMixin {
    @Inject(method = "hasInventory", at = @At("HEAD"), cancellable = true)
    private static void gadomancy$enableBreakFilters(int core, CallbackInfoReturnable<Boolean> ci) {
        if (core == GolemCores.BREAK) {
            ci.setReturnValue(true);
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void gadomancy$giveMiningTool(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        GolemBase golem = (GolemBase) (Object) this;
        ItemStack held = player.getItemInHand(hand);
        if (golem.getCore() != GolemCores.BREAK || !player.isShiftKeyDown()
            || !(held.has(DataComponents.TOOL) || held.isEmpty() && !golem.getCarried().isEmpty())) {
            return;
        }
        if (golem.level() instanceof ServerLevel level) {
            if (!player.getUUID().equals(golem.getOwnerUUID())) {
                ci.setReturnValue(InteractionResult.FAIL);
                return;
            }
            ItemStack old = golem.getCarried();
            golem.setCarried(held.isEmpty() ? ItemStack.EMPTY : held.split(1));
            if (!old.isEmpty() && !player.addItem(old)) {
                golem.spawnAtLocation(level, old);
            }
        }
        ci.setReturnValue(InteractionResult.SUCCESS);
    }
}
