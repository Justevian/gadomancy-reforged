package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import thaumcraft.client.platform.ImmediateGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;


import thaumcraft.client.platform.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;

final class CondenserRenderer extends PortBlockRenderer<CondenserBlockEntity, CondenserRenderer.State> {
    private static final ResourceLocation VORTEX = ResourceLocation.parse("thaumcraft:textures/misc/vortex.png");
    private static final int LIGHT = 0xF000F0;

    CondenserRenderer(BlockEntityRendererProvider.Context context) {}

    static final class State extends PortBlockRenderer.State {
        boolean active;
        float ticks;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CondenserBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
        Object overlay) {
        super.extractRenderState(entity, state, partialTicks, cameraPosition, overlay);
        state.active = entity.isActive(AuraStructureBlock.Kind.CONDENSER);
        if (state.active && entity.getLevel() != null) {
            long time = entity.getLevel().getGameTime();
            state.ticks = (time % 7200) + partialTicks;

        }
    }

    @Override
    public void submit(State state, PoseStack pose, ImmediateGeometry collector, CameraRenderState camera) {
        if (!state.active) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(camera.orientation);
        pose.mulPose(Axis.ZP.rotationDegrees(state.ticks * 4F));
        quad(pose, collector, VORTEX, 0.34F, 0xFFFFFFFF);
        pose.popPose();
    }

    private static void quad(PoseStack pose, ImmediateGeometry collector, ResourceLocation texture, float size, int color) {
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
