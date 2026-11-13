package makeo.gadomancy;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.essentia.Essentia;
import thaumcraft.essentia.EssentiaTransport;

final class EnervationEmitterBlockEntity extends TcBlockEntity implements EssentiaTransport {
    private static final int BUFFER = 8;
    private static final int MAX_RANGE = 15;
    private static final Map<ServerLevel, Set<EnervationEmitterBlockEntity>> EMITTERS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private int essentia;
    private int range;
    private int saturation;

    EnervationEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(EnervationEmitters.TYPE.get(), pos, state);
    }

    void tick(ServerLevel server) {
        long tick = server.getGameTime();
        if (tick % 5 == 0 && essentia < BUFFER) {
            EssentiaTransport source = Essentia.getConnectable(server, worldPosition, Direction.DOWN);
            if (source != null && source.canOutputTo(Direction.UP) && source.getSuctionAmount(Direction.UP) < 128
                && source.getMinimumSuction() <= 128) {
                int taken = source.takeEssentia(Aspect.ORDER, 1, Direction.UP);
                if (taken > 0) {
                    addEssentia(Aspect.ORDER, taken, Direction.DOWN);
                }
            }
        }
        if (tick % 15 == 0) {
            updateField(server);
        }
        if (range > 0) {
            EMITTERS.computeIfAbsent(server, ignored -> Collections.newSetFromMap(new WeakHashMap<>())).add(this);
            if (tick % 10 == 0) {
                server.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5, 2, 0.12, 0.12, 0.12, 0.015);
            }
        }
    }

    private void updateField(ServerLevel server) {
        int strength = Mth.clamp(server.getBestNeighborSignal(worldPosition), 0, MAX_RANGE);
        int previous = range;
        if (range > strength || (strength > 0 && saturation == 0 && essentia == 0)) {
            range = Math.max(0, range - 1);
            saturation = 0;
        } else if (saturation > 0) {
            saturation--;
        } else if (strength > 0 && essentia > 0) {
            essentia--;
            if (range < strength) {
                range++;
                saturation = 16 - range;
            }
        }
        if (range != previous) {
            if (range == 0) {
                unregister(server);
            }
            BlockState state = getBlockState();
            if ((range > 0) != state.getValue(EnervationEmitterBlock.ACTIVE)) {
                server.setBlockAndUpdate(worldPosition, state.setValue(EnervationEmitterBlock.ACTIVE, range > 0));
            }
            server.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
        setChanged();
    }

    int range() {
        return range;
    }

    static boolean protects(ServerLevel server, Vec3 location) {
        Set<EnervationEmitterBlockEntity> entries = EMITTERS.get(server);
        if (entries == null) {
            return false;
        }
        for (EnervationEmitterBlockEntity emitter : entries) {
            if (!emitter.isRemoved() && emitter.range > 0
                && new AABB(emitter.worldPosition).inflate(emitter.range).contains(location)) {
                return true;
            }
        }
        return false;
    }

    private void unregister(ServerLevel server) {
        Set<EnervationEmitterBlockEntity> entries = EMITTERS.get(server);
        if (entries != null) {
            entries.remove(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            unregister(server);
        }
        super.setRemoved();
    }

    @Override public boolean isConnectable(Direction side) { return side == Direction.DOWN; }
    @Override public boolean canInputFrom(Direction side) { return isConnectable(side); }
    @Override public boolean canOutputTo(Direction side) { return false; }
    @Override public void setSuction(@Nullable Aspect aspect, int suction) {}
    @Override public @Nullable Aspect getSuctionType(Direction side) { return canInputFrom(side) ? Aspect.ORDER : null; }
    @Override public int getSuctionAmount(Direction side) { return canInputFrom(side) && essentia < BUFFER ? 128 : 0; }
    @Override public int getMinimumSuction() { return 128; }
    @Override public boolean renderExtendedTube() { return false; }
    @Override public @Nullable Aspect getEssentiaType(Direction side) { return null; }
    @Override public int getEssentiaAmount(Direction side) { return 0; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction side) { return 0; }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction side) {
        if (aspect != Aspect.ORDER || amount <= 0 || !canInputFrom(side)) {
            return 0;
        }
        int added = Math.min(amount, BUFFER - essentia);
        essentia += added;
        setChanged();
        return added;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        essentia = Mth.clamp(input.getIntOr("essentia", 0), 0, BUFFER);
        range = Mth.clamp(input.getIntOr("range", 0), 0, MAX_RANGE);
        saturation = Mth.clamp(input.getIntOr("saturation", 0), 0, MAX_RANGE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("essentia", essentia);
        output.putInt("range", range);
        output.putInt("saturation", saturation);
    }
}
