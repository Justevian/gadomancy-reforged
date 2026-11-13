package makeo.gadomancy;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.node.NodeType;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

@Mod(Gadomancy.MOD_ID)
public final class Gadomancy {
    public static final String MOD_ID = "gadomancy";
    private static final String RESEARCH = "GADOMANCY.ELEMENT_DARKNESS";
    private static final String RECIPE = "gadomancy:element_darkness";
    private static final String DROPPER_RESEARCH = "GADOMANCY.ARCANEDROPPER";
    private static final String DROPPER_RECIPE = "gadomancy:arcane_dropper";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    private static final DeferredItem<DarknessElementItem> DARKNESS = ITEMS.registerItem(
        "element_darkness", DarknessElementItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );

    private static final DeferredBlock<ArcaneDropperBlock> DROPPER = BLOCKS.registerBlock(
        "arcane_dropper", ArcaneDropperBlock::new,
        properties -> properties.strength(3.5F).requiresCorrectToolForDrops().noOcclusion()
    );
    private static final DeferredItem<BlockItem> DROPPER_ITEM = ITEMS.registerSimpleBlockItem(DROPPER);

    private static final List<DeferredItem<CreativeNodeItem>> CREATIVE_NODES = Arrays.stream(NodeType.values())
        .map(type -> ITEMS.registerItem("creative_node_" + type.getSerializedName(),
            properties -> new CreativeNodeItem(properties, type)))
        .toList();

    static {
        TABS.register("gadomancy", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gadomancy"))
            .icon(() -> new ItemStack(DARKNESS.get()))
            .displayItems((parameters, output) -> {
                output.accept(DARKNESS.get());
                output.accept(DROPPER_ITEM.get());
                output.accept(InfusionClaws.ITEM.get());
                output.accept(EnervationEmitters.ITEM.get());
                output.accept(KnowledgeBooks.ITEM.get());
                output.accept(ArcanePackagers.ITEM.get());
                output.accept(RemoteJars.ITEM.get());
                StickyJars.CREATIVE_ITEMS.forEach(jar -> output.accept(jar.get()));
                output.accept(AuraCores.BLANK.get());
                AuraCores.CHARGED.forEach(core -> output.accept(core.get()));
                AuraStructures.CREATIVE_ITEMS.forEach(part -> output.accept(part.get()));
                NodeManipulators.CREATIVE_ITEMS.forEach(part -> output.accept(part.get()));
                output.accept(GolemCores.BREAK_ITEM.get());
                output.accept(GolemCores.BODYGUARD_ITEM.get());
                output.accept(EtherealFamiliarItem.ITEM.get());
                output.accept(SilverwoodGolems.ITEM.get());
                CREATIVE_NODES.forEach(node -> output.accept(node.get()));
            })
            .build());
    }

    public Gadomancy(IEventBus bus) {
        BLOCKS.register(bus);
        RemoteJars.register(bus);
        StickyJars.register(bus);
        InfusionClaws.register(bus);
        EnervationEmitters.register(bus);
        KnowledgeBooks.register(bus);
        ArcanePackagers.register(bus);
        AuraCores.register(bus);
        AuraStructures.register(bus);
        PylonStatusEffects.register(bus);
        NodeManipulators.register(bus);
        ArmorDisguises.register(bus);
        GolemCores.register(bus);
        EtherealFamiliarItem.register(bus);
        SilverwoodGolems.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::registerCapabilities);
    }

    static Item darknessItem() {
        return DARKNESS.get();
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, blockEntity, side) -> {
            Direction facing = state.getValue(ArcaneDropperBlock.FACING);
            Direction.Axis flatAxis = facing.getAxis().isVertical() ? Direction.Axis.Z : Direction.Axis.Y;
            if (level instanceof ServerLevel server
                && (side == null || side == facing.getOpposite() || side.getAxis() == flatAxis)) {
                return new ArcaneDropperTransfer(server, pos, facing);
            }
            return null;
        }, DROPPER.get());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TcStack result = TcStack.of(RECIPE);
            TcIngredient alumentum = TcIngredient.item("thaumcraft:alumentum");
            TcIngredient voidMetal = TcIngredient.item("thaumcraft:void_metal_ingot");
            TcIngredient salisMundus = TcIngredient.item("thaumcraft:salis_mundus");

            TcRecipes.register(new InfusionRecipe(
                RECIPE, RESEARCH, result, 6,
                new AspectList().add(Aspect.VOID, 16).add(Aspect.DARKNESS, 13)
                    .add(Aspect.ELDRITCH, 15).add(Aspect.AURA, 7),
                TcIngredient.item("thaumcraft:void_seed"),
                List.of(alumentum, voidMetal, voidMetal, salisMundus, alumentum, voidMetal, voidMetal, salisMundus)
            ));

            ResearchCategories.registerCategory(
                MOD_ID,
                Identifier.fromNamespaceAndPath(MOD_ID, "textures/item/category_icon.png"),
                Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/gui_researchback.png")
            );
            ResearchCategories.addResearch(new ResearchItem(
                RESEARCH, MOD_ID,
                new AspectList().add(Aspect.DARKNESS, 5).add(Aspect.VOID, 5).add(Aspect.ELDRITCH, 3),
                0, 0, 2, result
            ).setParents("VOIDMETAL").setParentsHidden("INFUSION").setPages(
                ResearchPage.text("tc.research_page.GADOMANCY.ELEMENT_DARKNESS.1"),
                ResearchPage.infusion(RECIPE)
            ));
            registerDropperResearch();
            RemoteJars.registerResearch();
            StickyJars.registerResearch();
            InfusionClaws.registerResearch();
            EnervationEmitters.registerResearch();
            KnowledgeBooks.registerResearch();
            ArcanePackagers.registerResearch();
            AuraCores.registerResearch();
            AuraStructures.registerResearch();
            NodeManipulators.registerResearch();
            ThaumicSight.registerResearch();
            ArmorDisguises.registerResearch();
            GolemCores.registerResearch();
            FamiliarRecipes.register();
            SilverwoodGolems.registerResearch();
            RunicGolemShields.registerResearch();
        });
    }

    private static void registerDropperResearch() {
        TcStack result = TcStack.of(DROPPER_RECIPE);
        TcIngredient stone = TcIngredient.item("thaumcraft:arcane_stone_bricks");
        TcIngredient bellows = TcIngredient.item("thaumcraft:arcane_bellows");
        TcRecipes.register(new ShapedArcaneRecipe(
            DROPPER_RECIPE, DROPPER_RESEARCH, result,
            new AspectList().add(Aspect.ORDER, 14).add(Aspect.AIR, 14).add(Aspect.EARTH, 10),
            3, 3, List.of(
                stone, TcIngredient.item("minecraft:dropper"), stone,
                bellows, TcIngredient.item("thaumcraft:arcane_levitator"), bellows,
                stone, stone, stone
            )
        ));
        ResearchCategories.addResearch(new ResearchItem(
            DROPPER_RESEARCH, MOD_ID,
            new AspectList().add(Aspect.ORDER, 4).add(Aspect.AIR, 6).add(Aspect.EARTH, 5)
                .add(Aspect.SENSES, 5).add(Aspect.MECHANISM, 7),
            -2, -2, 2, result
        ).setParents("DISTILESSENTIA", "BELLOWS").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.ARCANEDROPPER.1"),
            ResearchPage.arcane(DROPPER_RECIPE)
        ));
    }
}
