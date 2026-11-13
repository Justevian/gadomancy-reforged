package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;

final class CondenserRenderer implements BlockEntityRenderer<CondenserBlockEntity, CondenserRenderer.State> {
    private static final Identifier VORTEX = Identifier.parse("thaumcraft:textures/misc/vortex.png");
    private static final int LIGHT = 0xF000F0;

    CondenserRenderer(BlockEntityRendererProvider.Context context) {}

    static final class State extends BlockEntityRenderState {
        boolean active;
        float ticks;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CondenserBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, overlay);
        state.active = entity.isActive(AuraStructureBlock.Kind.CONDENSER);
        if (state.active && entity.getLevel() != null) {
            long time = entity.getLevel().getGameTime();
            state.ticks = (time % 7200) + partialTicks;

        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.active) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotate(camera.orientation);
        pose.rotateDegrees(Axis.ZP, state.ticks * 4F);
        quad(pose, collector, VORTEX, 0.34F, 0xFFFFFFFF);
        pose.popPose();
    }

    private static void quad(PoseStack pose, SubmitNodeCollector collector, Identifier texture, float size, int color) {
        collector.submitCustomGeometry(pose, TcRenderTypes.translucent(texture), (entry, buffer) -> {
            VertexQuadSink sink = new VertexQuadSink(entry, buffer);
            sink.vertex(-size, -size, 0, 0, 1, color, LIGHT);
            sink.vertex(-size, size, 0, 0, 0, color, LIGHT);
            sink.vertex(size, size, 0, 1, 0, color, LIGHT);
            sink.vertex(size, -size, 0, 1, 1, color, LIGHT);
        });
    }

    @Override
    public AABB getRenderBoundingBox(CondenserBlockEntity entity) {
        return new AABB(entity.getBlockPos());
    }
}
