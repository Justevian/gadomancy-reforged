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
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemSnapshot;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.item.golem.GolemPlacerItem;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class SilverwoodGolems {
    static final String ID = "gadomancy:silverwood_golem";
    static final String RESEARCH = "GADOMANCY.GOLEMSILVERWOOD";
    private static final String MARKER = "~";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    static final DeferredItem<GolemPlacerItem> ITEM = ITEMS.registerItem("silverwood_golem",
        properties -> new GolemPlacerItem(properties, GolemType.WOOD));

    private SilverwoodGolems() {}

    static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    static void registerResearch() {
        TcIngredient salis = TcIngredient.item("thaumcraft:salis_mundus");
        TcIngredient balanced = TcIngredient.item("thaumcraft:balanced_shard");
        TcIngredient quicksilver = TcIngredient.item("thaumcraft:quicksilver");
        TcStack result = TcStack.of(ID);
        TcRecipes.register(new InfusionRecipe(ID, RESEARCH, result, 7,
            new AspectList().add(Aspect.MOTION, 16).add(Aspect.MAGIC, 16).add(Aspect.MAN, 16).add(Aspect.SENSES, 16),
            TcIngredient.item("thaumcraft:silverwood_log"),
            List.of(TcIngredient.item("thaumcraft:primal_charm"), salis, balanced, quicksilver, salis, balanced, quicksilver)));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.MOTION, 10).add(Aspect.TREE, 10).add(Aspect.MAGIC, 8)
                .add(Aspect.ORDER, 8).add(Aspect.FLESH, 6).add(Aspect.EXCHANGE, 5),
            1, -2, 3, result
        ).setParents("GOLEMTALLOW").setConcealed().setPages(
            ResearchPage.text("tc.research_page." + RESEARCH + ".1"), ResearchPage.infusion(ID)
        ));
    }

    public static boolean isSilverwood(GolemBase golem) {
        return golem.getGolemType() == GolemType.WOOD && golem.getGolemDecoration().contains(MARKER);
    }

    public static GolemSnapshot placedSnapshot(ItemStack stack, GolemSnapshot snapshot) {
        if (!stack.is(ITEM.get()) || snapshot.decoration().contains(MARKER)) {
            return snapshot;
        }
        return new GolemSnapshot(snapshot.core(), snapshot.advanced(), snapshot.upgrades(), snapshot.decoration() + MARKER,
            snapshot.markers(), snapshot.filters(), snapshot.colors(), snapshot.toggles());
    }

    public static ItemStack pickup(GolemBase golem, ItemStack stack) {
        return isSilverwood(golem) ? stack.transmuteCopy(ITEM.get()) : stack;
    }
}
