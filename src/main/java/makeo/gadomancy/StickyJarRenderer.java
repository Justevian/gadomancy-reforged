package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.render.EssentiaJarRenderer;
import thaumcraft.essentia.JarBlockEntity;

final class StickyJarRenderer extends EssentiaJarRenderer {
    private static final class MountedState extends State {
        private Direction mount = Direction.UP;
    }

    StickyJarRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new MountedState();
    }

    @Override
    public void extractRenderState(JarBlockEntity entity, State state, float partialTicks, Vec3 camera,
        ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        super.extractRenderState(entity, state, partialTicks, camera, overlay);
        ((MountedState) state).mount = entity.getBlockState().getValue(StickyJarBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction mount = ((MountedState) state).mount;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (mount == Direction.DOWN) {
            pose.rotateDegrees(Axis.XP, 180);
        } else if (!mount.getAxis().isVertical()) {
            pose.rotateDegrees(Axis.YP, switch (mount) {
                case EAST -> -90;
                case SOUTH -> 180;
                case WEST -> 90;
                default -> 0;
            });
            pose.rotateDegrees(Axis.XP, -90);
        }
        pose.translate(-0.5, -0.5, -0.5);
        super.submit(state, pose, collector, camera);
        pose.popPose();
    }
}
