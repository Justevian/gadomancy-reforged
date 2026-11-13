package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
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
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class ArcanePackagers {
    private static final String ID = "arcane_packager";
    private static final String RECIPE = "gadomancy:" + ID;
    private static final String RESEARCH = "GADOMANCY.ARCANE_PACKAGER";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Gadomancy.MOD_ID);
    static final DeferredHolder<MenuType<?>, MenuType<PackagerMenu>> MENU =
        MENUS.register(ID, () -> Menus.simple(PackagerMenu::new));

    private static final DeferredBlock<PackagerBlock> BLOCK = BLOCKS.registerBlock(ID, PackagerBlock::new,
        properties -> properties.strength(3.5F).requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE));
    static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    static final DeferredItem<ArcanePackageItem> PACKAGE = ITEMS.registerItem("arcane_package", ArcanePackageItem::new,
        properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    static final DeferredItem<ArcanePackageItem> DISGUISED_PACKAGE = ITEMS.registerItem("disguised_package", ArcanePackageItem::new,
        properties -> properties.stacksTo(1));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackagerBlockEntity>> TYPE =
        ENTITIES.register(ID, () -> new BlockEntityType<>(PackagerBlockEntity::new, BLOCK.get()));

    private ArcanePackagers() {
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(ArcanePackagers::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, TYPE.get(),
            (entity, side) -> side != null && entity.getSlotsForFace(side).length != 0
                ? new WorldlyContainerWrapper(entity, side) : null);
    }

    static void registerResearch() {
        TcStack result = TcStack.of(RECIPE);
        TcIngredient grate = TcIngredient.item("thaumcraft:item_grate");
        TcIngredient glass = TcIngredient.item("thaumcraft:warded_glass");
        TcIngredient jar = TcIngredient.item("thaumcraft:warded_jar");
        TcRecipes.register(new ShapedArcaneRecipe(RECIPE, RESEARCH, result,
            new AspectList().add(Aspect.AIR, 100).add(Aspect.ORDER, 60).add(Aspect.ENTROPY, 50),
            3, 3, List.of(grate, TcIngredient.item("minecraft:piston"), grate,
                glass, TcIngredient.item("gadomancy:aura_core_air"), glass,
                jar, TcIngredient.item("thaumcraft:arcane_worktable"), jar
            )));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.AIR, 12).add(Aspect.MECHANISM, 16).add(Aspect.VOID, 10).add(Aspect.MAGIC, 10),
            2, 7, 2, result
        ).setParents(AuraCores.RESEARCH, "WARDEDARCANA").setWarp(1).setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.ARCANE_PACKAGER.1"),
            ResearchPage.arcane(RECIPE),
            ResearchPage.text("tc.research_page.GADOMANCY.ARCANE_PACKAGER.2")
        ));
    }
}
