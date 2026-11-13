package makeo.gadomancy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import thaumcraft.infusion.InfusionMatrixBlockEntity;

@Mixin(value = InfusionMatrixBlockEntity.class, remap = false)
public interface InfusionMatrixAccessor {
    @Accessor("recipeInstability")
    int gadomancy$getRecipeInstability();

    @Accessor("recipeInstability")
    void gadomancy$setRecipeInstability(int value);

    @Accessor("instability")
    void gadomancy$setInstability(int value);
}
