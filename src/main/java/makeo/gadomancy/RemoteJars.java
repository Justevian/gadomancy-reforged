package makeo.gadomancy;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
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

final class RemoteJars {
    private static final String ID = "remote_jar";
    private static final String RECIPE = "gadomancy:remote_jar";
    private static final String RESEARCH = "GADOMANCY.REMOTEJAR";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Gadomancy.MOD_ID);

    static final DeferredBlock<RemoteJarBlock> BLOCK = BLOCKS.registerBlock(ID, RemoteJarBlock::new,
        properties -> properties.strength(0.3F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 9)
            .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos, box) -> false)
    );
    static final DeferredItem<RemoteJarItem> ITEM = ITEMS.registerItem(ID,
        properties -> new RemoteJarItem(BLOCK.get(), properties), properties -> properties.rarity(Rarity.UNCOMMON).stacksTo(1)
    );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RemoteJarBlockEntity>> TYPE =
        BLOCK_ENTITIES.register(ID, () -> new BlockEntityType<>(RemoteJarBlockEntity::new, BLOCK.get()));
    static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> NETWORK =
        COMPONENTS.register("remote_jar_network",
            () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    private RemoteJars() {
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        COMPONENTS.register(bus);
    }

    static void registerResearch() {
        TcIngredient greatwood = TcIngredient.item("thaumcraft:greatwood_log");
        TcStack result = TcStack.of(RECIPE);
        TcRecipes.register(new ShapedArcaneRecipe(RECIPE, RESEARCH, result,
            new AspectList().add(Aspect.WATER, 10).add(Aspect.EARTH, 10).add(Aspect.ORDER, 10),
            3, 2, List.of(
                greatwood, TcIngredient.item("thaumcraft:warded_jar"), greatwood,
                greatwood, TcIngredient.item("thaumcraft:essentia_mirror"), greatwood
            )
        ));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.WATER, 4).add(Aspect.MECHANISM, 8).add(Aspect.EARTH, 4).add(Aspect.ORDER, 8),
            -4, -3, 3, result
        ).setParents("JARLABEL", "MIRRORESSENTIA").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.REMOTEJAR.1"),
            ResearchPage.arcane(RECIPE),
            ResearchPage.text("tc.research_page.GADOMANCY.REMOTEJAR.2")
        ));
    }
}
