package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.alchemy.AlembicBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.EssentiaReservoirBlockEntity;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.essentia.EssentiaTransport;

public final class CondenserGolemAccess {
    public static final int ALCHEMY_CORE = 6;

    private CondenserGolemAccess() {}

    public static boolean isCondenser(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(AuraStructures.CONDENSER.get());
    }

    public static @Nullable BlockPos controller(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!state.is(AuraStructures.CONDENSER.get()) || state.getValue(AuraStructureBlock.PART) == 0) {
            return null;
        }
        var kind = AuraStructureBlock.Kind.CONDENSER;
        BlockPos base = AuraStructureBlock.base(pos, state);
        return AuraStructureBlock.valid(level, base, kind) ? base.above(kind.controllerPart - 1) : null;
    }

    public static BlockPos approach(GolemBase golem, BlockPos pos) {
        Level level = golem.level();
        if (pos.equals(controller(level, pos))) {
            for (var marker : golem.getMarkers()) {
                if (marker.inLevel(level) && marker.pos().equals(pos) && !marker.side().getAxis().isVertical()) {
                    return pos.below().relative(marker.side());
                }
            }
        }
        return pos;
    }

    public static boolean isEssentiaSource(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof JarBlockEntity || entity instanceof AlembicBlockEntity
            || entity instanceof EssentiaReservoirBlockEntity;
    }

    public static UseOnContext clickAt(UseOnContext context, BlockPos pos) {
        var player = context.getPlayer();
        if (player == null) {
            return context;
        }
        Direction side = context.getClickedFace().getAxis().isVertical()
            ? player.getDirection().getOpposite() : context.getClickedFace();
        if (context.getClickedPos().equals(pos) && context.getClickedFace() == side) {
            return context;
        }
        Vec3 hit = Vec3.atCenterOf(pos).add(side.getStepX() * 0.5, 0, side.getStepZ() * 0.5);
        return new UseOnContext(player, context.getHand(), new BlockHitResult(hit, side, pos, false));
    }

    public static boolean canAcceptEssentia(EssentiaTransport target, Aspect aspect) {
        return !(target instanceof CondenserBlockEntity condenser) || condenser.doesContainerAccept(aspect);
    }

    public static Direction jarPort(BlockEntity entity, Direction fallback) {
        return entity instanceof MountedJarBlockEntity jar ? jar.port() : fallback;
    }

    public static boolean canBind(Level level, BlockPos pos, Direction side) {
        return !side.getAxis().isVertical() && pos.equals(controller(level, pos))
            && level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser && condenser.canOutputTo(side);
    }
}
