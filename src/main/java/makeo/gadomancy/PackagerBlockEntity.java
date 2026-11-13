package makeo.gadomancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.essentia.Essentia;
import thaumcraft.essentia.EssentiaTransport;

final class PackagerBlockEntity extends TcBlockEntity implements WorldlyContainer, EssentiaTransport {
    static final int SLOT_COUNT = 12;
    static final int LEATHER = 9;
    static final int STRING = 10;
    static final int OUTPUT = 11;
    private static final int BUFFER = 8;
    private static final int PACK_TICKS = 47;
    private static final int[] IN = {LEATHER, STRING, 0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUT = {OUTPUT};
    private static final int[] NONE = {};
    private final List<ItemStack> items = new ArrayList<>(Collections.nCopies(SLOT_COUNT, ItemStack.EMPTY));
    private boolean automatic;
    private boolean useEssentia;
    private boolean disguise;
    private boolean powered;
    private boolean initialized;
    private int remaining;
    private int fuel;

    PackagerBlockEntity(BlockPos pos, BlockState state) {
        super(ArcanePackagers.TYPE.get(), pos, state);
    }

    void tick(ServerLevel server) {
        long tick = server.getGameTime();
        if (tick % 5 == 0 && fuel < BUFFER) {
            EssentiaTransport source = Essentia.getConnectable(server, worldPosition, Direction.UP);
            if (source != null && source.canOutputTo(Direction.DOWN) && source.getSuctionAmount(Direction.DOWN) < 128
                && source.getMinimumSuction() <= 128) {
                int taken = source.takeEssentia(Aspect.CLOTH, 1, Direction.DOWN);
                if (taken > 0) {
                    addEssentia(Aspect.CLOTH, taken, Direction.UP);
                }
            }
        }
        boolean currentPower = server.hasNeighborSignal(worldPosition);
        if (remaining > 0) {
            if (--remaining == 0) {
                pack(server);
            }
            setChanged();
        } else if (((initialized && currentPower && !powered) || (automatic && tick % 5 == 0)) && canPack()) {
            remaining = PACK_TICKS;
            setChanged();
        }
        powered = currentPower;
        initialized = true;
    }

    boolean isPacking() {
        return remaining > 0;
    }

    int progress() {
        return isPacking() ? Math.min(46, PACK_TICKS - remaining + 1) : 0;
    }

    int settings() {
        return (useEssentia ? 1 << PackagerMenu.USE_ESSENTIA : 0)
            | (automatic ? 1 << PackagerMenu.AUTO_START : 0)
            | (disguise ? 1 << PackagerMenu.DISGUISE : 0);
    }

    private boolean canPack() {
        return items.get(OUTPUT).isEmpty() && items.subList(0, LEATHER).stream().anyMatch(stack -> !stack.isEmpty())
            && (useEssentia ? fuel >= 4 : items.get(LEATHER).is(Items.LEATHER) && items.get(STRING).is(Items.STRING));
    }

    private void pack(ServerLevel server) {
        if (!canPack()) {
            return;
        }
        List<ItemStack> contents = items.subList(0, LEATHER).stream().filter(stack -> !stack.isEmpty())
            .map(ItemStack::copy).toList();
        ItemStack result = new ItemStack(disguise ? ArcanePackagers.DISGUISED_PACKAGE.get() : ArcanePackagers.PACKAGE.get());
        if (!PackageContents.load(server, result, contents)) {
            return;
        }
        if (useEssentia) {
            fuel -= 4;
        } else {
            items.get(LEATHER).shrink(1);
            items.get(STRING).shrink(1);
        }
        Collections.fill(items.subList(0, LEATHER), ItemStack.EMPTY);
        items.set(OUTPUT, result);
        setChanged();
    }

    void toggleSetting(int button) {
        if (isPacking()) {
            return;
        }
        switch (button) {
            case PackagerMenu.AUTO_START -> automatic = !automatic;
            case PackagerMenu.USE_ESSENTIA -> useEssentia = !useEssentia;
            case PackagerMenu.DISGUISE -> disguise = !disguise;
            default -> {
                return;
            }
        }
        setChanged();
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (isPacking() || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot).split(amount);
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot < SLOT_COUNT ? items.set(slot, ItemStack.EMPTY) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < SLOT_COUNT) {
            items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !isPacking() && (slot < LEATHER
            || !useEssentia && (slot == LEATHER && stack.is(Items.LEATHER) || slot == STRING && stack.is(Items.STRING)));
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64;
    }

    @Override
    public void clearContent() {
        Collections.fill(items, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUT;
        }
        return side == getBlockState().getValue(PackagerBlock.FACING) ? IN : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side == getBlockState().getValue(PackagerBlock.FACING) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isPacking() && side == Direction.DOWN && slot == OUTPUT;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            Vec3 center = Vec3.atCenterOf(pos);
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                ItemStack item = removeItemNoUpdate(slot);
                if (!item.isEmpty()) {
                    Containers.dropItemStack(server, center.x, center.y, center.z, item);
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override public boolean isConnectable(Direction face) { return face == Direction.UP; }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return false; }
    @Override public void setSuction(@Nullable Aspect aspect, int amount) {}
    @Override public @Nullable Aspect getSuctionType(Direction face) { return canInputFrom(face) ? Aspect.CLOTH : null; }
    @Override public int getSuctionAmount(Direction face) { return canInputFrom(face) && fuel < BUFFER ? 128 : 0; }
    @Override public int getMinimumSuction() { return 128; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    @Override public @Nullable Aspect getEssentiaType(Direction face) { return null; }
    @Override public int getEssentiaAmount(Direction face) { return 0; }
    @Override public boolean renderExtendedTube() { return false; }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (aspect != Aspect.CLOTH || amount <= 0 || !canInputFrom(face)) {
            return 0;
        }
        int accepted = Math.min(amount, BUFFER - fuel);
        fuel += accepted;
        setChanged();
        return accepted;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Collections.fill(items, ItemStack.EMPTY);
        List<ItemStack> stored = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(stored.size(), SLOT_COUNT); i++) {
            items.set(i, stored.get(i));
        }
        automatic = input.getBooleanOr("automatic", false);
        useEssentia = input.getBooleanOr("use_essentia", false);
        disguise = input.getBooleanOr("disguise", false);
        fuel = Mth.clamp(input.getIntOr("fuel", 0), 0, BUFFER);
        remaining = Mth.clamp(input.getIntOr("remaining", 0), 0, PACK_TICKS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.putBoolean("automatic", automatic);
        output.putBoolean("use_essentia", useEssentia);
        output.putBoolean("disguise", disguise);
        output.putInt("fuel", fuel);
        output.putInt("remaining", remaining);
    }
}
