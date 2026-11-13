package makeo.gadomancy;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionEnchantmentRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class ThaumicSight {
    private static final String RESEARCH = "GADOMANCY.REVEALER";
    private static final String RECIPE = "gadomancy:thaumic_sight";
    private static final Identifier ENCHANTMENT = Identifier.parse(RECIPE);

    private ThaumicSight() {}

    public static boolean reveals(ItemStack helmet) {
        return EnchantmentHelper.getEnchantmentsForCrafting(helmet).keySet().stream()
            .anyMatch(holder -> holder.unwrapKey().filter(key -> key.identifier().equals(ENCHANTMENT)).isPresent());
    }

    static void registerResearch() {
        TcIngredient goggles = TcIngredient.item("thaumcraft:goggles_of_revealing");
        TcIngredient salis = TcIngredient.item("thaumcraft:salis_mundus");
        TcRecipes.register(new InfusionEnchantmentRecipe(RECIPE, RESEARCH, ENCHANTMENT, 4,
            new AspectList().add(Aspect.SENSES, 16).add(Aspect.MAGIC, 8), List.of(goggles, salis, salis)));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.SENSES, 4).add(Aspect.MAGIC, 4),
            1, 3, 1, TcStack.of("thaumcraft:goggles_of_revealing")
        ).setParents("INFUSIONENCHANTMENT", "GOGGLES").setConcealed().setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.REVEALER.1"), ResearchPage.infusionEnchantment(RECIPE)
        ));
    }
}
