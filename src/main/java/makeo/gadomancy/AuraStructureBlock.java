package makeo.gadomancy;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.essentia.PhialItem;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchGate;
import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.WandItem;

final class AuraStructureBlock extends Block implements EntityBlock, WandInteractable {
    static final IntegerProperty PART = IntegerProperty.create("part", 0, 4);
    static Consumer<AuraStructureBlockEntity> visualTick = entity -> {};

    enum Kind {
        CONDENSER(3, 2),
        PYLON(4, 1);

        final int height;
        final int controllerPart;

        Kind(int height, int controllerPart) {
            this.height = height;
            this.controllerPart = controllerPart;
        }
    }

    final Kind kind;

    AuraStructureBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.CONDENSER ? new CondenserBlockEntity(pos, state) : new PylonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != kind.controllerPart
            || type != (kind == Kind.CONDENSER ? AuraStructures.CONDENSER_TYPE.get() : AuraStructures.PYLON_TYPE.get())) {
            return null;
        }
        return (world, pos, current, entity) -> {
            if (valid(world, base(pos, current), kind)) {
                AuraStructureBlockEntity structure = (AuraStructureBlockEntity) entity;
                if (world instanceof ServerLevel server) {
                    structure.serverTick(server);
                } else if (world.isClientSide()) {
                    visualTick.accept(structure);
                }
            }
        };
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack stack, Direction side) {
        if (!(stack.getItem() instanceof WandItem wand) || level.getBlockState(pos).getValue(PART) != 0
            || !ResearchGate.isComplete(player, AuraStructures.research(kind))) {
            return InteractionResult.PASS;
        }
        BlockPos bottom = findUnassembledBase(level, pos, kind);
        if (bottom == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            for (int i = 0; i < kind.height; i++) {
                if (!server.mayInteract(player, bottom.above(i))) {
                    return InteractionResult.FAIL;
                }
            }
            AspectList cost = AuraStructures.assemblyCost(kind);
            if (!wand.consumeAllVisCrafting(stack, player, cost, true)) {
                return InteractionResult.FAIL;
            }
            for (int i = 0; i < kind.height; i++) {
                BlockPos part = bottom.above(i);
                server.setBlock(part, server.getBlockState(part).setValue(PART, i + 1), UPDATE_ALL);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(PART) == 0 || !valid(level, base(pos, state), kind)) {
            return InteractionResult.PASS;
        }
        BlockPos master = base(pos, state).above(kind.controllerPart - 1);
        if (kind == Kind.PYLON && PylonBlockEntity.crystalAspect(stack) != null
            && level.getBlockEntity(master) instanceof PylonBlockEntity pylon && !pylon.hasCrystal()) {
            if (level instanceof ServerLevel server) {
                if (!server.mayInteract(player, pos) || !pylon.tune(stack)) {
                    return InteractionResult.FAIL;
                }
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (kind == Kind.CONDENSER && stack.getItem() instanceof PhialItem phial
            && level.getBlockEntity(master) instanceof CondenserBlockEntity condenser) {
            return transferPhial(stack, phial, condenser, player);
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult transferPhial(ItemStack stack, PhialItem phial,
        CondenserBlockEntity condenser, Player player) {
        boolean filled = phial.isFilled();
        AspectList aspects = filled ? AspectKeys.ASPECTS.get(stack) : condenser.getAspects();
        if (aspects == null || aspects.isEmpty() || filled && aspects.size() != 1) {
            return InteractionResult.PASS;
        }
        int amount = PhialItem.CAPACITY;
        Aspect aspect = aspects.getAspects().stream().filter(candidate -> aspects.getAmount(candidate) >= amount)
            .findFirst().orElse(null);
        if (aspect == null || filled && (aspects.getAmount(aspect) != amount
            || condenser.containerContains(aspect) > condenser.capacity() - amount)) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel level) {
            if (!level.mayInteract(player, condenser.getBlockPos())) {
                return InteractionResult.FAIL;
            }
            ItemStack output;
            if (filled) {
                if (condenser.addToContainer(aspect, amount) != 0) {
                    return InteractionResult.FAIL;
                }
                output = new ItemStack(ModItems.GLASS_PHIAL.get());
            } else {
                if (!condenser.takeFromContainer(aspect, amount)) {
                    return InteractionResult.FAIL;
                }
                output = new ItemStack(ModItems.ESSENTIA_PHIAL.get());
                AspectKeys.ASPECTS.set(output, new AspectList().add(aspect, amount));
            }
            stack.consume(1, player);
            if (!player.addItem(output)) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), output);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind == Kind.PYLON && player.isShiftKeyDown() && state.getValue(PART) != 0
            && level.getBlockEntity(base(pos, state)) instanceof PylonBlockEntity pylon && pylon.hasCrystal()) {
            if (level instanceof ServerLevel server && server.mayInteract(player, pos)) {
                ItemStack crystal = pylon.removeCrystal(server);
                if (!player.addItem(crystal)) {
                    Containers.dropItemStack(server, player.getX(), player.getY(), player.getZ(), crystal);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static @Nullable BlockPos findUnassembledBase(Level level, BlockPos pos, Kind kind) {
        for (int offset = 0; offset < kind.height; offset++) {
            BlockPos bottom = pos.below(offset);
            if (matches(level, bottom, kind, false)) {
                return bottom;
            }
        }
        return null;
    }

    static BlockPos base(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART) - 1);
    }

    static boolean valid(Level level, BlockPos bottom, Kind kind) {
        return matches(level, bottom, kind, true);
    }

    private static boolean matches(Level level, BlockPos bottom, Kind kind, boolean assembled) {
        for (int i = 0; i < kind.height; i++) {
            BlockPos pos = bottom.above(i);
            if (!level.isInWorldBounds(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(AuraStructures.part(kind, i)) || state.getValue(PART) != (assembled ? i + 1 : 0)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(PART) != 0 && !level.getBlockState(pos).is(this)) {
            BlockPos bottom = base(pos, state);
            BlockPos controller = bottom.above(kind.controllerPart - 1);
            AuraStructureBlockEntity master = level.getBlockEntity(controller) instanceof AuraStructureBlockEntity structure ? structure : null;
            disassemble(level, bottom, kind);
            if (master != null) {
                master.onDisassembled(level);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    static void disassemble(ServerLevel level, BlockPos bottom, Kind kind) {
        for (int i = 0; i < kind.height; i++) {
            BlockPos pos = bottom.above(i);
            BlockState current = level.getBlockState(pos);
            if (current.is(AuraStructures.part(kind, i)) && current.getValue(PART) == i + 1) {
                level.setBlock(pos, current.setValue(PART, 0), UPDATE_ALL);
            }
        }
    }
}
