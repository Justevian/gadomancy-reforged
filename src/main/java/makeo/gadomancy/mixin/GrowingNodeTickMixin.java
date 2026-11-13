package makeo.gadomancy.mixin;

import makeo.gadomancy.GrowingNodes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.node.NodeBlockEntity;

@Mixin(NodeBlockEntity.class)
abstract class GrowingNodeTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void gadomancy$tickGrowingNode(Level level, CallbackInfo ci) {
        if (level instanceof ServerLevel server) {
            GrowingNodes.tick((NodeBlockEntity) (Object) this, server);
        }
    }
}
