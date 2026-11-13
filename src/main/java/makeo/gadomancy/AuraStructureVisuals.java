package makeo.gadomancy;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.fx.EssentiaTrailFx;
import thaumcraft.client.fx.WispFx;
import thaumcraft.client.platform.WorldFxRenderer;

final class AuraStructureVisuals {
    private AuraStructureVisuals() {}

    static void tick(AuraStructureBlockEntity entity) {
        if (entity instanceof CondenserBlockEntity condenser) {
            condenser(condenser);
        } else if (entity instanceof PylonBlockEntity pylon) {
            pylon(pylon);
        }
    }

    private static void condenser(CondenserBlockEntity entity) {
        var level = entity.getLevel();
        if (level == null || level.getGameTime() % 2 != 0) {
            return;
        }
        var aspects = entity.getAspects().getAspects();
        if (aspects.isEmpty()) {
            return;
        }
        long ticks = level.getGameTime();
        int color = aspects.get((int) ((ticks / 20) % aspects.size())).getColor();
        Vec3 center = Vec3.atCenterOf(entity.getBlockPos());
        RandomSource random = level.getRandom();
        trail(random, center.add(0, -1.1, 0), center, ticks, color, 0.8F, 0.025);
        trail(random, center.add(0, 1.1, 0), center, ticks + 4, color, 0.8F, -0.025);
    }

    private static void pylon(PylonBlockEntity entity) {
        var level = entity.getLevel();
        Aspect aspect = entity.tunedAspect();
        if (level == null || aspect == null) {
            return;
        }
        long ticks = level.getGameTime();
        RandomSource random = level.getRandom();
        int color = aspect.getColor();
        boolean dark = aspect == Aspect.ENTROPY || aspect == Aspect.DARKNESS || aspect == Aspect.UNDEAD;
        Vec3 cap = Vec3.atCenterOf(entity.getBlockPos().above(3)).add(0, 0.2, 0);
        if (random.nextInt(12) == 0) {
            wisp(random, cap, cap.add(random.nextDouble() * 0.4 - 0.2, 0.28, random.nextDouble() * 0.4 - 0.2),
                color, 0.5F, dark);
        }
        if (entity.getEssentiaAmount(net.minecraft.core.Direction.DOWN) <= 0) {
            return;
        }
        if (ticks % 2 == 0) {
            Vec3 base = Vec3.atCenterOf(entity.getBlockPos());
            trail(random, base.add(0, -0.3, 0), cap, ticks, color, 1F, 0.1);
        }
        if (ticks % 3 == 0) {
            for (int i = 0; i < 3; i++) {
                double phase = ticks * (Math.PI * 2 / (65 + i * 13)) + i * (Math.PI * 2 / 3);
                Vec3 orbit = cap.add(Math.cos(phase) * (2.1 + i * 0.26),
                    Math.sin(phase + i) * (0.65 + i * 0.2), Math.sin(phase) * (2.1 + i * 0.26));
                Vec3 next = cap.add(Math.cos(phase + 0.25) * (2.1 + i * 0.26),
                    Math.sin(phase + 0.25 + i) * (0.65 + i * 0.2), Math.sin(phase + 0.25) * (2.1 + i * 0.26));
                addWisp(new OrbitWisp(random, orbit, dark), next, color, dark);
            }
        }
    }

    private static void trail(RandomSource random, Vec3 origin, Vec3 target, long tick, int color,
        float scale, double rise) {
        var effect = new EssentiaTrailFx(random, origin.x, origin.y, origin.z, target.x, target.y, target.z,
            (int) (tick % 1200), color, scale);
        effect.motion(random.nextGaussian() * 0.012, rise, random.nextGaussian() * 0.012);
        effect.gravity(0);
        WorldFxRenderer.add(effect);
    }

    private static void wisp(RandomSource random, Vec3 origin, Vec3 target, int color, float size, boolean dark) {
        addWisp(new WispFx(random, origin.x, origin.y, origin.z, size, dark ? 5 : 6, null), target, color, dark);
    }

    private static void addWisp(WispFx effect, Vec3 target, int color, boolean dark) {
        if (!dark) {
            effect.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
        }
        effect.moveTo(target.x, target.y, target.z).shrink(true).gravity(-0.04F);
        WorldFxRenderer.add(effect);
    }

    private static final class OrbitWisp extends WispFx {
        OrbitWisp(RandomSource random, Vec3 pos, boolean dark) {
            super(random, pos.x, pos.y, pos.z, 0.16F, dark ? 5 : 6, null);
            maxAge = 11;
        }
    }
}
