package makeo.gadomancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import net.minecraft.world.level.block.entity.BlockEntityType;

final class RemoteJarBlockEntity extends MountedJarBlockEntity {
    private static final Map<MinecraftServer, Map<UUID, Network>> NETWORKS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private @Nullable UUID network;

    RemoteJarBlockEntity(BlockPos pos, BlockState state) {
        this(RemoteJars.TYPE.get(), pos, state);
    }

    RemoteJarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, false);
    }

    @Nullable UUID network() {
        return network;
    }

    void setNetwork(@Nullable UUID id) {
        if (!Objects.equals(network, id)) {
            leaveNetwork();
            network = id;
            sync();
        }
    }

    @Override
    protected void tick() {
        super.tick();
        if (network != null && level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            Network members = NETWORKS.computeIfAbsent(server, ignored -> new HashMap<>())
                .computeIfAbsent(network, ignored -> new Network());
            members.jars.add(this);
            if (server.getTickCount() % 4 == 0) {
                members.balance(server.getTickCount());
            }
        }
    }

    @Override
    public void setRemoved() {
        leaveNetwork();
        super.setRemoved();
    }

    private void leaveNetwork() {
        if (network == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Map<UUID, Network> networks = NETWORKS.get(serverLevel.getServer());
        if (networks == null) {
            return;
        }
        Network members = networks.get(network);
        if (members != null) {
            members.jars.remove(this);
            if (members.jars.isEmpty()) {
                networks.remove(network);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        UUID id = input.read("network", UUIDUtil.CODEC).orElse(null);
        if (!Objects.equals(network, id)) {
            leaveNetwork();
            network = id;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("network", UUIDUtil.CODEC, network);
    }

    private static final class Network {
        private final Set<RemoteJarBlockEntity> jars = Collections.newSetFromMap(new WeakHashMap<>());
        private int lastTick = Integer.MIN_VALUE;

        void balance(int tick) {
            if (lastTick == tick) {
                return;
            }
            lastTick = tick;

            var active = new ArrayList<>(jars);
            active.removeIf(jar -> !jar.isLoaded());
            RemoteJarBlockEntity source = null;
            RemoteJarBlockEntity destination = null;
            int difference = 1;
            for (RemoteJarBlockEntity candidate : active) {
                Aspect aspect = candidate.getAspect();
                if (aspect == null || candidate.getAmount() <= difference) {
                    continue;
                }
                for (RemoteJarBlockEntity other : active) {
                    int gap = candidate.getAmount() - other.getAmount();
                    if (gap > difference && other.accepts(aspect)) {
                        difference = gap;
                        source = candidate;
                        destination = other;
                    }
                }
            }

            if (source != null && destination != null) {
                Aspect aspect = Objects.requireNonNull(source.getAspect());
                if (source.takeFromContainer(aspect, 1) && destination.addToContainer(aspect, 1) != 0) {
                    source.addToContainer(aspect, 1);
                }
            }
        }
    }

    private boolean isLoaded() {
        return !isRemoved() && level instanceof ServerLevel server
            && server.hasChunk(SectionPos.blockToSectionCoord(worldPosition.getX()),
                SectionPos.blockToSectionCoord(worldPosition.getZ()));
    }

    private boolean accepts(Aspect aspect) {
        Aspect filter = getAspectFilter();
        return (filter == null || filter == aspect) && getAmount() < getMaxAmount()
            && (getAmount() == 0 || getAspect() == aspect);
    }
}
