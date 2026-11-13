package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSource;
import thaumcraft.essentia.Essentia;
import thaumcraft.network.EssentiaSourceFxPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.infusion.PedestalBlockEntity;
import thaumcraft.device.EssentiaMirrorBlockEntity;
import thaumcraft.registry.ModBlocks;

final class CondenserBlockEntity extends AuraStructureBlockEntity implements AspectSource {
    private static final int BASE_CAPACITY_PER_ASPECT = 200;
    private static final int MAX_CAPACITY_PER_ASPECT = 3000;
    private static final int MAX_UPGRADES = 8;
    private static final int CAPACITY_PER_UPGRADE = (MAX_CAPACITY_PER_ASPECT - BASE_CAPACITY_PER_ASPECT) / MAX_UPGRADES;
    private static final int UPGRADE_TICKS = 400;
    private static final DustParticleOptions UPGRADE_DUST = new DustParticleOptions(0x512574, 0.8F);
    private AspectList essentia = new AspectList();
    private int upgrades;
    private @Nullable BlockPos pedestal;
    private int upgradeProgress;

    CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(AuraStructures.CONDENSER_TYPE.get(), pos, state);
    }

    int capacity() {
        return BASE_CAPACITY_PER_ASPECT + upgrades * CAPACITY_PER_UPGRADE;
    }

    private boolean active() {
        return isActive(AuraStructureBlock.Kind.CONDENSER);
    }

    @Override
    void serverTick(ServerLevel server) {
        long tick = server.getGameTime();
        if (tick % 10 == 0 && !drawFromPorts(server)) {
            drawEssentia(server);
        }
        if (upgrades < MAX_UPGRADES && (pedestal != null || tick % 40 == 0)) {
            upgrade(server);
        }
        if (upgrades > 0) {
            Vec3 center = Vec3.atCenterOf(worldPosition);
            for (Entity entity : server.getEntitiesOfClass(Entity.class, new AABB(worldPosition).inflate(4),
                target -> target.isAlive() && !target.isSpectator())) {
                Vec3 offset = center.subtract(entity.position());
                double distance = offset.length();
                if (distance > 0.01 && distance < 8) {
                    Vec3 pull = offset.scale((0.08 * upgrades / MAX_UPGRADES) * Mth.square(1.0 - distance / 8.0) / distance);
                    entity.push(pull.x, pull.y, pull.z);
                }
            }
        }
    }

    private boolean drawFromPorts(ServerLevel server) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Direction output = side.getOpposite();
            var source = Essentia.getConnectable(server, worldPosition, side);
            if (source == null || !source.canOutputTo(output) || source.getSuctionAmount(output) >= getSuctionAmount(side)
                || source.getMinimumSuction() > getSuctionAmount(side)) {
                continue;
            }
            Aspect aspect = source.getEssentiaType(output);
            if (aspect != null && doesContainerAccept(aspect) && source.takeEssentia(aspect, 1, output) > 0) {
                addToContainer(aspect, 1);
                showIntake(server, worldPosition.relative(side), aspect);
                return true;
            }
        }
        return false;
    }

    private void drawEssentia(ServerLevel server) {
        int total = essentia.visSize();
        if (total == 0) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-5, -5, -5), worldPosition.offset(5, 5, 5))) {
            if (pos.distSqr(worldPosition) > 25 || !loaded(server, pos)
                || !(server.getBlockEntity(pos) instanceof AspectSource source) || source instanceof CondenserBlockEntity) {
                continue;
            }
            AspectList available = source.getAspects();
            if (available == null || available.visSize() > total) {
                continue;
            }
            boolean mirror = source instanceof EssentiaMirrorBlockEntity;
            Iterable<Aspect> candidates = mirror ? java.util.Arrays.asList(Aspect.values()) : available.getAspects();
            for (Aspect aspect : candidates) {
                if ((mirror || available.getAmount(aspect) > 0) && essentia.getAmount(aspect) < capacity()
                    && source.takeFromContainer(aspect, 1)) {
                    essentia.add(aspect, 1);
                    sync();
                    showIntake(server, pos, aspect);
                    return;
                }
            }
        }
    }

    private void showIntake(ServerLevel server, BlockPos source, Aspect aspect) {
        ModNetwork.sendToNear(server, null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
            worldPosition.getZ() + 0.5, 32, new EssentiaSourceFxPayload(worldPosition,
                (byte) (worldPosition.getX() - source.getX()), (byte) (worldPosition.getY() - source.getY()),
                (byte) (worldPosition.getZ() - source.getZ()), aspect.getColor()));
    }

    private void upgrade(ServerLevel server) {
        if (pedestal == null) {
            // The original stores essentia in the base. Also accept pedestals level with the visible chamber.
            for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-3, -1, -3), worldPosition.offset(3, 0, 3))) {
                if (upgradePedestal(server, pos) != null) {
                    pedestal = pos.immutable();
                    break;
                }
            }
            return;
        }
        PedestalBlockEntity target = upgradePedestal(server, pedestal);
        if (target == null) {
            pedestal = null;
            upgradeProgress = 0;
            return;
        }
        if (++upgradeProgress >= UPGRADE_TICKS) {
            if (target.removeItem(0, 1).isEmpty()) {
                return;
            }
            upgrades++;
            pedestal = null;
            upgradeProgress = 0;
            sync();
        } else if (upgradeProgress % 2 == 0) {
            Vec3 start = Vec3.atCenterOf(pedestal).add(0, 0.65, 0);
            Vec3 end = Vec3.atCenterOf(worldPosition);
            Vec3 particle = start.lerp(end, (upgradeProgress % 24) / 24.0);
            server.sendParticles(UPGRADE_DUST, particle.x, particle.y, particle.z, 3, 0.08, 0.08, 0.08, 0);
        }
    }

    private static @Nullable PedestalBlockEntity upgradePedestal(ServerLevel server, BlockPos pos) {
        if (loaded(server, pos) && server.getBlockState(pos).is(AuraStructures.ANCIENT_PEDESTAL.get())
            && server.getBlockEntity(pos) instanceof PedestalBlockEntity tile
            && tile.getStack().is(Gadomancy.darknessItem())) {
            return tile;
        }
        return null;
    }

    private static boolean loaded(ServerLevel server, BlockPos pos) {
        return server.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    @Override
    void onDisassembled(ServerLevel server) {
        boolean hadEssentia = !essentia.isEmpty();
        essentia = new AspectList();
        upgrades = 0;
        pedestal = null;
        upgradeProgress = 0;
        sync();
        if (!hadEssentia) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(worldPosition);
        server.explode(null, center.x, center.y, center.z, 4, Level.ExplosionInteraction.BLOCK);
        var random = server.getRandom();
        for (int i = 0; i < 12; i++) {
            BlockPos pos = worldPosition.offset(random.nextInt(7) - 3, random.nextInt(5) - 2, random.nextInt(7) - 3);
            if (loaded(server, pos) && server.isEmptyBlock(pos)) {
                Block flux = pos.getY() < worldPosition.getY() ? ModBlocks.FLUX_GOO.get() : ModBlocks.FLUX_GAS.get();
                server.setBlockAndUpdate(pos, flux.defaultBlockState());
            }
        }
    }

    @Override
    public AspectList getAspects() {
        return active() ? essentia.copy() : new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
        if (active()) {
            essentia = new AspectList();
            for (Aspect aspect : aspects.getAspects()) {
                int count = Mth.clamp(aspects.getAmount(aspect), 0, capacity());
                if (count > 0) {
                    essentia.add(aspect, count);
                }
            }
            sync();
        }
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return active() && essentia.getAmount(aspect) < capacity();
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (amount <= 0 || !doesContainerAccept(aspect)) {
            return amount;
        }
        int accepted = Math.min(amount, capacity() - essentia.getAmount(aspect));
        essentia.add(aspect, accepted);
        sync();
        return amount - accepted;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (amount <= 0 || !doesContainerContainAmount(aspect, amount)) {
            return false;
        }
        essentia.remove(aspect, amount);
        sync();
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return active() && essentia.getAmount(aspect) >= amount;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return active() ? essentia.getAmount(aspect) : 0;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return canInputFrom(face);
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != null && !face.getAxis().isVertical() && active();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return canInputFrom(face);
    }

    @Override
    public @Nullable Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return canInputFrom(face) ? 16 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        int taken = Math.min(containerContains(aspect), amount);
        return canOutputTo(face) && takeFromContainer(aspect, taken) ? taken : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
    }

    @Override
    public @Nullable Aspect getEssentiaType(Direction face) {
        return canOutputTo(face) && !essentia.isEmpty() ? essentia.getAspects().getFirst() : null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        Aspect aspect = getEssentiaType(face);
        return aspect == null ? 0 : essentia.getAmount(aspect);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        essentia = input.read("essentia", AspectList.CODEC).orElseGet(AspectList::new);
        upgrades = Mth.clamp(input.getIntOr("upgrades", 0), 0, MAX_UPGRADES);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("essentia", AspectList.CODEC, essentia);
        output.putInt("upgrades", upgrades);
    }
}
