package makeo.gadomancy;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

final class PackagerMenu extends AbstractContainerMenu {
    static final int USE_ESSENTIA = 0;
    static final int AUTO_START = 1;
    static final int DISGUISE = 2;

    private final @Nullable PackagerBlockEntity packager;
    private final DataSlot progress;
    private final DataSlot settings;

    PackagerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(PackagerBlockEntity.SLOT_COUNT), null);
    }

    PackagerMenu(int id, Inventory inventory, PackagerBlockEntity packager) {
        this(id, inventory, packager, packager);
    }

    private PackagerMenu(int id, Inventory inventory, Container storage, @Nullable PackagerBlockEntity packager) {
        super(ArcanePackagers.MENU.get(), id);
        this.packager = packager;
        progress = addDataSlot(packager == null ? DataSlot.standalone() : serverData(packager::progress));
        settings = addDataSlot(packager == null ? DataSlot.standalone() : serverData(packager::settings));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addPackagerSlot(storage, column + row * 3, 16 + column * 18, 42 + row * 23);
            }
        }
        addPackagerSlot(storage, PackagerBlockEntity.LEATHER, 95, 33);
        addPackagerSlot(storage, PackagerBlockEntity.STRING, 113, 33);
        addPackagerSlot(storage, PackagerBlockEntity.OUTPUT, 160, 64);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        }
    }

    private static DataSlot serverData(java.util.function.IntSupplier value) {
        return new DataSlot() {
            @Override
            public int get() {
                return value.getAsInt();
            }

            @Override
            public void set(int ignored) {
            }
        };
    }

    private void addPackagerSlot(Container storage, int index, int x, int y) {
        addSlot(new Slot(storage, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (packager != null) {
                    return packager.canPlaceItem(index, stack);
                }
                return progress() == 0 && (index < PackagerBlockEntity.LEATHER || !enabled(USE_ESSENTIA)
                    && (index == PackagerBlockEntity.LEATHER && stack.is(Items.LEATHER)
                        || index == PackagerBlockEntity.STRING && stack.is(Items.STRING)));
            }

            @Override
            public boolean mayPickup(Player player) {
                return progress() == 0;
            }
        });
    }

    int progress() {
        return progress.get();
    }

    boolean enabled(int button) {
        return (settings.get() & (1 << button)) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (packager == null || !stillValid(player) || packager.isPacking() || button < USE_ESSENTIA || button > DISGUISE) {
            return false;
        }
        packager.toggleSetting(button);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return packager == null || packager.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PackagerBlockEntity.SLOT_COUNT) {
            moveItemStackTo(stack, PackagerBlockEntity.SLOT_COUNT, slots.size(), true);
        } else if (progress() == 0) {
            if (!enabled(USE_ESSENTIA) && (stack.is(Items.LEATHER) || stack.is(Items.STRING))) {
                int supply = stack.is(Items.LEATHER) ? PackagerBlockEntity.LEATHER : PackagerBlockEntity.STRING;
                moveItemStackTo(stack, supply, supply + 1, false);
            }
            if (!stack.isEmpty()) {
                moveItemStackTo(stack, 0, PackagerBlockEntity.LEATHER, false);
            }
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
