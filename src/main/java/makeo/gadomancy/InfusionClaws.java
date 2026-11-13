package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class InfusionClaws {
    private static final String ID = "infusion_claw";
    private static final String RECIPE = "gadomancy:" + ID;
    private static final String RESEARCH = "GADOMANCY.INFUSIONCLAW";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);

    private static final DeferredBlock<InfusionClawBlock> BLOCK = BLOCKS.registerBlock(
        ID, InfusionClawBlock::new, properties -> properties.strength(3.5F).noOcclusion()
            .requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE)
    );
    static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionClawBlockEntity>> TYPE =
        BLOCK_ENTITIES.register(ID, () -> new BlockEntityType<>(InfusionClawBlockEntity::new, BLOCK.get()));

    private InfusionClaws() {
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(InfusionClaws::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, TYPE.get(),
            (entity, side) -> side == Direction.UP ? new WorldlyContainerWrapper(entity, side) : null);
    }

    static void registerResearch() {
        TcIngredient stone = TcIngredient.item("thaumcraft:arcane_stone_bricks");
        TcIngredient metal = TcIngredient.item("thaumcraft:void_metal_ingot");
        TcStack result = TcStack.of(RECIPE);
        TcRecipes.register(new InfusionRecipe(RECIPE, RESEARCH, result, 10,
            new AspectList().add(Aspect.ELDRITCH, 25).add(Aspect.MECHANISM, 20).add(Aspect.MAGIC, 16)
                .add(Aspect.ORDER, 20).add(Aspect.DARKNESS, 12),
            TcIngredient.item("thaumcraft:wand_recharge_pedestal"),
            List.of(TcIngredient.item("thaumcraft:focus_primal"), stone, TcIngredient.item("thaumcraft:golem_core_use"),
                metal, stone, TcIngredient.item("thaumcraft:primal_charm"), stone,
                TcIngredient.item("thaumcraft:zombie_brain"), metal, stone)
        ));
        ResearchCategories.addResearch(new ResearchItem(
            RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.ELDRITCH, 8).add(Aspect.MECHANISM, 8).add(Aspect.MAGIC, 8)
                .add(Aspect.ORDER, 8).add(Aspect.DARKNESS, 4),
            -6, -7, 3, result
        ).setParents("FOCUSPRIMAL", "VOIDMETAL", "WANDPED", "COREUSE").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.INFUSIONCLAW.1"),
            ResearchPage.infusion(RECIPE),
            ResearchPage.text("tc.research_page.GADOMANCY.INFUSIONCLAW.2")
        ));
    }
}
