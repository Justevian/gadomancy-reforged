package makeo.gadomancy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.infusion.PedestalBlockEntity;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModBlocks;
import thaumcraft.wand.Wands;

final class NodeManipulatorBlockEntity extends TcBlockEntity {
    private static final List<BlockPos> CORNERS = List.of(
        new BlockPos(-1, 0, -1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, 1));
    private static final List<NodeType> TYPES = List.of(
        NodeType.NORMAL, NodeType.NORMAL, NodeType.PURE, NodeType.DARK,
        NodeType.UNSTABLE, NodeType.TAINTED, NodeType.HUNGRY);
    private @Nullable UUID owner;
    private boolean portal;
    private int workTicks;
    private ItemStack wand = ItemStack.EMPTY;
    private final int[] charge = new int[Aspect.getPrimalAspects().size()];

    NodeManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(NodeManipulators.TYPE.get(), pos, state);
    }

    boolean isAssembled() {
        return owner != null;
    }

    boolean canInsert(Player player) {
        return player.getUUID().equals(owner) && wand.isEmpty();
    }

    boolean canRemove(Player player) {
        return player.getUUID().equals(owner) && !wand.isEmpty();
    }

    void assemble(Player player, boolean usePortal) {
        owner = player.getUUID();
        portal = usePortal;
        sync();
    }

    void insert(ItemStack stack) {
        wand = stack;
        sync();
    }

    ItemStack removeWand() {
        ItemStack result = wand;
        wand = ItemStack.EMPTY;
        sync();
        return result;
    }

    boolean matches(Level level, boolean usePortal, boolean requireEyes) {
        return matchesFocusAndPillars(level, usePortal) && (!usePortal || portalPedestals(level, requireEyes).size() == 4);
    }

    private boolean matchesFocusAndPillars(Level level, boolean usePortal) {
        BlockPos pos = worldPosition;
        if (!structureLoaded(level) || !level.getBlockState(pos.above()).is(usePortal ? NodeManipulators.REDIRECTION.get() : NodeManipulators.RANDOMIZATION.get())
            || !(level.getBlockEntity(pos.above(2)) instanceof NodeBlockEntity)) {
            return false;
        }
        for (BlockPos corner : CORNERS) {
            BlockPos pillar = pos.offset(corner);
            if (!level.getBlockState(pillar).is(ModBlocks.ANCIENT_STONE_PEDESTAL.get())
                || !level.getBlockState(pillar.above()).is(ModBlocks.ANCIENT_STONE.get())) {
                return false;
            }
        }
        return true;
    }

    private boolean structureLoaded(Level level) {
        return level.hasChunkAt(worldPosition.offset(-1, 0, -1)) && level.hasChunkAt(worldPosition.offset(1, 0, -1))
            && level.hasChunkAt(worldPosition.offset(-1, 0, 1)) && level.hasChunkAt(worldPosition.offset(1, 0, 1));
    }

    private List<PedestalBlockEntity> portalPedestals(Level level, boolean requireEyes) {
        List<PedestalBlockEntity> candidates = new ArrayList<>();
        BlockPos min = worldPosition.offset(-8, -5, -8);
        BlockPos max = worldPosition.offset(8, 10, 8);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
                if (level.getChunk(x, z, ChunkStatus.FULL, false) instanceof LevelChunk chunk) {
                    for (var entity : chunk.getBlockEntities().values()) {
                        if (!(entity instanceof AncientPedestalBlock.AncientPedestalBlockEntity pedestal) || pedestal.isRemoved()) {
                            continue;
                        }
                        BlockPos pos = pedestal.getBlockPos();
                        if (pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY()
                            && pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()
                            && (!requireEyes || pedestal.getStack().is(EldritchContent.ELDRITCH_EYE.get()))) {
                            candidates.add(pedestal);
                        }
                    }
                }
            }
        }
        candidates.sort(Comparator.<PedestalBlockEntity>comparingDouble(p -> p.getBlockPos().distSqr(worldPosition))
            .thenComparingLong(p -> p.getBlockPos().asLong()));
        var columns = new HashSet<BlockPos>();
        return candidates.stream().filter(p -> columns.add(p.getBlockPos().atY(0))).limit(4).toList();
    }

    boolean mayOperate(ServerLevel level, Player player, boolean usePortal) {
        return mayOperate(level, player, usePortal ? portalPedestals(level, false) : List.of());
    }

    private boolean mayOperate(ServerLevel level, Player player, List<PedestalBlockEntity> pedestals) {
        return level.mayInteract(player, worldPosition) && level.mayInteract(player, worldPosition.above(2))
            && pedestals.stream().allMatch(pedestal -> level.mayInteract(player, pedestal.getBlockPos()));
    }

    void tick(ServerLevel level) {
        if (owner == null || level.getGameTime() % 5 != 0 || !structureLoaded(level)) {
            return;
        }
        if (!matchesFocusAndPillars(level, portal)) {
            ItemStack returned = wand;
            wand = ItemStack.EMPTY;
            Arrays.fill(charge, 0);
            owner = null;
            workTicks = 0;
            sync();
            if (!returned.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5, returned);
            }
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (!Wands.isWand(wand) || player == null || player.level() != level
            || !mayOperate(level, player, List.of())
            || !(level.getBlockEntity(worldPosition.above(2)) instanceof NodeBlockEntity node)) {
            return;
        }
        List<Aspect> primals = Aspect.getPrimalAspects();
        int min = portal ? 120 : 70;
        int max = portal ? 150 : 120;
        boolean charged = true;
        boolean draining = false;
        for (int i = 0; i < charge.length; i++) {
            Aspect aspect = primals.get(i);
            int vis = Wands.getVis(wand, aspect);
            if (vis >= 100 && charge[i] < max) {
                Wands.storeVis(wand, aspect, vis - 100);
                charge[i]++;
                draining = true;
            }
            charged &= charge[i] >= min;
        }
        if (draining) {
            setChanged();
            return;
        }
        if (!charged) {
            return;
        }
        List<PedestalBlockEntity> pedestals = portal ? portalPedestals(level, true) : List.of();
        if (portal && (pedestals.size() != 4 || !mayOperate(level, player, pedestals))) {
            return;
        }
        workTicks += 5;
        setChanged();
        if (workTicks < (portal ? 400 : 300)) {
            if (workTicks % 20 == 0) {
                level.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 2.5,
                    worldPosition.getZ() + 0.5, 8, 0.35, 0.35, 0.35, 0.1);
            }
            return;
        }
        if (portal) {
            if (!level.setBlock(worldPosition.above(2), NodeManipulators.PORTAL.get().defaultBlockState(), Block.UPDATE_ALL)) {
                return;
            }
            for (PedestalBlockEntity pedestal : pedestals) {
                pedestal.removeItem(0, 1);
            }
        } else {
            mutate(level.getRandom(), node);
            if (GrowingNodes.isGrowing(node)) {
                GrowingNodes.discover(level, node.getBlockPos(), NodeManipulators.GROWING);
            }
        }
        level.sendParticles(ParticleTypes.PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 2.5,
            worldPosition.getZ() + 0.5, 28, 0.35, 0.35, 0.35, 0.35);
        Arrays.fill(charge, 0);
        workTicks = 0;
        ItemStack returned = removeWand();
        if (!player.addItem(returned)) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                worldPosition.getZ() + 0.5, returned);
        }
        player.sendSystemMessage(Component.translatable(portal
            ? "message.gadomancy.node_manipulator.gate" : "message.gadomancy.node_manipulator.mutated"));
    }

    private void mutate(RandomSource random, NodeBlockEntity node) {
        AspectList base = node.getAspectsBase();
        List<Aspect> split = base.getAspects().stream().filter(aspect -> !aspect.isPrimal()).toList();
        List<Aspect> join = Aspect.getCompoundAspects().stream().filter(aspect -> {
            Aspect[] parts = aspect.getComponents();
            return parts != null && base.getAmount(parts[0]) > 0 && base.getAmount(parts[1]) > (parts[0] == parts[1] ? 1 : 0);
        }).toList();
        List<Aspect> absent = Aspect.getPrimalAspects().stream().filter(aspect -> base.getAmount(aspect) == 0).toList();
        List<Runnable> changes = new ArrayList<>();
        int total = Arrays.stream(charge).sum();
        if (!split.isEmpty()) {
            weighted(changes, total < 570 ? 6 : 2, () -> {
                Aspect aspect = split.get(random.nextInt(split.size()));
                Aspect[] parts = aspect.getComponents();
                if (parts != null) {
                    adjust(node, aspect, -1);
                    adjust(node, parts[0], 1);
                    adjust(node, parts[1], 1);
                }
            });
        }
        if (!join.isEmpty()) {
            weighted(changes, 5, () -> {
                Aspect aspect = join.get(random.nextInt(join.size()));
                Aspect[] parts = aspect.getComponents();
                if (parts != null) {
                    adjust(node, parts[0], -1);
                    adjust(node, parts[1], -1);
                    adjust(node, aspect, 1);
                }
            });
        }
        NodeModifier modifier = node.getNodeModifier();
        if (modifier != NodeModifier.BRIGHT) {
            weighted(changes, total >= 570 ? 7 : 4, () -> node.setNodeModifier(
                modifier == NodeModifier.FADING ? NodeModifier.PALE : modifier == NodeModifier.PALE ? null : NodeModifier.BRIGHT));
        }
        if (modifier != NodeModifier.FADING) {
            weighted(changes, total < 570 ? 5 : 2, () -> node.setNodeModifier(
                modifier == NodeModifier.BRIGHT ? null : modifier == null ? NodeModifier.PALE : NodeModifier.FADING));
        }
        if (!absent.isEmpty()) {
            weighted(changes, 3, () -> adjust(node, absent.get(random.nextInt(absent.size())), 1));
        }
        List<NodeType> types = TYPES.stream().filter(type -> type != node.getNodeType()).toList();
        weighted(changes, 2, () -> node.setNodeType(types.get(random.nextInt(types.size()))));
        if (!GrowingNodes.isGrowing(node)) {
            weighted(changes, 1, () -> GrowingNodes.markGrowing(node));
        }
        changes.get(random.nextInt(changes.size())).run();
        node.nodeChange();
    }

    static void adjust(NodeBlockEntity node, Aspect aspect, int delta) {
        AspectList base = node.getAspectsBase();
        AspectList current = node.getAspects();
        int amount = Math.max(0, base.getAmount(aspect) + delta);
        base.remove(aspect);
        if (amount > 0) {
            base.add(aspect, amount);
        }
        if (current.getAmount(aspect) > amount) {
            current.remove(aspect, current.getAmount(aspect) - amount);
        }
    }

    private static void weighted(List<Runnable> changes, int weight, Runnable change) {
        for (int i = 0; i < weight; i++) {
            changes.add(change);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server && !wand.isEmpty()) {
            Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, wand);
            wand = ItemStack.EMPTY;
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        portal = input.getBooleanOr("portal", false);
        workTicks = Math.clamp(input.getIntOr("work_ticks", 0), 0, portal ? 400 : 300);
        wand = input.read("wand", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        for (int i = 0; i < charge.length; i++) {
            charge[i] = Math.clamp(input.getIntOr("charge_" + i, 0), 0, portal ? 150 : 120);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.putBoolean("portal", portal);
        output.putInt("work_ticks", workTicks);
        output.store("wand", ItemStack.OPTIONAL_CODEC, wand);
        for (int i = 0; i < charge.length; i++) {
            output.putInt("charge_" + i, charge[i]);
        }
    }
}
