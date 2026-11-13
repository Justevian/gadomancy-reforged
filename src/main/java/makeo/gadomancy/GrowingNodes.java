package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.entity.AspectOrb;
import thaumcraft.entity.monster.Wisp;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.Node;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchManager;

public final class GrowingNodes {
    private static final String GROWING = "gadomancy_growing_node";
    private static final String BASE = "gadomancy_node_capacity";
    private static final int CAPACITY = 150;

    private GrowingNodes() {}

    public static boolean isGrowing(BlockEntity entity) {
        return entity.getPersistentData().getBooleanOr(GROWING, false);
    }

    public static void markGrowing(BlockEntity entity) {
        entity.getPersistentData().putBoolean(GROWING, true);
        entity.setChanged();
    }

    public static void copyToItem(ItemStack stack, Node node) {
        Tag capacity = AspectList.CODEC.encodeStart(NbtOps.INSTANCE, node.getAspectsBase()).getOrThrow();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(GROWING, true);
            tag.put(BASE, capacity);
        });
    }

    public static void readJar(ItemStack stack, thaumcraft.blockentity.NodeJarBlockEntity jar) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.getBooleanOr(GROWING, false)) {
            markGrowing(jar);
            Tag capacity = data.get(BASE);
            if (capacity != null) {
                AspectList.CODEC.parse(NbtOps.INSTANCE, capacity).result().ifPresent(base -> copyCapacity(base, jar));
            }
        }
    }

    public static void copyCapacity(AspectList base, Node target) {
        AspectList existing = target.getAspectsBase();
        for (Aspect aspect : existing.getAspects()) {
            existing.remove(aspect);
        }
        for (Aspect aspect : base.getAspects()) {
            existing.add(aspect, base.getAmount(aspect));
        }
    }

    public static void tick(NodeBlockEntity node, ServerLevel level) {
        if (!isGrowing(node) || node.isRemoved() || level.getGameTime() % 8 != 0) {
            return;
        }
        BlockPos pos = node.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos);
        AABB area = new AABB(pos).inflate(12);
        for (AspectOrb orb : level.getEntitiesOfClass(AspectOrb.class, area)) {
            if (canGrow(node, orb.getAspect()) && pull(orb, center)) {
                grow(level, node, orb.getAspect());
                orb.discard();
            }
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = item.getItem();
            if (stack.is(ModItems.WISP_ESSENCE.get()) || stack.is(ModItems.CRYSTALLIZED_ESSENCE.get()) || stack.is(ModBlocks.MANA_BEAN.get())) {
                AspectList aspects = AspectKeys.ASPECTS.get(stack);
                Aspect aspect = aspects == null || aspects.getAspects().isEmpty() ? null : aspects.getAspects().getFirst();
                if (aspect != null && canGrow(node, aspect) && pull(item, center)) {
                    grow(level, node, aspect);
                    if (stack.getCount() == 1) {
                        item.discard();
                    } else {
                        item.setItem(stack.copyWithCount(stack.getCount() - 1));
                    }
                }
            }
        }
        for (Wisp wisp : level.getEntitiesOfClass(Wisp.class, area)) {
            Aspect aspect = wisp.getAspect();
            if (aspect != null && wisp.isAlive() && canGrow(node, aspect) && pull(wisp, center)
                && wisp.hurtServer(level, level.damageSources().magic(), 3F) && wisp.isDeadOrDying()) {
                grow(level, node, aspect);
            }
        }
        if (level.getRandom().nextInt(16) == 0) {
            drainNearbyNode(node, level);
        }
        if (level.getDifficulty() != Difficulty.PEACEFUL && level.getRandom().nextInt(10) == 0) {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(6),
                entity -> entity.isAlive() && !(entity instanceof Player player && (player.isCreative() || player.isSpectator())))) {
                if (victim.hurtServer(level, level.damageSources().magic(), 2F)) {
                    discover(level, pos, NodeManipulators.GROWING_AGGRESSION);
                }
            }
        }
    }

    private static boolean canGrow(NodeBlockEntity node, Aspect aspect) {
        return node.getNodeVisBase(aspect) < CAPACITY;
    }

    private static void grow(ServerLevel level, NodeBlockEntity node, Aspect aspect) {
        NodeManipulatorBlockEntity.adjust(node, aspect, 1);
        node.getAspects().add(aspect, 1);
        node.nodeChange();
        discover(level, node.getBlockPos(), NodeManipulators.GROWING_GROWTH);
    }

    private static boolean pull(Entity entity, Vec3 center) {
        Vec3 direction = center.subtract(entity.position());
        double distance = direction.length();
        if (distance < 1) {
            return true;
        }
        if (distance <= 12) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(0.22 * (1 - distance / 13) / distance)));
        }
        return false;
    }

    private static void drainNearbyNode(NodeBlockEntity node, ServerLevel level) {
        var random = level.getRandom();
        BlockPos pos = node.getBlockPos().offset(random.nextInt(15) - 7, random.nextInt(15) - 7, random.nextInt(15) - 7);
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof NodeBlockEntity target) || node == target
            || target.getAspectsBase().visSize() < 2 || target.getAspectsBase().visSize() > node.getAspectsBase().visSize()) {
            return;
        }
        @Nullable Aspect aspect = target.getAspectsBase().getAspects().stream().filter(a -> canGrow(node, a)).findAny().orElse(null);
        if (aspect != null) {
            NodeManipulatorBlockEntity.adjust(target, aspect, -1);
            target.nodeChange();
            grow(level, node, aspect);
            discover(level, node.getBlockPos(), NodeManipulators.GROWING_ATTACK);
        }
    }

    static void discover(ServerLevel level, BlockPos pos, String research) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(12))) {
            if (ResearchManager.isComplete(player, NodeManipulators.MUTATION)) {
                ResearchManager.complete(player, NodeManipulators.GROWING);
                if (!research.equals(NodeManipulators.GROWING)) {
                    ResearchManager.complete(player, research);
                }
            }
        }
    }
}
