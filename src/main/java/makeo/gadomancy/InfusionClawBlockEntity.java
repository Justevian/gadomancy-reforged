package makeo.gadomancy;

import java.util.UUID;
import makeo.gadomancy.mixin.InfusionMatrixAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandTriggers;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.PrimalFocus;

final class InfusionClawBlockEntity extends TcBlockEntity implements WorldlyContainer {
    private static final int DELAY = 290;
    private static final int COOLDOWN = 440;
    private static final int[] SLOT = {0};
    private static final int[] NO_SLOTS = {};

    private ItemStack wand = ItemStack.EMPTY;
    private @Nullable UUID owner;
    private boolean emptyOnly;
    private boolean powered;
    private boolean initialized;
    private int countdown;
    private boolean lastCrafting;

    InfusionClawBlockEntity(BlockPos pos, BlockState state) {
        super(InfusionClaws.TYPE.get(), pos, state);
    }

    void claim(Player player) {
        if (owner == null) {
            owner = player.getUUID();
            sync();
        }
    }

    void toggleEmptyOnly(Player player) {
        emptyOnly = !emptyOnly;
        player.sendSystemMessage(Component.translatable(emptyOnly
            ? "message.gadomancy.infusion_claw.empty_only" : "message.gadomancy.infusion_claw.any_charge"));
        sync();
    }

    boolean isRunning() {
        return countdown > 0;
    }

    boolean canInsert() {
        return !isRunning() && wand.isEmpty();
    }

    void tick(ServerLevel server) {
        BlockPos target = worldPosition.below();
        boolean signal = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            signal |= server.hasSignal(worldPosition.relative(side), side);
        }
        if (initialized && signal && !powered && !isRunning() && !wand.isEmpty() && owner != null) {
            if (server.getBlockEntity(target) instanceof InfusionMatrixBlockEntity
                || server.getBlockState(target).is(Blocks.CAULDRON) || server.getBlockState(target).is(Blocks.BOOKSHELF)) {
                countdown = COOLDOWN;
                setChanged();
            }
        }
        initialized = true;
        powered = signal;
        if (countdown > 0) {
            countdown--;
            setChanged();
            if (countdown == COOLDOWN - DELAY) {
                activate(server, target);
            }
        }
        boolean crafting = server.getBlockEntity(target) instanceof InfusionMatrixBlockEntity matrix && matrix.isCrafting();
        if (crafting != lastCrafting) {
            lastCrafting = crafting;
            server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void activate(ServerLevel server, BlockPos target) {
        ServerPlayer player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != server || !(wand.getItem() instanceof WandItem castingWand)
            || !server.mayInteract(player, target)) {
            return;
        }
        if (server.getBlockEntity(target) instanceof InfusionMatrixBlockEntity matrix) {
            if (matrix.isCrafting()) {
                return;
            }
            PrimalFocus focus = (PrimalFocus) ModItems.FOCUS_PRIMAL.get();
            AspectList cost = focus.getVisCost(wand);
            if (!castingWand.consumeAllVis(wand, player, cost, false, false)) {
                return;
            }
            matrix.onWandUseOn(server, target, player, wand, Direction.UP);
            if (matrix.isCrafting()) {
                castingWand.consumeAllVis(wand, player, cost, true, false);
                if (matrix instanceof InfusionMatrixAccessor access) {
                    int penalty = instabilityPenalty();
                    access.gadomancy$setRecipeInstability(access.gadomancy$getRecipeInstability() + penalty);
                    access.gadomancy$setInstability(matrix.getInstability() + penalty);
                    matrix.sync();
                }
            }
        } else if (server.getBlockState(target).is(Blocks.CAULDRON) || server.getBlockState(target).is(Blocks.BOOKSHELF)) {
            WandTriggers.perform(server, wand, player, target, Direction.UP);
        }
        server.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3,
            worldPosition.getZ() + 0.5, 15, 0.15, 0.25, 0.15, 0.04);
        sync();
    }

    private int instabilityPenalty() {
        int capacity = Wands.getMaxVis(wand) / 100;
        int penalty = 23 + (capacity < 100 ? 3 : -2) - Math.min(capacity, 300) / 30;
        penalty -= Wands.isStaff(wand) ? 6 : -1;
        penalty += (int) Math.floor((Wands.getCap(wand).baseCostModifier() - 0.4) * 3);
        return Math.max(1, penalty);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? SLOT : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side == Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.UP && !isRunning() && (!emptyOnly
            || AuraCores.PRIMALS.stream().allMatch(aspect -> Wands.getVis(stack, aspect) == 0));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return canInsert() && Wands.isWand(stack);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return wand.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? wand : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || isRunning()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = removeItemNoUpdate(slot);
        sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = wand;
        wand = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            wand = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            sync();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) <= 64;
    }

    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server && !wand.isEmpty()) {
            Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, removeItemNoUpdate(0));
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        wand = input.read("wand", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        emptyOnly = input.getBooleanOr("empty_only", false);
        countdown = input.getIntOr("countdown", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("wand", ItemStack.OPTIONAL_CODEC, wand);
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.putBoolean("empty_only", emptyOnly);
        output.putInt("countdown", countdown);
    }
}
