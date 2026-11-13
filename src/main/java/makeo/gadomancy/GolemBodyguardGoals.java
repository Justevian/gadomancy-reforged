package makeo.gadomancy;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import thaumcraft.entity.golem.GolemBase;

public final class GolemBodyguardGoals {
    private GolemBodyguardGoals() {}

    public static void install(GolemBase golem, GoalSelector goals, GoalSelector targets) {
        goals.addGoal(1, new MeleeAttackGoal(golem, 1.15, true));
        goals.addGoal(3, new FollowOwner(golem));
        targets.addGoal(1, new DefendOwner(golem));
    }

    public static boolean followingOwner(GolemBase golem) {
        ServerPlayer owner = owner(golem);
        return owner != null && owner.isAlive() && !owner.isSpectator();
    }

    private static ServerPlayer owner(GolemBase golem) {
        if (golem.level() instanceof ServerLevel server && golem.getOwnerUUID() != null) {
            return server.getPlayerByUUID(golem.getOwnerUUID()) instanceof ServerPlayer player ? player : null;
        }
        return null;
    }

    private static final class FollowOwner extends Goal {
        private final GolemBase golem;
        private ServerPlayer owner;
        private int recalculate;

        private FollowOwner(GolemBase golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            owner = owner(golem);
            return golem.isWorking() && owner != null && !owner.isSpectator() && golem.distanceToSqr(owner) > 16;
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isWorking() && owner != null && owner.level() == golem.level() && owner.isAlive()
                && !owner.isSpectator() && golem.distanceToSqr(owner) > 6;
        }

        @Override
        public void start() {
            recalculate = 0;
        }

        @Override
        public void tick() {
            if (owner != null && --recalculate <= 0) {
                recalculate = 5;
                if (golem.distanceToSqr(owner) < 144 || !catchUp()) {
                    golem.getNavigation().moveTo(owner, 1.15);
                }
            }
        }

        private boolean catchUp() {
            if (!(golem.level() instanceof ServerLevel level)) {
                return false;
            }
            var random = golem.getRandom();
            for (int attempt = 0; attempt < 10; attempt++) {
                BlockPos pos = owner.blockPosition().offset(random.nextInt(7) - 3, random.nextInt(3) - 1,
                    random.nextInt(7) - 3);
                Vec3 destination = Vec3.atBottomCenterOf(pos);
                if (!level.hasChunkAt(pos) || owner.distanceToSqr(destination) < 4
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.getFluidState(pos).isEmpty()
                    || !level.noCollision(golem, golem.getBoundingBox().move(destination.subtract(golem.position())))) {
                    continue;
                }
                golem.getNavigation().stop();
                golem.setPos(destination.x, destination.y, destination.z);
                return true;
            }
            return false;
        }

        @Override
        public void stop() {
            owner = null;
            golem.getNavigation().stop();
        }
    }

    private static final class DefendOwner extends Goal {
        private final GolemBase golem;
        private LivingEntity threat;
        private int lastAttack = -1;
        private int lastOwnerAttack = -1;
        private int lastGolemAttack = -1;

        private DefendOwner(GolemBase golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = owner(golem);
            if (!golem.isWorking() || owner == null || owner.isSpectator() || golem.distanceToSqr(owner) > 144) {
                return false;
            }
            int attacked = owner.getLastHurtByMobTimestamp();
            int ownerAttack = owner.getLastHurtMobTimestamp();
            int golemAttack = golem.getLastHurtByMobTimestamp();
            threat = attacked != lastAttack && owner.tickCount - attacked <= 100 ? owner.getLastHurtByMob()
                : ownerAttack != lastOwnerAttack && owner.tickCount - ownerAttack <= 100 ? owner.getLastHurtMob()
                : golemAttack != lastGolemAttack && golem.tickCount - golemAttack <= 100 ? golem.getLastHurtByMob() : null;
            lastAttack = attacked;
            lastOwnerAttack = ownerAttack;
            lastGolemAttack = golemAttack;
            return threat instanceof Enemy && threat.isAlive() && !owner.isAlliedTo(threat) && golem.canAttack(threat);
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = owner(golem);
            return owner != null && golem.isWorking() && threat != null && threat.isAlive()
                && owner.distanceToSqr(threat) < 144 && !owner.isAlliedTo(threat);
        }

        @Override
        public void start() {
            golem.setTarget(threat);
        }

        @Override
        public void stop() {
            if (golem.getTarget() == threat) {
                golem.setTarget(null);
            }
            threat = null;
        }
    }
}
