package makeo.gadomancy;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.device.CrystalClusterBlock;
import thaumcraft.research.ResearchGate;
import thaumcraft.wand.WandItem;

@EventBusSubscriber(modid = Gadomancy.MOD_ID)
final class AuraCoreItem extends Item {
    private static final String CLUSTER = "gadomancy_aura_cluster";
    private static final String START = "gadomancy_aura_start";
    private static final String ORIGIN = "gadomancy_aura_origin";
    private static final int DURATION = 500;

    AuraCoreItem(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    static void energizeCluster(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || !(event.getItemStack().getItem() instanceof WandItem wand)
            || !ResearchGate.isComplete(event.getEntity(), AuraCores.RESEARCH)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof CrystalClusterBlock cluster)
            || cluster.getKind() > CrystalClusterBlock.MIXED || !level.mayInteract(event.getEntity(), pos)) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        ItemEntity core = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(10), candidate ->
            candidate.isAlive() && candidate.getItem().is(AuraCores.BLANK.get())
                && candidate.getAge() != candidate.lifespan - 1 && !candidate.getPersistentData().contains(START)
        ).stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center))).orElse(null);
        if (core == null) {
            return;
        }

        int kind = cluster.getKind();
        AspectList cost = new AspectList();
        if (kind == CrystalClusterBlock.MIXED) {
            AuraCores.PRIMALS.forEach(aspect -> cost.add(aspect, 14));
        } else {
            cost.add(AuraCores.PRIMALS.get(kind), 84);
        }
        ItemStack wandStack = event.getItemStack();
        if (!wand.consumeAllVisCrafting(wandStack, event.getEntity(), cost, false)
            || !level.removeBlock(pos, false)) {
            return;
        }
        wand.consumeAllVisCrafting(wandStack, event.getEntity(), cost, true);

        core.getPersistentData().putLong(START, level.getGameTime());
        core.getPersistentData().putInt(CLUSTER, kind);
        core.getPersistentData().putLong(ORIGIN, pos.asLong());
        core.setPickUpDelay(DURATION);
        core.lifespan = Math.max(core.lifespan, core.getAge() + DURATION + 20);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || !item.getPersistentData().contains(START)) {
            return false;
        }
        long elapsed = level.getGameTime() - makeo.gadomancy.PortNbt.getLongOr(item.getPersistentData(), START, level.getGameTime());
        if (elapsed >= DURATION) {
            int variant = sampleAura(level, item.blockPosition(), makeo.gadomancy.PortNbt.getIntOr(item.getPersistentData(), CLUSTER, -1));
            if (variant >= 0) {
                item.setItem(new ItemStack(AuraCores.CHARGED.get(variant).get()));
            }
            item.getPersistentData().remove(ORIGIN);
            item.getPersistentData().remove(START);
            item.getPersistentData().remove(CLUSTER);
            item.setDefaultPickUpDelay();
            level.sendParticles(variant >= 0 ? ParticleTypes.END_ROD : ParticleTypes.SMOKE,
                item.getX(), item.getY(), item.getZ(), 18, 0.35, 0.35, 0.35, 0.03);
        } else {
            item.setPickUpDelay((int) (DURATION - Math.max(0, elapsed)));
            if (elapsed >= 0 && elapsed % 2 == 0) chargeTrails(level, item, elapsed);
        }
        return false;
    }

    private static void chargeTrails(ServerLevel level, ItemEntity item, long elapsed) {
        Vec3 center = item.position().add(0, 0.25, 0);
        Vec3 origin = Vec3.atCenterOf(BlockPos.of(PortNbt.getLongOr(item.getPersistentData(), ORIGIN, item.blockPosition().asLong())));
        int kind = PortNbt.getIntOr(item.getPersistentData(), CLUSTER, CrystalClusterBlock.MIXED);
        double radius = 0.85 * Math.min(1.0, (DURATION - elapsed) / 450.0);
        for (int i = 0; i < AuraCores.PRIMALS.size(); i++) {
            Aspect aspect = AuraCores.PRIMALS.get(kind >= 0 && kind < AuraCores.PRIMALS.size() ? kind : i);
            double phase = i * Math.PI / 3;
            double angle = phase + elapsed * Math.PI * 2 / (120 + i * 7);
            Vec3 orbit = center.add(radius * Math.cos(angle), radius * Math.sin(angle) * Math.sin(phase),
                radius * Math.sin(angle) * Math.cos(phase));
            Vec3 from = elapsed < 50 ? origin : orbit;
            Vec3 to = elapsed < 50 ? orbit : center;
            var dust = new net.minecraft.core.particles.DustParticleOptions(
                Vec3.fromRGB24(aspect.getColor()).toVector3f(), 0.48F);
            double progress = ((elapsed / 2 + i) % 7) / 7.0;
            Vec3 point = from.lerp(to, progress);
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static int sampleAura(ServerLevel level, BlockPos center, int kind) {
        if (kind < 0 || kind > CrystalClusterBlock.MIXED) {
            return -1;
        }

        AspectList aura = new AspectList();
        if (kind == CrystalClusterBlock.MIXED) {
            AuraCores.PRIMALS.forEach(aspect -> aura.add(aspect, 10));
        } else {
            aura.add(AuraCores.PRIMALS.get(kind), 60);
        }

        int scanned = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))) {
            if (!level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
                continue;
            }
            var state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            AspectList aspects = AspectHelper.getObjectTags(new ItemStack(state.getBlock()));
            if (aspects != null && !aspects.isEmpty()) {
                AspectList primals = AspectHelper.reduceToPrimals(aspects);
                for (Aspect aspect : primals.getAspects()) {
                    aura.add(aspect, primals.getAmount(aspect));
                }
                scanned++;
            }
        }
        if (scanned < 109) {
            return -1;
        }

        double average = (double) aura.visSize() / aura.size();
        int total = aura.getAspects().stream().mapToInt(aura::getAmount).filter(amount -> amount >= average).sum();
        int draw = level.getRandom().nextInt(total);
        for (int index = 0; index < AuraCores.PRIMALS.size(); index++) {
            int weight = aura.getAmount(AuraCores.PRIMALS.get(index));
            if (weight >= average) {
                draw -= weight;
                if (draw < 0) {
                    return index;
                }
            }
        }
        return -1;
    }
}
