package makeo.gadomancy;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ArcaneCrafting;
import thaumcraft.crafting.DynamicArcaneRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.essentia.EssentiaKeys;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchGate;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class StickyJars {
    private static final String RESEARCH = "GADOMANCY.STICKYJAR";
    private static final String WARDED_ID = "sticky_warded_jar";
    private static final String VOID_ID = "sticky_void_jar";
    private static final String REMOTE_ID = "sticky_remote_jar";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);

    private static final DeferredBlock<StickyJarBlock> WARDED = block(WARDED_ID, false, false);
    private static final DeferredBlock<StickyJarBlock> VOID = block(VOID_ID, true, false);
    private static final DeferredBlock<StickyJarBlock> REMOTE = block(REMOTE_ID, false, true);
    private static final DeferredItem<MountedJarItem> WARDED_ITEM = ITEMS.registerItem(WARDED_ID,
        properties -> new MountedJarItem(WARDED.get(), properties), properties -> properties.stacksTo(1));
    private static final DeferredItem<MountedJarItem> VOID_ITEM = ITEMS.registerItem(VOID_ID,
        properties -> new MountedJarItem(VOID.get(), properties), properties -> properties.stacksTo(1));
    private static final DeferredItem<RemoteJarItem> REMOTE_ITEM = ITEMS.registerItem(REMOTE_ID,
        properties -> new RemoteJarItem(REMOTE.get(), properties),
        properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    static final List<DeferredItem<? extends MountedJarItem>> CREATIVE_ITEMS =
        List.of(WARDED_ITEM, VOID_ITEM, REMOTE_ITEM);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MountedJarBlockEntity>> TYPE =
        BLOCK_ENTITIES.register("sticky_jar", () -> new BlockEntityType<>(
            (pos, state) -> new MountedJarBlockEntity(StickyJars.TYPE.get(), pos, state, state.is(VOID.get())),
            WARDED.get(), VOID.get()
        ));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RemoteJarBlockEntity>> REMOTE_TYPE =
        BLOCK_ENTITIES.register(REMOTE_ID, () -> new BlockEntityType<>(
            (pos, state) -> new RemoteJarBlockEntity(StickyJars.REMOTE_TYPE.get(), pos, state), REMOTE.get()
        ));

    private StickyJars() {
    }

    private static DeferredBlock<StickyJarBlock> block(String id, boolean voidJar, boolean remote) {
        return BLOCKS.registerBlock(id, properties -> new StickyJarBlock(properties, voidJar, remote),
            properties -> properties.strength(0.3F).sound(SoundType.GLASS).noOcclusion());
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    static void registerResearch() {
        ArcaneCrafting.registerDynamic(new StickyJarRecipe());
        registerPreview(WARDED_ID, "thaumcraft:warded_jar");
        registerPreview(VOID_ID, "thaumcraft:void_jar");
        registerPreview(REMOTE_ID, "gadomancy:remote_jar");
        ResearchCategories.addResearch(new ResearchItem(
            RESEARCH, Gadomancy.MOD_ID, new AspectList().add(Aspect.SLIME, 8).add(Aspect.EARTH, 8),
            -5, -5, 2, TcStack.of("gadomancy:" + WARDED_ID)
        ).setParents("JARLABEL").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.STICKYJAR.1"),
            ResearchPage.arcane("gadomancy:" + WARDED_ID, "gadomancy:" + VOID_ID, "gadomancy:" + REMOTE_ID)
        ));
    }

    private static void registerPreview(String id, String ingredient) {
        String key = "gadomancy:" + id;
        TcRecipes.register(new ShapedArcaneRecipe(key, RESEARCH, TcStack.of(key), cost(), 1, 2,
            List.of(TcIngredient.item(ingredient), TcIngredient.item("minecraft:slime_ball"))));
    }

    private static AspectList cost() {
        return new AspectList().add(Aspect.WATER, 10).add(Aspect.EARTH, 10);
    }

    private static @Nullable Item resultItem(ItemStack stack) {
        if (stack.is(ModBlocks.WARDED_JAR.get().asItem()) || stack.is(ModBlocks.FILLED_JAR.get())) {
            return WARDED_ITEM.get();
        }
        if (stack.is(ModBlocks.VOID_JAR.get().asItem()) || stack.is(ModBlocks.FILLED_VOID_JAR.get())) {
            return VOID_ITEM.get();
        }
        return stack.is(RemoteJars.ITEM.get()) ? REMOTE_ITEM.get() : null;
    }

    private static final class StickyJarRecipe implements DynamicArcaneRecipe {
        private static final int WIDTH = ArcaneCrafting.GRID_SIZE;

        @Override
        public boolean matches(List<ItemStack> grid, Player player) {
            return ResearchGate.isComplete(player, RESEARCH) && !jar(grid).isEmpty();
        }

        @Override
        public ItemStack result(List<ItemStack> grid) {
            ItemStack source = jar(grid);
            Item item = resultItem(source);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            ItemStack result = new ItemStack(item);
            AspectList aspects = AspectKeys.ASPECTS.get(source);
            if (aspects != null) {
                AspectKeys.ASPECTS.set(result, aspects.copy());
            }
            EssentiaKeys.FILTER.set(result, EssentiaKeys.FILTER.get(source));
            result.set(RemoteJars.NETWORK.get(), source.get(RemoteJars.NETWORK.get()));
            return result;
        }

        @Override
        public AspectList aspects(List<ItemStack> grid) {
            return cost();
        }

        private static ItemStack jar(List<ItemStack> grid) {
            if (grid.size() != WIDTH * WIDTH || grid.stream().filter(stack -> !stack.isEmpty()).count() != 2) {
                return ItemStack.EMPTY;
            }
            for (int i = 0; i < grid.size() - WIDTH; i++) {
                ItemStack candidate = grid.get(i);
                if (resultItem(candidate) != null && grid.get(i + WIDTH).is(Items.SLIME_BALL)) {
                    return candidate;
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
