package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BookModel;
import thaumcraft.client.platform.ImmediateGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;


import thaumcraft.client.platform.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class KnowledgeBookRenderer extends PortBlockRenderer<KnowledgeBookBlockEntity, KnowledgeBookRenderer.State> {
    private final BookModel model;


    KnowledgeBookRenderer(BlockEntityRendererProvider.Context context) {
        model = new BookModel(context.bakeLayer(ModelLayers.BOOK));

    }

    static final class State extends PortBlockRenderer.State {
        float rotation;
        float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KnowledgeBookBlockEntity book, State state, float partialTick, Vec3 cameraPosition,
        Object overlay) {
        super.extractRenderState(book, state, partialTick, cameraPosition, overlay);
        state.rotation = book.bookRotation(partialTick);
        state.time = book.bookAnimationTime(partialTick);
    }

    @Override
    public void submit(State state, PoseStack pose, ImmediateGeometry collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.2 + 0.03 * Mth.sin(state.time * 0.1F), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.rotation));
        pose.mulPose(Axis.ZP.rotationDegrees(80));
        model.setupAnim(45, 0, 1, 0.9F);
        model.renderToBuffer(pose, EnchantTableRenderer.BOOK_LOCATION.buffer(collector.buffers, net.minecraft.client.renderer.RenderType::entitySolid), state.lightCoords, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
