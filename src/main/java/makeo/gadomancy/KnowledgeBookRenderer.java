package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class KnowledgeBookRenderer implements BlockEntityRenderer<KnowledgeBookBlockEntity, KnowledgeBookRenderer.State> {
    private final BookModel model;
    private final SpriteGetter sprites;

    KnowledgeBookRenderer(BlockEntityRendererProvider.Context context) {
        model = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        sprites = context.sprites();
    }

    static final class State extends BlockEntityRenderState {
        float rotation;
        float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KnowledgeBookBlockEntity book, State state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(book, state, partialTick, cameraPosition, overlay);
        state.rotation = book.bookRotation(partialTick);
        state.time = book.bookAnimationTime(partialTick);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.2 + 0.03 * Mth.sin(state.time * 0.1F), 0.5);
        pose.rotateDegrees(Axis.YP, -state.rotation);
        pose.rotateDegrees(Axis.ZP, 80);
        BookModel.State pages = BookModel.State.forAnimation(45, 0, 1, 0.9F);
        collector.submitModel(model, pages, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
            EnchantTableRenderer.BOOK_TEXTURE, sprites, 0);
        pose.popPose();
    }
}
