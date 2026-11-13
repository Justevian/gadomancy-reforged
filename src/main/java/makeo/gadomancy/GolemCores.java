package makeo.gadomancy;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.item.golem.GolemPartItem;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class GolemCores {
    public static final int BREAK = 12;
    public static final int BODYGUARD = 13;
    private static final String BREAK_RESEARCH = "GADOMANCY.GOLEMCOREBREAK";
    private static final String BODYGUARD_RESEARCH = "GADOMANCY.GOLEMCOREBODYGUARD";
    private static final String BREAK_ID = "golem_core_break";
    private static final String BODYGUARD_ID = "golem_core_bodyguard";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    static final DeferredItem<GolemPartItem> BREAK_ITEM = ITEMS.registerItem(BREAK_ID,
        properties -> new GolemPartItem(properties, GolemPartItem.Kind.CORE, BREAK));
    static final DeferredItem<GolemPartItem> BODYGUARD_ITEM = ITEMS.registerItem(BODYGUARD_ID,
        properties -> new GolemPartItem(properties, GolemPartItem.Kind.CORE, BODYGUARD));

    private GolemCores() {}

    static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static ItemStack coreStack(int index) {
        return switch (index) {
            case BREAK -> new ItemStack(BREAK_ITEM.get());
            case BODYGUARD -> new ItemStack(BODYGUARD_ITEM.get());
            default -> ItemStack.EMPTY;
        };
    }

    static void registerResearch() {
        String breaking = "gadomancy:" + BREAK_ID;
        String guard = "gadomancy:" + BODYGUARD_ID;
        TcIngredient pearl = TcIngredient.item("minecraft:ender_pearl");
        TcRecipes.register(new InfusionRecipe(breaking, BREAK_RESEARCH, TcStack.of(breaking), 3,
            new AspectList().add(Aspect.ENTROPY, 16).add(Aspect.TOOL, 24).add(Aspect.MECHANISM, 16),
            tc("golem_core_harvest"), List.of(tc("elemental_shovel"), tc("elemental_pickaxe"), tc("elemental_axe"))));
        TcRecipes.register(new InfusionRecipe(guard, BODYGUARD_RESEARCH, TcStack.of(guard), 3,
            new AspectList().add(Aspect.TOOL, 28).add(Aspect.MECHANISM, 20).add(Aspect.WEAPON, 10).add(Aspect.ARMOR, 16),
            tc("golem_core_guard"), List.of(tc("boots_traveller"), pearl, tc("elemental_sword"), pearl)));
        ResearchCategories.addResearch(new ResearchItem(BREAK_RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.TOOL, 8).add(Aspect.ENTROPY, 8).add(Aspect.MECHANISM, 8),
            3, -3, 3, TcStack.of(breaking)
        ).setParents("COREHARVEST", "ELEMENTALAXE", "ELEMENTALPICK", "ELEMENTALSHOVEL").setConcealed()
            .setPages(ResearchPage.text("tc.research_page." + BREAK_RESEARCH + ".1"), ResearchPage.infusion(breaking)));
        ResearchCategories.addResearch(new ResearchItem(BODYGUARD_RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.TOOL, 8).add(Aspect.ORDER, 8).add(Aspect.MECHANISM, 10)
                .add(Aspect.ARMOR, 8).add(Aspect.WEAPON, 8),
            5, -3, 2, TcStack.of(guard)
        ).setParents("COREGUARD", "ELEMENTALSWORD", "BOOTSTRAVELLER").setConcealed()
            .setPages(ResearchPage.text("tc.research_page." + BODYGUARD_RESEARCH + ".1"), ResearchPage.infusion(guard)));
    }

    private static TcIngredient tc(String id) {
        return TcIngredient.item("thaumcraft:" + id);
    }
}
