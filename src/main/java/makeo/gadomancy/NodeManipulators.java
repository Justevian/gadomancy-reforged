package makeo.gadomancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CompoundRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class NodeManipulators {
    static final String MUTATION = "GADOMANCY.NODE_MANIPULATOR";
    static final String GATE = "GADOMANCY.E_PORTAL_CREATOR";
    static final String GROWING = "GADOMANCY.GROWING";
    static final String GROWING_AGGRESSION = "GADOMANCY.GROWING_AGGRESSION";
    static final String GROWING_GROWTH = "GADOMANCY.GROWING_GROWTH";
    static final String GROWING_ATTACK = "GADOMANCY.GROWING_ATTACK";
    private static final String GROWING_GROWTHCLUE = "GADOMANCY.GROWING_GROWTHCLUE";
    private static final TcStack NODE = TcStack.of("thaumcraft:aura_node");
    private static final String MANIPULATOR_ID = "gadomancy:node_manipulator";
    private static final String RANDOMIZATION_ID = "gadomancy:randomization_focus";
    private static final String REDIRECTION_ID = "gadomancy:redirection_focus";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);
    static final DeferredBlock<NodeManipulatorBlock> MANIPULATOR =
        BLOCKS.registerBlock("node_manipulator", NodeManipulatorBlock::new, properties -> properties.strength(4F).noOcclusion());
    static final DeferredBlock<Block> RANDOMIZATION =
        BLOCKS.registerBlock("randomization_focus", ManipulationFocusBlock::new, properties -> properties.strength(4F).noOcclusion());
    static final DeferredBlock<Block> REDIRECTION =
        BLOCKS.registerBlock("redirection_focus", ManipulationFocusBlock::new, properties -> properties.strength(4F).noOcclusion());
    static final DeferredBlock<VoidwalkerPortalBlock> PORTAL =
        BLOCKS.registerBlock("voidwalker_portal", VoidwalkerPortalBlock::new,
            properties -> properties.noCollision().noLootTable().strength(-1F).lightLevel(state -> 11));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NodeManipulatorBlockEntity>> TYPE =
        ENTITIES.register("node_manipulator", () -> new BlockEntityType<>(NodeManipulatorBlockEntity::new, MANIPULATOR.get()));
    static final List<DeferredItem<BlockItem>> CREATIVE_ITEMS = List.of(
        ITEMS.registerSimpleBlockItem(MANIPULATOR), ITEMS.registerSimpleBlockItem(RANDOMIZATION),
        ITEMS.registerSimpleBlockItem(REDIRECTION));

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }

    static AspectList assemblyCost(boolean portal) {
        AspectList cost = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            cost.add(aspect, portal ? 150 : 100);
        }
        return cost;
    }

    static void registerResearch() {
        TcIngredient mixed = tc("mixed_crystal_cluster");
        TcIngredient seed = tc("void_seed");
        TcIngredient ancient = tc("ancient_stone");
        TcIngredient nitor = tc("nitor");
        TcIngredient charm = tc("primal_charm");
        TcIngredient voidMetal = tc("void_metal_ingot");
        TcIngredient rechargeFocus = tc("compound_recharge_focus");
        TcIngredient eye = tc("eldritch_eye");
        TcIngredient entropy = tc("entropy_crystal_cluster");
        TcRecipes.register(new InfusionRecipe(MANIPULATOR_ID, MUTATION, TcStack.of(MANIPULATOR_ID), 10,
            new AspectList().add(Aspect.AURA, 42).add(Aspect.ELDRITCH, 22).add(Aspect.MAGIC, 38)
                .add(Aspect.MECHANISM, 28).add(Aspect.DARKNESS, 14),
            tc("wand_recharge_pedestal"), List.of(tc("arcane_levitator"), mixed, seed, ancient, nitor, charm,
                tc("arcane_stone"), mixed, seed, ancient, nitor, charm)));
        TcRecipes.register(new InfusionRecipe(RANDOMIZATION_ID, MUTATION, TcStack.of(RANDOMIZATION_ID), 7,
            new AspectList().add(Aspect.ELDRITCH, 18).add(Aspect.MAGIC, 18).add(Aspect.MECHANISM, 20)
                .add(Aspect.DARKNESS, 28).add(Aspect.ORDER, 30),
            rechargeFocus, List.of(charm, voidMetal, mixed, voidMetal, charm, voidMetal, mixed, voidMetal)));
        TcRecipes.register(new InfusionRecipe(REDIRECTION_ID, GATE, TcStack.of(REDIRECTION_ID), 7,
            new AspectList().add(Aspect.ELDRITCH, 22).add(Aspect.VOID, 38).add(Aspect.MECHANISM, 30)
                .add(Aspect.DARKNESS, 28).add(Aspect.EXCHANGE, 38),
            rechargeFocus, List.of(eye, entropy, voidMetal, entropy, eye, entropy, voidMetal, entropy)));

        String mutationDiagram = registerDiagram(false);
        String portalDiagram = registerDiagram(true);
        ResearchCategories.addResearch(new ResearchItem(MUTATION, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.ELDRITCH, 8).add(Aspect.AURA, 6).add(Aspect.DARKNESS, 4)
                .add(Aspect.MAGIC, 8).add(Aspect.GREED, 4).add(Aspect.VOID, 10)
                .add(Aspect.MECHANISM, 6).add(Aspect.EXCHANGE, 4),
            -9, -4, 2, TcStack.of(MANIPULATOR_ID)
        ).setParents("VOIDMETAL", "PAVEWARD", "WANDPED", "WANDPEDFOC").setWarp(4).setSpecial().setConcealed().setPages(
            page(MUTATION), ResearchPage.infusion(MANIPULATOR_ID), page(MUTATION, 3), ResearchPage.infusion(RANDOMIZATION_ID),
            ResearchPage.compound(mutationDiagram), page(MUTATION, 6)
        ));
        ResearchCategories.addResearch(new ResearchItem(GATE, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.ELDRITCH, 6).add(Aspect.DARKNESS, 10).add(Aspect.TRAVEL, 8)
                .add(Aspect.AURA, 10).add(Aspect.VOID, 6).add(Aspect.MECHANISM, 4),
            -10, 0, 2, TcStack.of(REDIRECTION_ID)
        ).setParents(MUTATION, "OCULUS", "GADOMANCY.ESSENTIA_COMPRESSOR").setWarp(4).setSpecial().setConcealed().setPages(
            page(GATE), ResearchPage.arcane("gadomancy:ancient_pedestal"), page(GATE, 3), page(GATE, 4),
            ResearchPage.infusion(REDIRECTION_ID), page(GATE, 6), ResearchPage.compound(portalDiagram), page(GATE, 8)
        ));
        ResearchCategories.addResearch(new ResearchItem(GROWING, Gadomancy.MOD_ID, new AspectList(), -8, -1, 5, NODE)
            .setSpecial().setLost().setConcealed().setParents(MUTATION).setPages(page(GROWING), page(GROWING, 2)));
        registerObservation(GROWING_AGGRESSION, -5, 0);
        registerObservation(GROWING_GROWTH, -7, 2);
        registerObservation(GROWING_ATTACK, -6, 1);
        ResearchCategories.addResearch(new ResearchItem(GROWING_GROWTHCLUE, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.AURA, 10).add(Aspect.MAGIC, 15).add(Aspect.ELDRITCH, 5)
                .add(Aspect.VOID, 12).add(Aspect.ENERGY, 12).add(Aspect.ENTROPY, 8).add(Aspect.DEATH, 3)
                .add(Aspect.GREED, 10).add(Aspect.HUNGER, 10).add(Aspect.MECHANISM, 12).add(Aspect.DARKNESS, 12),
            -4, 3, 2, NODE
        ).setSpecial().setRound().setConcealed().setWarp(3).setParents(GROWING_AGGRESSION, GROWING_GROWTH, GROWING_ATTACK)
            .setPages(page(GROWING_GROWTHCLUE), page(GROWING_GROWTHCLUE, 2)));
    }

    private static void registerObservation(String research, int x, int y) {
        ResearchItem item = new ResearchItem(research, Gadomancy.MOD_ID, new AspectList(), x, y, 6, NODE)
            .setLost().setParents(GROWING);
        item.setPages(research.equals(GROWING_GROWTH)
            ? new ResearchPage[]{page(research), page(research, 2)} : new ResearchPage[]{page(research)});
        ResearchCategories.addResearch(item);
    }

    private static String registerDiagram(boolean portal) {
        String id = portal ? "gadomancy:voidwalker_gate_assembly" : "gadomancy:node_manipulator_assembly";
        int width = 3;
        int center = width / 2;
        int layer = width * width;
        List<TcStack> cells = new ArrayList<>(Collections.nCopies(layer * 3, TcStack.NONE));
        cells.set(center * width + center, NODE);
        cells.set(layer + center * width + center, TcStack.of(portal ? REDIRECTION_ID : RANDOMIZATION_ID));
        cells.set(2 * layer + center * width + center, TcStack.of(MANIPULATOR_ID));
        for (int z : new int[]{center - 1, center + 1}) {
            for (int x : new int[]{center - 1, center + 1}) {
                cells.set(layer + z * width + x, TcStack.of("thaumcraft:ancient_stone"));
                cells.set(2 * layer + z * width + x, TcStack.of("thaumcraft:ancient_stone_pedestal"));
            }
        }
        TcRecipes.register(new CompoundRecipe(id, portal ? GATE : MUTATION, assemblyCost(portal), width, 3, width, cells));
        return id;
    }

    private static ResearchPage page(String research) {
        return page(research, 1);
    }

    private static ResearchPage page(String research, int number) {
        return ResearchPage.text("tc.research_page." + research + "." + number);
    }

    private static TcIngredient tc(String path) {
        return TcIngredient.item("thaumcraft:" + path);
    }
}
