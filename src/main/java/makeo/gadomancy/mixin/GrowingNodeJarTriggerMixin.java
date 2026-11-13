package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import makeo.gadomancy.GrowingNodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.NodeJarBlockEntity;
import thaumcraft.blockentity.NodeJarTrigger;
import thaumcraft.node.Node;
import thaumcraft.node.NodeBlockEntity;

@Mixin(NodeJarTrigger.class)
abstract class GrowingNodeJarTriggerMixin {
    @WrapOperation(method = "replace", at = @At(value = "INVOKE",
        target = "Lthaumcraft/node/Node;setAspects(Lthaumcraft/aspect/AspectList;)V"))
    private static void gadomancy$rememberCapacity(Node source, AspectList vis, Operation<Void> operation,
        @Share("growingCapacity") LocalRef<AspectList> capacity) {
        if (source instanceof NodeBlockEntity node && GrowingNodes.isGrowing(node)) {
            capacity.set(node.getAspectsBase().copy());
        }
        operation.call(source, vis);
    }

    @WrapOperation(method = "replace", at = @At(value = "INVOKE",
        target = "Lthaumcraft/blockentity/NodeJarBlockEntity;setAspects(Lthaumcraft/aspect/AspectList;)V"))
    private static void gadomancy$jarGrowingNode(NodeJarBlockEntity jar, AspectList vis, Operation<Void> operation,
        @Share("growingCapacity") LocalRef<AspectList> capacity) {
        operation.call(jar, vis);
        if (capacity.get() != null) {
            GrowingNodes.markGrowing(jar);
            GrowingNodes.copyCapacity(capacity.get(), jar);
        }
    }
}
