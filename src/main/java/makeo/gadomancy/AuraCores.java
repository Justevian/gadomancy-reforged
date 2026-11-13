package makeo.gadomancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class AuraCores {
    static final String RESEARCH = "GADOMANCY.AURA_CORE";
    private static final String CLEAN_RESEARCH = "GADOMANCY.CLEAN_AURA_CORE";
    private static final String INFUSION = "gadomancy:aura_core";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);

    static final List<Aspect> PRIMALS = List.of(
        Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY
    );
    private static final List<String> VARIANTS = List.of("air", "fire", "water", "earth", "order", "entropy");

    static final DeferredItem<AuraCoreItem> BLANK = ITEMS.registerItem(
        "aura_core", AuraCoreItem::new, properties -> properties.stacksTo(1).rarity(Rarity.EPIC)
    );
    static final List<DeferredItem<Item>> CHARGED = VARIANTS.stream()
        .map(name -> ITEMS.registerSimpleItem("aura_core_" + name, properties -> properties.stacksTo(1).rarity(Rarity.EPIC)))
        .toList();

    private AuraCores() {
    }

    static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    static void registerResearch() {
        TcStack blank = TcStack.of(INFUSION);
        TcIngredient shard = TcIngredient.item("thaumcraft:balanced_shard");
        TcIngredient thaumium = TcIngredient.item("thaumcraft:thaumium_ingot");
        List<TcIngredient> ingredients = Collections.nCopies(4, List.of(shard, thaumium, thaumium))
            .stream().flatMap(List::stream).toList();
        TcRecipes.register(new InfusionRecipe(INFUSION, RESEARCH, blank, 6,
            new AspectList().add(Aspect.AURA, 6).add(Aspect.MAGIC, 14).add(Aspect.ELDRITCH, 12).add(Aspect.VOID, 25),
            TcIngredient.item("thaumcraft:primal_charm"), ingredients
        ));

        AspectList discovery = new AspectList().add(Aspect.AURA, 22).add(Aspect.MAGIC, 10);
        PRIMALS.forEach(aspect -> discovery.add(aspect, 8));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID, discovery, -1, 6, 2, blank)
            .setSpecial().setParents("INFUSION", "THAUMIUM").setPages(
                ResearchPage.text("tc.research_page.GADOMANCY.AURA_CORE.1"),
                ResearchPage.infusion(INFUSION),
                ResearchPage.text("tc.research_page.GADOMANCY.AURA_CORE.3")
            )
        );

        List<String> washRecipes = new ArrayList<>();
        List<String> salvageRecipes = new ArrayList<>();
        for (String variant : VARIANTS) {
            String item = "gadomancy:aura_core_" + variant;
            String wash = "gadomancy:wash_aura_core_" + variant;
            TcRecipes.register(new CrucibleRecipe(wash, CLEAN_RESEARCH, blank, TcIngredient.item(item),
                new AspectList().add(Aspect.MAGIC, 12).add(Aspect.WATER, 12).add(Aspect.HEAL, 18)));
            washRecipes.add(wash);
            if (variant.equals("earth") || variant.equals("entropy")) {
                String salvage = "gadomancy:salvage_aura_core_" + variant;
                TcRecipes.register(new CraftingRecipeRef(salvage, "", Identifier.parse(salvage), blank));
                salvageRecipes.add(salvage);
            }
        }
        ResearchCategories.addResearch(new ResearchItem(CLEAN_RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.HEAL, 8).add(Aspect.AURA, 6).add(Aspect.MAGIC, 8).add(Aspect.WATER, 10),
            1, 5, 1, blank
        ).setSecondary().setParents(RESEARCH).setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.CLEAN_AURA_CORE.1"),
            ResearchPage.crucible(washRecipes.toArray(String[]::new)),
            ResearchPage.crafting(salvageRecipes.toArray(String[]::new))
        ));
    }
}
