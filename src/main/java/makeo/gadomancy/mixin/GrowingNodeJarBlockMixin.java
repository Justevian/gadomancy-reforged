package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import makeo.gadomancy.GrowingNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.aspect.AspectList;
import thaumcraft.block.NodeJarBlock;
import thaumcraft.blockentity.NodeJarBlockEntity;
import thaumcraft.node.Node;
import thaumcraft.node.NodeBlockEntity;

@Mixin(NodeJarBlock.class)
abstract class GrowingNodeJarBlockMixin {
    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void gadomancy$placeGrowingJar(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack, CallbackInfo ci) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NodeJarBlockEntity jar) {
            GrowingNodes.readJar(stack, jar);
        }
    }

    @Inject(method = "onWandUseOn", at = @At("HEAD"))
    private void gadomancy$rememberCapacity(Level level, BlockPos pos, Player player, ItemStack wand, Direction side,
        CallbackInfoReturnable<InteractionResult> ci, @Share("growingCapacity") LocalRef<AspectList> capacity) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NodeJarBlockEntity jar && GrowingNodes.isGrowing(jar)) {
            capacity.set(jar.getAspectsBase().copy());
        }
    }

    @WrapOperation(method = "onWandUseOn", at = @At(value = "INVOKE",
        target = "Lthaumcraft/node/Node;setAspects(Lthaumcraft/aspect/AspectList;)V"))
    private void gadomancy$restoreCapacity(Node target, AspectList vis, Operation<Void> operation,
        @Share("growingCapacity") LocalRef<AspectList> capacity) {
        operation.call(target, vis);
        if (capacity.get() != null && target instanceof NodeBlockEntity node) {
            GrowingNodes.markGrowing(node);
            GrowingNodes.copyCapacity(capacity.get(), node);
        }
    }
}
