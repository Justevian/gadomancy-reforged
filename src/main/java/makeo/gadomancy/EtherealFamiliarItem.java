package makeo.gadomancy;

import java.util.Comparator;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.item.bauble.VisAmuletItem;
import thaumcraft.network.NodeZapPayload;
import thaumcraft.platform.Bauble;
import thaumcraft.platform.BaubleType;
import thaumcraft.platform.Baubles;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModEffects;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.VisStorage;
import thaumcraft.wand.WandItem;

final class EtherealFamiliarItem extends Item implements Bauble {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gadomancy.MOD_ID);
    static final DeferredItem<EtherealFamiliarItem> ITEM = ITEMS.registerItem("ethereal_familiar",
        EtherealFamiliarItem::new, props -> props.stacksTo(1).rarity(Rarity.EPIC));
    private static final String ASPECT = "gadomancy_familiar_aspect";
    private static final String NEXT_ATTACK = "gadomancy_familiar_next_attack";
    private static final FamiliarAugment[] AUGMENTS = FamiliarAugment.values();

    EtherealFamiliarItem(Properties properties) {
        super(properties);
    }

    static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    static Aspect aspect(ItemStack stack) {
        Aspect aspect = Aspect.getAspect(data(stack).getStringOr(ASPECT, Aspect.MAGIC.getTag()));
        return aspect == null ? Aspect.MAGIC : aspect;
    }

    static Vec3 orbit(LivingEntity wearer, long tick, float partialTick) {
        double angle = (tick + (double) partialTick) * 0.075;
        return wearer.getPosition(partialTick).add(Math.cos(angle) * 0.95,
            wearer.getEyeHeight() + 0.16 + Math.sin(angle * 2) * 0.24, Math.sin(angle) * 0.95);
    }

    static int visualColor(ItemStack stack) {
        CompoundTag state = data(stack);
        if (rank(state, FamiliarAugment.SHOCK) > 0) return 0x8DC8FF;
        if (rank(state, FamiliarAugment.FIRE) > 0) return 0xFF7934;
        if (rank(state, FamiliarAugment.POISON) > 0) return 0x76E94C;
        if (rank(state, FamiliarAugment.WEAKNESS) > 0) return 0xB084E9;
        return aspect(stack).getColor();
    }

    static void setAspect(ItemStack stack, Aspect aspect) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(ASPECT, aspect.getTag()));
    }

    static void setRank(ItemStack stack, FamiliarAugment augment, int rank) {
        if (rank > 0 && rank <= augment.maximum) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(rankKey(augment), rank));
        }
    }

    static boolean canUpgrade(ItemStack stack, FamiliarAugment augment, int current) {
        CompoundTag state = data(stack);
        if (rank(state, augment) != current || current >= augment.maximum) {
            return false;
        }
        boolean hasElement = false;
        for (FamiliarAugment other : AUGMENTS) {
            if (rank(state, other) == 0) {
                continue;
            }
            hasElement |= other.element;
            if (other != augment && other.element == augment.element) {
                return false;
            }
        }
        return augment.element || hasElement;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static int rank(CompoundTag state, FamiliarAugment augment) {
        return Math.clamp(state.getIntOr(rankKey(augment), 0), 0, augment.maximum);
    }

    private static String rankKey(FamiliarAugment augment) {
        return "gadomancy_familiar_" + augment.id();
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.literal(aspect(stack).getName()));
        CompoundTag state = data(stack);
        for (FamiliarAugment augment : AUGMENTS) {
            int rank = rank(state, augment);
            if (rank > 0) {
                builder.accept(Component.translatable("tc.research_name." + augment.research())
                    .append(" ").append(switch (rank) { case 3 -> "III"; case 2 -> "II"; default -> "I"; }));
            }
        }
    }

    @Override
    public void onWornTick(ItemStack stack, LivingEntity wearer) {
        if (!(wearer instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
            || player.isSpectator() || !player.isAlive()) {
            return;
        }
        long now = level.getGameTime();
        Vec3 origin = orbit(player, now, 1F);
        if (level.getDifficulty() == Difficulty.PEACEFUL || now < player.getPersistentData().getLongOr(NEXT_ATTACK, 0)) {
            return;
        }

        CompoundTag state = data(stack);
        FamiliarAugment element = null;
        int elementRank = 0;
        AspectList cost = new AspectList();
        for (FamiliarAugment augment : AUGMENTS) {
            int rank = rank(state, augment);
            augment.addVisCost(cost, rank);
            if (augment.element && rank > 0) {
                element = augment;
                elementRank = rank;
            }
        }
        int speed = rank(state, FamiliarAugment.SPEED);
        double range = 6 + rank(state, FamiliarAugment.RANGE) * 1.8;
        float damage = 1 + rank(state, FamiliarAugment.DAMAGE) * 1.1F;
        if (element != null) {
            switch (element) {
                case SHOCK -> damage += elementRank * 0.8F;
                case FIRE -> damage += elementRank * 0.5F;
                case POISON -> range += elementRank * 0.6;
                case WEAKNESS -> damage -= elementRank * 0.3F;
                default -> {}
            }
        }
        double rangeSquared = range * range;
        int maximumTargets = element == FamiliarAugment.WEAKNESS && elementRank == 3 ? 3 : speed == 2 ? 2 : 1;
        var targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
            mob -> mob instanceof Enemy && mob.isAlive() && !player.isAlliedTo(mob)
                && player.distanceToSqr(mob) <= rangeSquared && player.hasLineOfSight(mob))
            .stream().sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(maximumTargets).toList();
        if (targets.isEmpty()) {
            player.getPersistentData().putLong(NEXT_ATTACK, now + 5);
            return;
        }
        if (!player.isCreative() && !cost.isEmpty() && !consume(player, cost, level.getRandom().nextInt(3) == 0)) {
            player.getPersistentData().putLong(NEXT_ATTACK, now + 8);
            return;
        }
        for (LivingEntity target : targets) {
            attack(level, player, origin, target, damage, element, elementRank);
        }
        player.getPersistentData().putLong(NEXT_ATTACK, now + 32 - 6L * speed);
    }

    private static boolean consume(ServerPlayer player, AspectList cost, boolean spend) {
        for (ItemStack stack : Baubles.stacks(player)) {
            if (stack.getItem() instanceof VisAmuletItem && canPay(stack, cost)) {
                if (spend) {
                    for (Aspect aspect : cost.getAspects()) {
                        VisStorage.storeVis(stack, aspect, VisStorage.getVis(stack, aspect) - cost.getAmount(aspect));
                    }
                }
                return true;
            }
        }
        for (int slot = player.getInventory().getContainerSize() - 1; slot >= 0; slot--) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof WandItem wand && wand.consumeAllVis(stack, player, cost, spend, true)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canPay(ItemStack stack, AspectList cost) {
        for (Aspect aspect : cost.getAspects()) {
            if (VisStorage.getVis(stack, aspect) < cost.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    private static void attack(ServerLevel level, ServerPlayer player, Vec3 origin, LivingEntity target, float damage,
        @Nullable FamiliarAugment element, int rank) {
        DamageSource source = level.damageSources().indirectMagic(player, player);
        if (!target.hurtServer(level, source, damage)) {
            return;
        }
        if (element != null) {
            switch (element) {
                case SHOCK -> {
                    if (level.getRandom().nextBoolean()) {
                        target.knockback(0.4 * rank, player.getX() - target.getX(), player.getZ() - target.getZ(), source, damage);
                    }
                }
                case FIRE -> {
                    target.igniteForSeconds(14 * (1 << rank));
                    if (rank == 3) {
                        target.addEffect(new MobEffectInstance(ModEffects.SUN_SCORNED, 1200, 2), player);
                    }
                }
                case POISON -> {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 600, rank > 1 ? 1 : 0), player);
                    if (rank == 3) {
                        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 2400, 3), player);
                    }
                }
                case WEAKNESS -> {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, rank == 3 ? 1 : 0), player);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, rank == 3 ? 1 : 0), player);
                }
                default -> {}
            }
        }
        ModNetwork.sendToNear(level, null, origin.x, origin.y, origin.z, 32,
            new NodeZapPayload((float) origin.x, (float) origin.y, (float) origin.z,
                (float) target.getX(), (float) target.getEyeY(), (float) target.getZ()));
        level.playSound(null, target.blockPosition(), ModSounds.ZAP.get(), SoundSource.PLAYERS, 0.6F, 1F);
    }
}
