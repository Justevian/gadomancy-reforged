package makeo.gadomancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

final class ArcaneDropperTransfer extends SnapshotJournal<List<ItemStack>> implements ResourceHandler<ItemResource> {
    static final int OUTPUT_SLOTS = 10;
    private static final Map<ItemEntity, Extraction> EXTRACTIONS = new WeakHashMap<>();
    private final ServerLevel level;
    private final BlockPos pos;
    private final Direction facing;
    private final List<ItemStack> pending = new ArrayList<>(Collections.nCopies(OUTPUT_SLOTS, ItemStack.EMPTY));

    ArcaneDropperTransfer(ServerLevel level, BlockPos pos, Direction facing) {
        this.level = level;
        this.pos = pos.immutable();
        this.facing = facing;
    }

    private List<ItemEntity> items() {
        var items = new ArrayList<>(ArcaneDropperBlock.items(level, pos, facing));
        items.sort(Comparator.comparingInt(ItemEntity::getId));
        return items;
    }

    @Override
    public int size() {
        return items().size() + OUTPUT_SLOTS;
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(stackAt(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return stackAt(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        var items = items();
        if (index < items.size()) {
            return items.get(index).getItem().getCount();
        }
        return resource.isEmpty() ? 64 : resource.toStack(1).getMaxStackSize();
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && index >= items().size();
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int outputSlot = index - items().size();
        if (amount == 0 || outputSlot < 0) {
            return 0;
        }
        ItemStack current = pending.get(outputSlot);
        if (!current.isEmpty() && !resource.matches(current)) {
            return 0;
        }
        int inserted = Math.min(amount, resource.toStack(1).getMaxStackSize() - current.getCount());
        if (inserted > 0) {
            updateSnapshots(transaction);
            pending.set(outputSlot, resource.toStack(current.getCount() + inserted));
        }
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        var items = items();
        if (amount == 0 || index >= items.size()) {
            return 0;
        }
        ItemEntity item = items.get(index);
        ItemStack stack = item.getItem();
        if (item.hasPickUpDelay() || !resource.matches(stack)) {
            return 0;
        }
        synchronized (EXTRACTIONS) {
            EXTRACTIONS.computeIfAbsent(item, Extraction::new).updateSnapshots(transaction);
        }
        int extracted = Math.min(amount, stack.getCount());
        item.setItem(stack.copyWithCount(stack.getCount() - extracted));
        return extracted;
    }

    private ItemStack stackAt(int index) {
        var items = items();
        return index < items.size() ? items.get(index).getItem() : pending.get(index - items.size());
    }

    @Override
    protected List<ItemStack> createSnapshot() {
        return List.copyOf(pending);
    }

    @Override
    protected void revertToSnapshot(List<ItemStack> snapshot) {
        Collections.copy(pending, snapshot);
    }

    @Override
    protected void onRootCommit(List<ItemStack> original) {
        Vec3 direction = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        Vec3 outlet = Vec3.atCenterOf(pos).add(direction.scale(0.6)).add(0, -0.2, 0);
        Vec3 motion = direction.scale(0.1);
        for (ItemStack stack : pending) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(level, outlet.x, outlet.y, outlet.z, stack, motion.x, motion.y, motion.z);
            item.setPickUpDelay(3);
            level.addFreshEntity(item);
        }
        Collections.fill(pending, ItemStack.EMPTY);
    }

    private static final class Extraction extends SnapshotJournal<ItemStack> {
        private final java.lang.ref.WeakReference<ItemEntity> item;

        private Extraction(ItemEntity item) {
            this.item = new java.lang.ref.WeakReference<>(item);
        }

        @Override
        protected ItemStack createSnapshot() {
            return item.get().getItem().copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            ItemEntity entity = item.get();
            if (entity != null) {
                entity.setItem(snapshot);
            }
        }

        @Override
        protected void onRootCommit(ItemStack original) {
            ItemEntity entity = item.get();
            if (entity != null && entity.getItem().isEmpty()) {
                entity.discard();
            }
        }
    }
}
