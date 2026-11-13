package makeo.gadomancy;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import thaumcraft.Config;
import makeo.gadomancy.mixin.ChampionWhitelistInvoker;
import thaumcraft.aspect.Aspect;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.monster.ChampionModifier;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModEffects;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.taint.TaintBiomes;
import thaumcraft.taint.TaintSpread;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Gadomancy.MOD_ID)
final class PylonEffects {
    private record Effect(Holder<MobEffect> type, int interval, int addedTicks, int capMinutes, int amplifier) {
        void apply(LivingEntity target) {
            MobEffectInstance old = target.getEffect(type);
            if (old != null && (old.isInfiniteDuration() || old.getAmplifier() > amplifier)) {
                return;
            }
            int duration = old == null ? 0 : old.getDuration();
            if (duration < capMinutes * 1200) {
                target.addEffect(new MobEffectInstance(type,
                    Math.min(capMinutes * 1200, duration + addedTicks), amplifier, true, true));
            }
        }
    }

    private static final Map<Aspect, Effect> POTIONS = Map.ofEntries(
        Map.entry(Aspect.AIR, new Effect(MobEffects.SPEED, 4, 6, 6, 1)),
        Map.entry(Aspect.WATER, new Effect(MobEffects.WATER_BREATHING, 4, 10, 10, 0)),
        Map.entry(Aspect.EARTH, new Effect(MobEffects.RESISTANCE, 4, 8, 5, 0)),
        Map.entry(Aspect.ORDER, new Effect(MobEffects.REGENERATION, 4, 6, 3, 0)),
        Map.entry(Aspect.ENTROPY, new Effect(MobEffects.WEAKNESS, 4, 8, 3, 0)),
        Map.entry(Aspect.WEAPON, new Effect(MobEffects.STRENGTH, 4, 6, 6, 2)),
        Map.entry(Aspect.ARMOR, new Effect(MobEffects.RESISTANCE, 4, 6, 6, 1)),
        Map.entry(Aspect.MOTION, new Effect(MobEffects.SPEED, 4, 6, 2, 2)),
        Map.entry(Aspect.TRAVEL, new Effect(MobEffects.SPEED, 2, 10, 15, 0)),
        Map.entry(Aspect.HUNGER, new Effect(MobEffects.HUNGER, 4, 6, 2, 0)),
        Map.entry(Aspect.SENSES, new Effect(MobEffects.NIGHT_VISION, 2, 8, 8, 0)),
        Map.entry(Aspect.FLIGHT, new Effect(MobEffects.JUMP_BOOST, 2, 5, 4, 0)),
        Map.entry(Aspect.ENERGY, new Effect(MobEffects.STRENGTH, 4, 6, 4, 0)),
        Map.entry(Aspect.DARKNESS, new Effect(MobEffects.BLINDNESS, 4, 6, 1, 0)),
        Map.entry(Aspect.HEAL, new Effect(MobEffects.REGENERATION, 8, 10, 5, 1)),
        Map.entry(Aspect.DEATH, new Effect(MobEffects.WITHER, 4, 6, 3, 1)),
        Map.entry(Aspect.MAN, new Effect(MobEffects.HEALTH_BOOST, 2, 5, 4, 1)),
        Map.entry(Aspect.COLD, new Effect(MobEffects.SLOWNESS, 2, 5, 3, 1)),
        Map.entry(Aspect.TOOL, new Effect(MobEffects.HASTE, 2, 7, 6, 0)),
        Map.entry(Aspect.MINE, new Effect(MobEffects.HASTE, 2, 7, 5, 2)),
        Map.entry(Aspect.POISON, new Effect(MobEffects.POISON, 2, 6, 1, 0)),
        Map.entry(Aspect.TRAP, new Effect(MobEffects.SLOWNESS, 4, 6, 3, 0))
    );

    private static final Effect UNDEAD_STRENGTH = new Effect(MobEffects.STRENGTH, 4, 30, 3, 1);
    private static final Effect UNDEAD_SPEED = new Effect(MobEffects.SPEED, 4, 30, 3, 0);
    private static final Effect UNDEAD_REGENERATION = new Effect(MobEffects.REGENERATION, 4, 30, 3, 0);
    private static final Effect GOLEM_BUFF = new Effect(PylonStatusEffects.GOLEM_BUFF, 4, 10, 6, 0);
    private static final Effect VIS_DISCOUNT = new Effect(PylonStatusEffects.VIS_DISCOUNT, 4, 6, 5, 1);
    private static final Map<ServerLevel, Map<BlockPos, Long>> LIGHT_FIELDS = new WeakHashMap<>();
    private static final List<EntityType<? extends Mob>> FARM_ANIMALS =
        List.of(EntityTypes.CHICKEN, EntityTypes.COW, EntityTypes.PIG, EntityTypes.SHEEP);
    private static final String ELDRITCH_EXPOSURE = "gadomancy_pylon_eldritch_exposure";
    private static final String ELDRITCH_EXPIRY = "gadomancy_pylon_eldritch_expiry";

    private PylonEffects() {
    }

    static void apply(ServerLevel server, Vec3 center, Aspect aspect, long tick) {
        if (aspect == Aspect.LIGHT && tick % 4 == 0) {
            Map<BlockPos, Long> fields = LIGHT_FIELDS.computeIfAbsent(server, level -> new HashMap<>());
            fields.values().removeIf(expiry -> expiry <= tick);
            fields.put(BlockPos.containing(center), tick + 5);
        }
        applyToBlocks(server, BlockPos.containing(center), aspect, tick);
        Effect effect = POTIONS.get(aspect);
        boolean fire = aspect == Aspect.FIRE && tick % 20 == 0;
        boolean special = (aspect == Aspect.AURA)
            || (aspect == Aspect.LIFE && tick % 20 == 0)
            || (aspect == Aspect.CLOTH && tick % 10 == 0)
            || (aspect == Aspect.CRAFT && tick % 4 == 0)
            || (aspect == Aspect.MAGIC && tick % 4 == 0)
            || (aspect == Aspect.TAINT && tick % 20 == 0)
            || (aspect == Aspect.ELDRITCH && tick % 20 == 0)
            || (aspect == Aspect.MIND && tick % 40 == 0)
            || (aspect == Aspect.UNDEAD && tick % 4 == 0);
        if (!fire && !special && (effect == null || tick % effect.interval() != 0)) {
            return;
        }
        AABB area = AABB.ofSize(center, 9, 9, 9);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area,
            entity -> entity.isAlive() && !entity.isSpectator())) {
            if (fire) {
                target.igniteForSeconds(10);
            } else if (effect != null) {
                effect.apply(target);
            } else if (aspect == Aspect.AURA && target instanceof ServerPlayer player) {
                ItemStack wand = player.getMainHandItem();
                if (Wands.isWand(wand)) {
                    for (Aspect primal : Aspect.getPrimalAspects()) {
                        if (Wands.getVis(wand, primal) < Wands.getMaxVis(wand)) {
                            Wands.addRealVis(wand, primal, 4, true);
                        }
                    }
                }
            } else if (aspect == Aspect.LIFE && target instanceof AgeableMob animal && animal.isBaby()
                && server.getRandom().nextInt(20) == 0) {
                animal.setAge(0);
            } else if (aspect == Aspect.CLOTH && target instanceof Sheep sheep && sheep.isSheared()
                && server.getRandom().nextInt(10) == 0) {
                sheep.setSheared(false);
            } else if (aspect == Aspect.MIND && target instanceof ServerPlayer player) {
                for (Aspect primal : Aspect.getPrimalAspects()) {
                    if (server.getRandom().nextInt(40) == 0) {
                        ResearchManager.addAspectPool(player, primal, 1);
                        if (server.getRandom().nextInt(20) == 0) {
                            Warp.add(player, 1, WarpType.TEMPORARY);
                        }
                    }
                }
            } else if (aspect == Aspect.UNDEAD && target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                UNDEAD_STRENGTH.apply(target);
                UNDEAD_SPEED.apply(target);
                UNDEAD_REGENERATION.apply(target);
            } else if (aspect == Aspect.CRAFT && target instanceof GolemBase) {
                GOLEM_BUFF.apply(target);
            } else if (aspect == Aspect.MAGIC && target instanceof ServerPlayer) {
                VIS_DISCOUNT.apply(target);
            } else if (aspect == Aspect.TAINT && !target.hasEffect(ModEffects.TAINT_POISON)
                && server.getRandom().nextInt(20) == 0) {
                target.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON, 400, 0, true, true));
            } else if (aspect == Aspect.ELDRITCH && target instanceof Mob mob
                && ChampionWhitelistInvoker.gadomancy$whitelistLevel(mob) >= 0 && ChampionModifier.get(mob) == ChampionModifier.NORMAL
                && mob.getMaxHealth() >= 10 && server.getRandom().nextInt(4) == 0) {
                exposeToEldritch(server, mob);
            }
        }
    }

    @SubscribeEvent
    static void suppressMonsterSpawn(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getSpawnType() != EntitySpawnReason.NATURAL
            || event.getEntity().getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        Map<BlockPos, Long> fields = LIGHT_FIELDS.get(level);
        if (fields == null) {
            return;
        }
        fields.values().removeIf(expiry -> expiry <= level.getGameTime());
        BlockPos pos = event.getEntity().blockPosition();
        if (fields.keySet().stream().anyMatch(center -> center.distSqr(pos) <= 4096)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static void applyToBlocks(ServerLevel server, BlockPos center, Aspect aspect, long tick) {
        if (aspect == Aspect.BEAST || aspect == Aspect.SLIME || aspect == Aspect.UNDEAD || aspect == Aspect.SOUL) {
            if (tick % 4 == 0 && server.getRandom().nextInt(aspect == Aspect.SLIME ? 40 : 60) == 0) {
                spawnFromAura(server, center, aspect);
            }
            return;
        }
        int samples;
        if (aspect == Aspect.COLD && tick % 2 == 0) {
            samples = 10 + server.getRandom().nextInt(10);
        } else if (aspect == Aspect.TAINT && tick % 20 == 0 && Config.taintSpreadRate() > 0) {
            samples = 5;
        } else if (aspect == Aspect.PLANT || aspect == Aspect.TREE && tick % 3 == 0) {
            samples = aspect == Aspect.PLANT ? 60 : 40;
        } else if (aspect == Aspect.WEATHER && tick % 20 == 0 && server.getRandom().nextInt(10) == 0) {
            BlockPos selected = randomPosition(server, center);
            if (server.hasChunkAt(selected)) {
                BlockPos surface = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, selected);
                var bolt = EntityTypes.LIGHTNING_BOLT.create(server, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.setPos(Vec3.atBottomCenterOf(surface));
                    server.addFreshEntity(bolt);
                }
            }
            return;
        } else {
            return;
        }
        for (int i = 0; i < samples; i++) {
            BlockPos pos = randomPosition(server, center);
            if (!server.hasChunkAt(pos)) {
                continue;
            }
            var state = server.getBlockState(pos);
            if (aspect == Aspect.COLD) {
                if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                    server.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                }
            } else if (aspect == Aspect.TAINT) {
                if (TaintSpread.spreadFibres(server, pos) && server.getRandom().nextInt(12) == 0) {
                    TaintBiomes.set(server, pos, TaintBiomes.TAINTED_LAND);
                }
            } else if (state.isRandomlyTicking()) {
                state.randomTick(server, pos, server.getRandom());
            }
        }
    }

    private static BlockPos randomPosition(ServerLevel server, BlockPos center) {
        var random = server.getRandom();
        return center.offset(random.nextInt(17) - 8, random.nextInt(13) - 8, random.nextInt(17) - 8);
    }

    private static void spawnFromAura(ServerLevel level, BlockPos center, Aspect aspect) {
        if (aspect != Aspect.BEAST && level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        BlockPos pos = randomPosition(level, center);
        if (!level.hasChunkAt(pos) || !level.isEmptyBlock(pos)) {
            return;
        }
        for (int i = 0; i < 8 && level.isEmptyBlock(pos.below()); i++) {
            pos = pos.below();
        }
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return;
        }
        var random = level.getRandom();
        EntityType<? extends Mob> type = aspect == Aspect.BEAST ? FARM_ANIMALS.get(random.nextInt(FARM_ANIMALS.size()))
            : aspect == Aspect.SLIME ? EntityTypes.SLIME : aspect == Aspect.UNDEAD ? ModEntities.BRAINY_ZOMBIE.get()
            : random.nextBoolean() ? EntityTypes.SKELETON : EntityTypes.ZOMBIE;
        Mob spawned = type.create(level, EntitySpawnReason.TRIGGERED);
        if (spawned == null) {
            return;
        }
        spawned.setPos(Vec3.atBottomCenterOf(pos));
        if (!level.noCollision(spawned)) {
            return;
        }
        if (spawned instanceof Slime slime) {
            slime.setSize(1, true);
        }
        if (aspect == Aspect.SOUL) {
            spawned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(type == EntityTypes.SKELETON ? Items.BOW : Items.STONE_SWORD));
            spawned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
            spawned.setDropChance(EquipmentSlot.MAINHAND, 0F);
            spawned.setDropChance(EquipmentSlot.HEAD, 0F);
            spawned.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, true, false));
        }
        level.addFreshEntity(spawned);
    }

    private static void exposeToEldritch(ServerLevel level, Mob mob) {
        var state = mob.getPersistentData();
        long tick = level.getGameTime();
        int exposure = tick < state.getLongOr(ELDRITCH_EXPIRY, 0) ? state.getIntOr(ELDRITCH_EXPOSURE, 0) + 1 : 1;
        if (exposure >= 6) {
            ChampionModifier.assign(mob, level.getRandom().nextInt(ChampionModifier.count()), true);
            state.remove(ELDRITCH_EXPOSURE);
            state.remove(ELDRITCH_EXPIRY);
        } else {
            state.putInt(ELDRITCH_EXPOSURE, exposure);
            state.putLong(ELDRITCH_EXPIRY, tick + 1200);
        }
    }
}
