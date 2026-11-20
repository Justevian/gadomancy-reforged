package makeo.gadomancy;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
/** Simulated item capability backed by item entities in the intake area. */
final class ArcaneDropperTransfer implements IItemHandler {
    static final int OUTPUT_SLOTS = 10;
    private final ServerLevel level;
    private final BlockPos pos;
    private final Direction facing;
    ArcaneDropperTransfer(ServerLevel level, BlockPos pos, Direction facing) {
        this.level=level; this.pos=pos.immutable(); this.facing=facing;
    }
    private java.util.List<ItemEntity> items() {
        var items=new ArrayList<>(ArcaneDropperBlock.items(level,pos,facing));
        items.sort(Comparator.comparingInt(ItemEntity::getId)); return items;
    }
    @Override public int getSlots() { return items().size()+OUTPUT_SLOTS; }
    @Override public int getSlotLimit(int slot) { return 64; }
    @Override public ItemStack getStackInSlot(int slot) {
        var items=items(); return slot>=0 && slot<items.size() ? items.get(slot).getItem() : ItemStack.EMPTY;
    }
    @Override public boolean isItemValid(int slot,ItemStack stack) {
        int inputs=items().size(); return !stack.isEmpty() && slot>=inputs && slot<inputs+OUTPUT_SLOTS;
    }
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        if (!isItemValid(slot,stack)) return stack;
        int inserted=Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()));
        if (!simulate) {
            Vec3 direction=new Vec3(facing.getStepX(),facing.getStepY(),facing.getStepZ());
            Vec3 outlet=Vec3.atCenterOf(pos).add(direction.scale(0.6)).add(0,-0.2,0);
            Vec3 motion=direction.scale(0.1);
            var entity=new ItemEntity(level,outlet.x,outlet.y,outlet.z,stack.copyWithCount(inserted),motion.x,motion.y,motion.z);
            entity.setPickUpDelay(3);
            if (!level.addFreshEntity(entity)) return stack;
        }
        return stack.copyWithCount(stack.getCount()-inserted);
    }
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
        var items=items(); if (amount<=0 || slot<0 || slot>=items.size()) return ItemStack.EMPTY;
        var entity=items.get(slot); if (entity.hasPickUpDelay()) return ItemStack.EMPTY;
        var stack=entity.getItem(); int extracted=Math.min(amount,stack.getCount());
        var result=stack.copyWithCount(extracted);
        if (!simulate) {
            if (extracted==stack.getCount()) entity.discard();
            else entity.setItem(stack.copyWithCount(stack.getCount()-extracted));
        }
        return result;
    }
}
