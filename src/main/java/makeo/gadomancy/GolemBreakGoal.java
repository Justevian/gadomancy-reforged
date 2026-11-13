package makeo.gadomancy;

import java.util.EnumSet;
import makeo.gadomancy.mixin.GolemActionsInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.Marker;

public final class GolemBreakGoal extends Goal {
    private final GolemBase golem;
    private BlockPos target;
    private BlockState state;
    private Path approach;
    private int nextSearch;
    private int started;
    private float progress;
    private boolean done;

    public GolemBreakGoal(GolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!golem.isWorking() || golem.tickCount < nextSearch || !(golem.level() instanceof ServerLevel server)) {
            return false;
        }
        nextSearch = golem.tickCount + 20;
        ServerPlayer owner = owner(server);
        if (owner == null) {
            return false;
        }
        double range = golem.getRange();
        for (Marker marker : golem.getMarkers()) {
            BlockPos pos = marker.pos();
            if (!marker.inLevel(server) || !server.hasChunkAt(pos) || !server.mayInteract(owner, pos)
                || !pos.closerThan(golem.getHomePosition(), range)) {
                continue;
            }
            BlockState candidate = server.getBlockState(pos);
            if (candidate.isAir() || !candidate.getFluidState().isEmpty() || candidate.getDestroySpeed(server, pos) < 0
                || !matchesFilter(candidate, marker.color())) {
                continue;
            }
            boolean reachable = withinReach(pos);
            Path path = reachable ? null : findApproach(pos, marker.side());
            if (!reachable && path == null) {
                continue;
            }
            target = pos;
            state = candidate;
            approach = path;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !done && target != null && golem.isWorking() && golem.level().hasChunkAt(target)
            && golem.level().getBlockState(target) == state && (withinReach(target) || !golem.getNavigation().isDone())
            && (withinReach(target) || golem.tickCount - started < 200);
    }

    @Override
    public void start() {
        started = golem.tickCount;
        progress = 0;
        done = false;
        if (approach != null) {
            done = !golem.getNavigation().moveTo(approach, 1);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || !(golem.level() instanceof ServerLevel level) || !withinReach(target)) {
            return;
        }
        ServerPlayer owner = owner(level);
        Vec3 center = Vec3.atCenterOf(target);
        if (owner == null || !level.mayInteract(owner, target)) {
            done = true;
            return;
        }
        golem.getNavigation().stop();
        golem.getLookControl().setLookAt(center.x, center.y, center.z, 30, 30);
        ItemStack tool = golem.getCarried();
        float hardness = state.getDestroySpeed(level, target);
        float speed = tool.getDestroySpeed(state) * (1 + golem.getGolemStrength() * 0.2F);
        progress += hardness == 0 ? 1 : speed / (hardness * (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state) ? 100 : 30));
        golem.startLeftArmTimer();
        if (progress < 1) {
            level.destroyBlockProgress(golem.getId(), target, Math.clamp((int) (progress * 10), 0, 9));
            return;
        }
        ServerPlayer actor = GolemActionsInvoker.gadomancy$prepare(golem, tool);
        try {
            actor.gameMode.destroyBlock(target);
        } finally {
            golem.setCarried(GolemActionsInvoker.gadomancy$finish(golem, actor));
        }
        done = true;
    }

    @Override
    public void stop() {
        if (target != null && golem.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(golem.getId(), target, -1);
        }
        target = null;
        state = null;
        approach = null;
        golem.getNavigation().stop();
    }

    private Path findApproach(BlockPos pos, Direction markedSide) {
        Path marked = pathFrom(pos, pos.relative(markedSide));
        if (marked != null) {
            return marked;
        }
        for (BlockPos standing : BlockPos.spiralAround(pos, 2, Direction.EAST, Direction.SOUTH)) {
            for (int dy : new int[] {0, -1, 1}) {
                Path path = pathFrom(pos, standing.offset(0, dy, 0));
                if (path != null) {
                    return path;
                }
            }
        }
        return null;
    }

    private Path pathFrom(BlockPos target, BlockPos standing) {
        var level = golem.level();
        if (standing.equals(target) || !level.hasChunkAt(standing)
            || !level.getFluidState(standing).isEmpty()
            || !level.getBlockState(standing.below()).isFaceSturdy(level, standing.below(), Direction.UP)) {
            return null;
        }
        Vec3 feet = Vec3.atBottomCenterOf(standing);
        if (feet.distanceToSqr(Vec3.atCenterOf(target)) > 9
            || !canSee(feet.add(0, golem.getEyeHeight(), 0), target)) {
            return null;
        }
        Path path = golem.getNavigation().createPath(standing, 0);
        return path != null && path.canReach() ? path : null;
    }

    private boolean withinReach(BlockPos pos) {
        return golem.distanceToSqr(Vec3.atCenterOf(pos)) <= 9 && canSee(golem.getEyePosition(), pos);
    }

    private boolean canSee(Vec3 eye, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (hitsTarget(eye, center, pos) || hitsTarget(eye, center.add(0, 0.48, 0), pos)) {
            return true;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (hitsTarget(eye, center.add(side.getStepX() * 0.48, 0.4, side.getStepZ() * 0.48), pos)) {
                return true;
            }
        }
        return false;
    }

    private boolean hitsTarget(Vec3 eye, Vec3 point, BlockPos pos) {
        var hit = golem.level().clip(new ClipContext(eye, point,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, golem));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && pos.equals(hit.getBlockPos());
    }

    private ServerPlayer owner(ServerLevel level) {
        return golem.getOwnerUUID() != null && level.getPlayerByUUID(golem.getOwnerUUID()) instanceof ServerPlayer player
            ? player : null;
    }

    private boolean matchesFilter(BlockState block, int color) {
        boolean filtered = false;
        for (int slot = 0; slot < golem.inventory().getContainerSize(); slot++) {
            if (golem.inventory().getItem(slot).getItem() instanceof BlockItem item) {
                filtered = true;
                int filterColor = golem.getColors(slot);
                if (block.is(item.getBlock()) && (color < 0 || filterColor < 0 || filterColor == color)) {
                    return true;
                }
            }
        }
        return !filtered;
    }
}
