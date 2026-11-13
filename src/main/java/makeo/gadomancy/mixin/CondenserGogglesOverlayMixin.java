package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import makeo.gadomancy.CondenserGolemAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.client.render.AspectContainerOverlay;

@Mixin(AspectContainerOverlay.class)
abstract class CondenserGogglesOverlayMixin {
    @WrapOperation(method = "onSubmit", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static @Nullable BlockEntity gadomancy$showContentsForAnyCondenserSection(ClientLevel level, BlockPos pos,
        Operation<BlockEntity> original) {
        BlockPos controller = CondenserGolemAccess.controller(level, pos);
        return original.call(level, controller != null ? controller : pos);
    }
}
