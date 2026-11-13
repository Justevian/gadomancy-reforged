package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class KnowledgeBooks {
    private static final String ID = "knowledge_book";
    private static final String RECIPE = "gadomancy:" + ID;
    private static final String RESEARCH = "GADOMANCY.KNOWLEDGE_BOOK";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);

    private static final DeferredBlock<KnowledgeBookBlock> BLOCK = BLOCKS.registerBlock(ID, KnowledgeBookBlock::new,
        properties -> properties.strength(0.4F).noOcclusion().lightLevel(state -> 8));
    static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnowledgeBookBlockEntity>> TYPE =
        ENTITIES.register(ID, () -> new BlockEntityType<>(KnowledgeBookBlockEntity::new, BLOCK.get()));

    private KnowledgeBooks() {
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }

    static void registerResearch() {
        TcStack book = TcStack.of(RECIPE);
        TcIngredient scribing = TcIngredient.item("thaumcraft:scribing_tools");
        TcIngredient paperBook = TcIngredient.item("minecraft:book");
        TcIngredient construct = TcIngredient.item("thaumcraft:alchemical_construct");
        TcRecipes.register(new ShapedArcaneRecipe(RECIPE, RESEARCH, book,
            new AspectList().add(Aspect.ORDER, 70).add(Aspect.ENTROPY, 20).add(Aspect.AIR, 20).add(Aspect.FIRE, 30),
            3, 3, List.of(scribing, TcIngredient.item("thaumcraft:thaumonomicon"), scribing,
                paperBook, TcIngredient.item("gadomancy:aura_core_order"), paperBook,
                construct, TcIngredient.item("thaumcraft:goggles_of_revealing"), construct
            )));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.MIND, 12).add(Aspect.MAGIC, 8).add(Aspect.MOTION, 6)
                .add(Aspect.ORDER, 10).add(Aspect.MECHANISM, 8).add(Aspect.CRAFT, 8),
            -2, 8, 2, book
        ).setParents(AuraCores.RESEARCH, "GOGGLES", "THAUMATORIUM").setWarp(1).setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.KNOWLEDGE_BOOK.1"),
            ResearchPage.arcane(RECIPE),
            ResearchPage.text("tc.research_page.GADOMANCY.KNOWLEDGE_BOOK.3"),
            ResearchPage.text("tc.research_page.GADOMANCY.KNOWLEDGE_BOOK.4")
        ));
    }
}
