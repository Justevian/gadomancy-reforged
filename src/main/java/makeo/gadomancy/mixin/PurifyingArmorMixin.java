package makeo.gadomancy.mixin;

import makeo.gadomancy.ArmorDisguises;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.block.PurifyingFluidBlock;

@Mixin(PurifyingFluidBlock.class)
abstract class PurifyingArmorMixin {
    @Inject(method = "entityInside", at = @At("HEAD"))
    private void gadomancy$washDisguise(BlockState state, Level level, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effects, boolean precise, CallbackInfo ci) {
        if (level instanceof ServerLevel server && entity instanceof ItemEntity item && item.isAlive()) {
            ArmorDisguises.purify(server, pos, item);
        }
    }
}
