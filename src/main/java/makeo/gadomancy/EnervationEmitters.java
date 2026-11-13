package makeo.gadomancy;

import java.util.List;
import java.util.Map;
import makeo.gadomancy.mixin.CreeperFuseAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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

@EventBusSubscriber(modid = Gadomancy.MOD_ID)
final class EnervationEmitters {
    private static final String ID = "enervation_emitter";
    private static final String RECIPE = "gadomancy:" + ID;
    private static final String RESEARCH = "GADOMANCY.BLOCK_PROTECTOR";
    private static final String POTION_KIND = "gadomancy:weakness_potion";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gadomancy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gadomancy.MOD_ID);

    private static final DeferredBlock<EnervationEmitterBlock> BLOCK = BLOCKS.registerBlock(
        ID, EnervationEmitterBlock::new, properties -> properties.strength(3.5F).noOcclusion()
            .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(EnervationEmitterBlock.ACTIVE) ? 7 : 0)
    );
    static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnervationEmitterBlockEntity>> TYPE =
        BLOCK_ENTITIES.register(ID, () -> new BlockEntityType<>(EnervationEmitterBlockEntity::new, BLOCK.get()));

    private EnervationEmitters() {
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    static void registerResearch() {
        TcStack.registerDataHandler(POTION_KIND, (stack, value) ->
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                value.equals("long") ? Potions.LONG_WEAKNESS : Potions.WEAKNESS)));
        TcStack.registerDataMatcher(POTION_KIND, (stack, value) -> {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            return contents != null && contents.is(value.equals("long") ? Potions.LONG_WEAKNESS : Potions.WEAKNESS);
        });
        TcIngredient weakness = TcIngredient.anyOf(
            TcIngredient.of(TcStack.withData("minecraft:potion", 1, Map.of(POTION_KIND, "normal"))),
            TcIngredient.of(TcStack.withData("minecraft:potion", 1, Map.of(POTION_KIND, "long")))
        );
        TcIngredient tube = TcIngredient.item("thaumcraft:essentia_tube");
        TcIngredient grate = TcIngredient.item("thaumcraft:item_grate");
        TcStack result = TcStack.of(RECIPE);
        TcRecipes.register(new ShapedArcaneRecipe(RECIPE, RESEARCH, result,
            new AspectList().add(Aspect.ORDER, 120).add(Aspect.EARTH, 120), 3, 3, List.of(
                weakness, TcIngredient.item("thaumcraft:nitor"), weakness,
                tube, TcIngredient.item("thaumcraft:warded_jar"), tube,
                grate, TcIngredient.item("minecraft:redstone"), grate
            )));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.ORDER, 10).add(Aspect.ARMOR, 8).add(Aspect.EARTH, 12)
                .add(Aspect.LIGHT, 10).add(Aspect.AURA, 8), -6, -2, 2, result
        ).setParents("GADOMANCY.STICKYJAR").setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.BLOCK_PROTECTOR.1"),
            ResearchPage.arcane(RECIPE),
            ResearchPage.text("tc.research_page.GADOMANCY.BLOCK_PROTECTOR.2")
        ));
    }

    @SubscribeEvent
    static void stopCreeperFuse(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Creeper creeper && creeper.level() instanceof ServerLevel server
            && EnervationEmitterBlockEntity.protects(server, creeper.position())) {
            ((CreeperFuseAccessor) creeper).gadomancy$setSwell(0);
            creeper.setSwellDir(-1);
        }
    }

    @SubscribeEvent
    static void stopMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel server
            && EnervationEmitterBlockEntity.protects(server, mob.position())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    static void stopEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel server
            && EnervationEmitterBlockEntity.protects(server, mob.position())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void protectExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server
            && (event.getExplosion().getDirectSourceEntity() instanceof Mob
                || event.getExplosion().getIndirectSourceEntity() instanceof Mob)) {
            event.getAffectedBlocks().removeIf(pos -> EnervationEmitterBlockEntity.protects(server, net.minecraft.world.phys.Vec3.atCenterOf(pos)));
        }
    }
}
