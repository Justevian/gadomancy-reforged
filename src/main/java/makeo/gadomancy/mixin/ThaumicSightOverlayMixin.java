package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import makeo.gadomancy.ThaumicSight;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.client.render.AspectContainerOverlay;
import thaumcraft.client.render.device.NoteOverlay;
import thaumcraft.registry.ModItems;

@Mixin({AspectContainerOverlay.class, NoteOverlay.class})
abstract class ThaumicSightOverlayMixin {
    @WrapOperation(method = "onSubmit", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private static Item gadomancy$revealAspectContents(ItemStack head, Operation<Item> original) {
        return ThaumicSight.reveals(head) ? ModItems.GOGGLES_OF_REVEALING.get() : original.call(head);
    }
}
