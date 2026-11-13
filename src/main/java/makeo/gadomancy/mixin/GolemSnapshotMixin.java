package makeo.gadomancy.mixin;

import makeo.gadomancy.GolemCores;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import thaumcraft.entity.golem.GolemSnapshot;

@Mixin(GolemSnapshot.class)
abstract class GolemSnapshotMixin {
    @ModifyArg(method = "lambda$static$0", at = @At(value = "INVOKE",
        target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;", ordinal = 0), index = 1)
    private static int gadomancy$extendCoreRange(int original) {
        return Math.max(original, GolemCores.BODYGUARD);
    }
}
