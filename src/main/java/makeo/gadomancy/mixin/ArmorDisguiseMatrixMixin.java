package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import makeo.gadomancy.ArmorDisguises;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.PedestalBlockEntity;

@Mixin(InfusionMatrixBlockEntity.class)
abstract class ArmorDisguiseMatrixMixin {
    @Shadow private String recipeKey;
    @Shadow private ItemStack recipeInput;

    @Unique private ItemStack gadomancy$consumedAppearance = ItemStack.EMPTY;

    @WrapOperation(method = "craftingStart", at = @At(value = "INVOKE",
        target = "Lthaumcraft/crafting/InfusionCrafting;findRecipe(Ljava/util/List;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lthaumcraft/crafting/InfusionCrafting$Plan;"))
    private InfusionCrafting.Plan gadomancy$resetDisguise(List<ItemStack> items, ItemStack armor, Player player,
        Operation<InfusionCrafting.Plan> operation) {
        gadomancy$consumedAppearance = ItemStack.EMPTY;
        return operation.call(items, armor, player);
    }

    @WrapOperation(method = "consumeIngredient", at = @At(value = "INVOKE",
        target = "Lthaumcraft/crafting/TcIngredient;test(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean gadomancy$matchAppearance(TcIngredient ingredient, ItemStack stack, Operation<Boolean> operation) {
        return ArmorDisguises.isAppearanceIngredient(recipeKey, ingredient)
            ? ArmorDisguises.isAppearance(recipeKey, recipeInput, stack) : operation.call(ingredient, stack);
    }

    @WrapOperation(method = "consumeIngredient", at = @At(value = "INVOKE",
        target = "Lthaumcraft/infusion/PedestalBlockEntity;setStackFromInfusion(Lnet/minecraft/world/item/ItemStack;)V"))
    private void gadomancy$recordAppearance(PedestalBlockEntity pedestal, ItemStack remainder, Operation<Void> operation) {
        ItemStack current = pedestal.getStack();
        boolean appearance = ArmorDisguises.isAppearance(recipeKey, recipeInput, current);
        operation.call(pedestal, remainder);
        if (appearance) {
            gadomancy$consumedAppearance = current.copyWithCount(1);
            ((InfusionMatrixBlockEntity) (Object) this).setChanged();
        }
    }

    @WrapOperation(method = "craftingFinish", at = @At(value = "INVOKE",
        target = "Lthaumcraft/crafting/InfusionCrafting;result(Lnet/minecraft/world/level/Level;ILjava/lang/String;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack gadomancy$applyDisguise(Level level, int type, String key, ItemStack central, Operation<ItemStack> operation) {
        if (!ArmorDisguises.isDisguiseRecipe(key)) {
            return operation.call(level, type, key, central);
        }
        ItemStack result = ArmorDisguises.apply(key, central, gadomancy$consumedAppearance);
        gadomancy$consumedAppearance = ItemStack.EMPTY;
        return result;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void gadomancy$loadAppearance(ValueInput input, CallbackInfo ci) {
        gadomancy$consumedAppearance = input.read("gadomancy_disguise", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void gadomancy$saveAppearance(ValueOutput output, CallbackInfo ci) {
        if (!gadomancy$consumedAppearance.isEmpty()) {
            output.store("gadomancy_disguise", ItemStack.OPTIONAL_CODEC, gadomancy$consumedAppearance);
        }
    }
}
