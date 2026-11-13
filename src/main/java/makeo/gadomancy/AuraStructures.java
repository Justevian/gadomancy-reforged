package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CompoundRecipe;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class AuraStructures {
    private static final String CONDENSER_RESEARCH = "GADOMANCY.ESSENTIA_COMPRESSOR";
    private static final String PYLON_RESEARCH = "GADOMANCY.AURA_PYLON";
    private static final String CONDENSER_ID = "essentia_condenser";
    private static final String PYLON_ID = "aura_pylon";
    private static final String CAP_ID = "aura_pylon_cap";
    private static final String PEDESTAL_ID = "ancient_pedestal";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);

    static final DeferredBlock<AuraStructureBlock> CONDENSER = structure(CONDENSER_ID, AuraStructureBlock.Kind.CONDENSER);
    static final DeferredBlock<AuraStructureBlock> PYLON = structure(PYLON_ID, AuraStructureBlock.Kind.PYLON);
    static final DeferredBlock<AuraStructureBlock> CAP = structure(CAP_ID, AuraStructureBlock.Kind.PYLON);
    static final DeferredBlock<AncientPedestalBlock> ANCIENT_PEDESTAL = BLOCKS.registerBlock(
        PEDESTAL_ID, AncientPedestalBlock::new, properties -> properties.strength(3.5F).requiresCorrectToolForDrops()
    );

    static final List<DeferredItem<BlockItem>> CREATIVE_ITEMS = List.of(
        ITEMS.registerSimpleBlockItem(CONDENSER), ITEMS.registerSimpleBlockItem(PYLON),
        ITEMS.registerSimpleBlockItem(CAP), ITEMS.registerSimpleBlockItem(ANCIENT_PEDESTAL)
    );

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER_TYPE =
        BLOCK_ENTITIES.register(CONDENSER_ID, () -> new BlockEntityType<>(CondenserBlockEntity::new, CONDENSER.get()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PylonBlockEntity>> PYLON_TYPE =
        BLOCK_ENTITIES.register(PYLON_ID, () -> new BlockEntityType<>(PylonBlockEntity::new, PYLON.get(), CAP.get()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncientPedestalBlock.AncientPedestalBlockEntity>> PEDESTAL_TYPE =
        BLOCK_ENTITIES.register(PEDESTAL_ID, () -> new BlockEntityType<>(AncientPedestalBlock.AncientPedestalBlockEntity::new, ANCIENT_PEDESTAL.get()));

    private AuraStructures() {
    }

    private static DeferredBlock<AuraStructureBlock> structure(String id, AuraStructureBlock.Kind kind) {
        return BLOCKS.registerBlock(id, properties -> new AuraStructureBlock(properties, kind),
            properties -> properties.strength(3.5F, 15).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
                .lightLevel(state -> state.getValue(AuraStructureBlock.PART) > 0 ? 7 : 0));
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    static Block part(AuraStructureBlock.Kind kind, int index) {
        if (kind == AuraStructureBlock.Kind.CONDENSER) {
            return CONDENSER.get();
        }
        return index == 3 ? CAP.get() : PYLON.get();
    }

    static String research(AuraStructureBlock.Kind kind) {
        return kind == AuraStructureBlock.Kind.CONDENSER ? CONDENSER_RESEARCH : PYLON_RESEARCH;
    }

    static AspectList assemblyCost(AuraStructureBlock.Kind kind) {
        if (kind == AuraStructureBlock.Kind.CONDENSER) {
            return new AspectList().add(Aspect.WATER, 140).add(Aspect.EARTH, 80).add(Aspect.AIR, 150)
                .add(Aspect.ORDER, 20).add(Aspect.ENTROPY, 140);
        }
        AspectList cost = new AspectList();
        AuraCores.PRIMALS.forEach(aspect -> cost.add(aspect, 150));
        return cost;
    }

    static void registerResearch() {
        registerCondenser();
        registerPylon();
        registerAncientStones();
    }

    private static void registerAncientStones() {
        String key = "GADOMANCY.ANCIENT_STONES";
        String stone = "gadomancy:ancient_stones";
        String pedestal = "gadomancy:ancient_stone_pedestal";
        TcIngredient arcaneStone = TcIngredient.item("thaumcraft:arcane_stone_bricks");
        TcStack result = TcStack.of("thaumcraft:ancient_stone", 4);
        TcRecipes.register(new ShapedArcaneRecipe(stone, key, result,
            new AspectList().add(Aspect.ENTROPY, 16).add(Aspect.EARTH, 8),
            3, 3, List.of(arcaneStone, arcaneStone, arcaneStone,
                arcaneStone, TcIngredient.item("thaumcraft:eldritch_eye"), arcaneStone,
                arcaneStone, arcaneStone, arcaneStone
            )));
        TcRecipes.register(new CrucibleRecipe(pedestal, key, TcStack.of("thaumcraft:ancient_stone_pedestal"),
            TcIngredient.item("thaumcraft:arcane_pedestal"),
            new AspectList().add(Aspect.ELDRITCH, 8).add(Aspect.EXCHANGE, 8).add(Aspect.ENTROPY, 12)
        ));
        ResearchCategories.addResearch(new ResearchItem(key, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.EARTH, 8)
                .add(Aspect.EXCHANGE, 6).add(Aspect.ENTROPY, 8),
            -12, 1, 3, TcStack.of("thaumcraft:ancient_stone")
        ).setParents("OCULUS").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.ANCIENT_STONES.1"),
            ResearchPage.arcane(stone),
            ResearchPage.crucible(pedestal)
        ));
    }

    private static void registerCondenser() {
        String id = "gadomancy:" + CONDENSER_ID;
        String assembly = id + "_assembly";
        String pedestal = "gadomancy:" + PEDESTAL_ID;
        TcIngredient log = TcIngredient.item("thaumcraft:greatwood_log");
        TcIngredient buffer = TcIngredient.item("thaumcraft:essentia_buffer");
        TcRecipes.register(new ShapedArcaneRecipe(id, CONDENSER_RESEARCH, TcStack.of(id, 3),
            new AspectList().add(Aspect.WATER, 190).add(Aspect.ORDER, 150).add(Aspect.ENTROPY, 120)
                .add(Aspect.FIRE, 100).add(Aspect.AIR, 160).add(Aspect.EARTH, 60),
            3, 3, List.of(
                log, TcIngredient.item("thaumcraft:order_crystal_cluster"), log,
                buffer, TcIngredient.item("gadomancy:element_darkness"), buffer,
                log, TcIngredient.item("gadomancy:aura_core_water"), log
            )));
        TcIngredient stone = TcIngredient.item("thaumcraft:ancient_stone");
        TcIngredient slab = TcIngredient.item("thaumcraft:ancient_stone_pedestal");
        TcIngredient empty = TcIngredient.EMPTY;
        TcRecipes.register(new ShapedArcaneRecipe(pedestal, CONDENSER_RESEARCH, TcStack.of(pedestal),
            new AspectList().add(Aspect.ENTROPY, 25).add(Aspect.ORDER, 25),
            3, 3, List.of(stone, slab, stone, empty, stone, empty, stone, slab, stone)));
        TcStack section = TcStack.of(id);
        TcRecipes.register(new CompoundRecipe(assembly, CONDENSER_RESEARCH,
            assemblyCost(AuraStructureBlock.Kind.CONDENSER), 1, 3, 1, List.of(section, section, section)));
        ResearchCategories.addResearch(new ResearchItem(CONDENSER_RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.VOID, 12).add(Aspect.WATER, 10).add(Aspect.ENTROPY, 12)
                .add(Aspect.MAGIC, 6).add(Aspect.MECHANISM, 12),
            -4, 7, 2, section
        ).setParents(AuraCores.RESEARCH, "ESSENTIARESERVOIR", "GADOMANCY.ELEMENT_DARKNESS").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.ESSENTIA_COMPRESSOR.1"),
            ResearchPage.arcane(id), ResearchPage.compound(assembly),
            ResearchPage.text("tc.research_page.GADOMANCY.ESSENTIA_COMPRESSOR.2"),
            ResearchPage.arcane(pedestal),
            ResearchPage.text("tc.research_page.GADOMANCY.ESSENTIA_COMPRESSOR.4"),
            ResearchPage.text("tc.research_page.GADOMANCY.ESSENTIA_COMPRESSOR.6"),
            ResearchPage.text("tc.research_page.GADOMANCY.ESSENTIA_COMPRESSOR.8"),
            ResearchPage.text("tc.research_page.GADOMANCY.ESSENTIA_COMPRESSOR.9")
        ));
    }

    private static void registerPylon() {
        String id = "gadomancy:" + PYLON_ID;
        String cap = "gadomancy:" + CAP_ID;
        String assembly = id + "_assembly";
        TcIngredient buffer = TcIngredient.item("thaumcraft:essentia_buffer");
        TcIngredient log = TcIngredient.item("thaumcraft:silverwood_log");
        TcIngredient empty = TcIngredient.EMPTY;
        TcRecipes.register(new ShapedArcaneRecipe(id, PYLON_RESEARCH, TcStack.of(id),
            new AspectList().add(Aspect.ORDER, 80).add(Aspect.WATER, 50).add(Aspect.FIRE, 50).add(Aspect.AIR, 50),
            3, 3, List.of(
                buffer, log, buffer, log, empty, log, buffer, TcIngredient.item("thaumcraft:warded_jar"), buffer
            )));
        TcIngredient alumentum = TcIngredient.item("thaumcraft:alumentum");
        TcRecipes.register(new ShapedArcaneRecipe(cap, PYLON_RESEARCH, TcStack.of(cap),
            new AspectList().add(Aspect.ORDER, 90).add(Aspect.FIRE, 120).add(Aspect.AIR, 70),
            3, 3, List.of(
                empty, TcIngredient.item("thaumcraft:nitor"), empty,
                alumentum, TcIngredient.item("gadomancy:aura_core_fire"), alumentum,
                empty, TcIngredient.item(id), empty
            )));
        TcStack section = TcStack.of(id);
        TcStack crown = TcStack.of(cap);
        TcRecipes.register(new CompoundRecipe(assembly, PYLON_RESEARCH,
            assemblyCost(AuraStructureBlock.Kind.PYLON), 1, 4, 1, List.of(crown, section, section, section)));
        ResearchCategories.addResearch(new ResearchItem(PYLON_RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.ORDER, 12).add(Aspect.AURA, 20).add(Aspect.MAGIC, 12).add(Aspect.MECHANISM, 8),
            0, 9, 2, crown
        ).setParents(AuraCores.RESEARCH, "TUBEFILTER").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.AURA_PYLON.1"),
            ResearchPage.arcane(id, cap), ResearchPage.compound(assembly),
            ResearchPage.text("tc.research_page.GADOMANCY.AURA_PYLON.4")
        ));
        ResearchCategories.addResearch(new ResearchItem("GADOMANCY.AURA_EFFECTS", Gadomancy.MOD_ID,
            new AspectList().add(Aspect.AURA, 6).add(Aspect.MIND, 8).add(Aspect.MAGIC, 4).add(Aspect.ORDER, 4),
            1, 10, 2, crown
        ).setConcealed().setSecondary().setParents(PYLON_RESEARCH).setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.AURA_EFFECTS.1")
        ));
    }
}
