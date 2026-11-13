package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import makeo.gadomancy.GrowingNodes;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.NodeJarBlockEntity;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;

@Mixin(NodeJarBlockEntity.class)
abstract class GrowingNodeJarDropMixin {
    @WrapOperation(method = "preRemoveSideEffects", at = @At(value = "INVOKE",
        target = "Lthaumcraft/item/NodeJarItem;create(Lthaumcraft/aspect/AspectList;Lthaumcraft/node/NodeType;Lthaumcraft/node/NodeModifier;Ljava/lang/String;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack gadomancy$dropGrowingJar(AspectList aspects, NodeType type, @Nullable NodeModifier modifier,
        String id, Operation<ItemStack> operation) {
        ItemStack stack = operation.call(aspects, type, modifier, id);
        NodeJarBlockEntity jar = (NodeJarBlockEntity) (Object) this;
        if (GrowingNodes.isGrowing(jar)) {
            GrowingNodes.copyToItem(stack, jar);
        }
        return stack;
    }
}
