package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.geom.EldritchSurfaceGeometry;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.TcRenderTypes;

final class VoidwalkerPortalRenderer implements BlockEntityRenderer<VoidwalkerPortalBlockEntity, VoidwalkerPortalRenderer.State> {
    private static final Identifier TEXTURE = Identifier.parse("thaumcraft:textures/misc/eldritch_portal.png");

    static final class State extends BlockEntityRenderState {
        float opening;
    }

    VoidwalkerPortalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(VoidwalkerPortalBlockEntity portal, State state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(portal, state, partialTick, cameraPosition, overlay);
        state.opening = portal.openTicks() + partialTick;
    }

    @Override
    public AABB getRenderBoundingBox(VoidwalkerPortalBlockEntity portal) {
        return new AABB(portal.getBlockPos()).inflate(1.0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float width = Mth.clamp(state.opening / 5.0F, 0.0F, 1.0F);
        float height = Mth.clamp(state.opening / 30.0F, 0.0F, 1.0F);
        int frame = (int) (System.nanoTime() / 50_000_000L) & 15;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotate(camera.orientation);
        collector.submitCustomGeometry(pose, TcRenderTypes.eldritchPortal(TEXTURE), (entry, vertices) ->
            EldritchSurfaceGeometry.portal(new EntityQuadSink(entry, vertices), width, height, frame, 0xFFFFFFFF, 0xF000F0));
        pose.popPose();
    }
}
