package makeo.gadomancy;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.Essentia;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.registry.ModItems;

final class PylonBlockEntity extends AuraStructureBlockEntity {
    private static final int BUFFER = 5;
    private ItemStack crystal = ItemStack.EMPTY;
    private int amount;
    private long tuneAfter;

    PylonBlockEntity(BlockPos pos, BlockState state) {
        super(AuraStructures.PYLON_TYPE.get(), pos, state);
    }

    private boolean active() {
        return isActive(AuraStructureBlock.Kind.PYLON);
    }

    static @Nullable Aspect crystalAspect(ItemStack stack) {
        if (!stack.is(ModItems.CRYSTALLIZED_ESSENCE.get())) {
            return null;
        }
        AspectList aspects = AspectKeys.ASPECTS.get(stack);
        return aspects != null && aspects.size() == 1 ? aspects.getAspects().getFirst() : null;
    }

    @Override
    void serverTick(ServerLevel server) {
        long tick = server.getGameTime();
        Vec3 flame = Vec3.atCenterOf(worldPosition.above(3)).add(0, 0.45, 0);
        Aspect aspect = crystalAspect(crystal);
        if (aspect == null) {
            if (tick % 8 == 0 && tick >= tuneAfter) {
                pickCrystal(server);
            }
            return;
        }
        if (tick % 16 == 0 && amount < BUFFER) {
            EssentiaTransport source = Essentia.getConnectable(server, worldPosition, Direction.DOWN);
            if (source != null && source.canOutputTo(Direction.UP)
                && source.getSuctionAmount(Direction.UP) < getSuctionAmount(Direction.DOWN)
                && source.getMinimumSuction() <= getSuctionAmount(Direction.DOWN)) {
                int taken = source.takeEssentia(aspect, 1, Direction.UP);
                if (taken > 0) {
                    addEssentia(aspect, taken, Direction.DOWN);
                }
            }
        }
        if (amount > 0) {
            PylonEffects.apply(server, flame, aspect, tick);
            if (tick % 32 == 0) {
                amount--;
                sync();
            }
        }
    }

    private void pickCrystal(ServerLevel server) {
        Vec3 center = Vec3.atCenterOf(worldPosition.above(2));
        ItemEntity item = server.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition.above(2)).inflate(8),
            entity -> entity.isAlive() && !entity.hasPickUpDelay() && crystalAspect(entity.getItem()) != null
        ).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(center))).orElse(null);
        if (item != null) {
            ItemStack stack = item.getItem();
            crystal = stack.copyWithCount(1);
            if (stack.getCount() == 1) {
                item.discard();
            } else {
                item.setItem(stack.copyWithCount(stack.getCount() - 1));
            }
            sync();
        }
    }

    boolean hasCrystal() {
        return !crystal.isEmpty();
    }

    @Nullable Aspect tunedAspect() {
        return crystalAspect(crystal);
    }

    boolean tune(ItemStack stack) {
        if (!active() || hasCrystal() || crystalAspect(stack) == null) {
            return false;
        }
        crystal = stack.copyWithCount(1);
        sync();
        return true;
    }

    ItemStack removeCrystal(ServerLevel server) {
        ItemStack returned = crystal;
        crystal = ItemStack.EMPTY;
        amount = 0;
        tuneAfter = server.getGameTime() + 80;
        sync();
        return returned;
    }

    @Override
    void onDisassembled(ServerLevel server) {
        if (hasCrystal()) {
            ItemStack removed = removeCrystal(server);
            Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, removed);
        }
    }

    @Override
    public boolean isConnectable(Direction face) {
        return canInputFrom(face);
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == Direction.DOWN && active();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public @Nullable Aspect getSuctionType(Direction face) {
        return canInputFrom(face) ? crystalAspect(crystal) : null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return amount < BUFFER && getSuctionType(face) != null ? 128 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int requested, Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int offered, Direction face) {
        if (offered <= 0 || aspect != getSuctionType(face) || amount >= BUFFER) {
            return 0;
        }
        int added = Math.min(offered, BUFFER - amount);
        amount += added;
        sync();
        return added;
    }

    @Override
    public @Nullable Aspect getEssentiaType(Direction face) {
        return getSuctionType(face);
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return canInputFrom(face) ? amount : 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        crystal = input.read("crystal", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        amount = Mth.clamp(input.getIntOr("amount", 0), 0, BUFFER);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("crystal", ItemStack.OPTIONAL_CODEC, crystal);
        output.putInt("amount", amount);
    }
}
