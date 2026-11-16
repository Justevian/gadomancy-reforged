package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.WorldRendering;
import thaumcraft.client.render.model.ObjModel;

final class NodeManipulatorRenderer implements BlockEntityRenderer<NodeManipulatorBlockEntity, NodeManipulatorRenderer.State> {
    private static final Identifier MODEL = Identifier.parse("thaumcraft:models/obj/pillar.obj");
    private static final Identifier TEXTURE = Identifier.parse("gadomancy:textures/block/manipulator_pillar.png");
    private static final int[] ROTATIONS = {0, 270, 90, 180};
    private final ItemModelResolver itemModels;

    NodeManipulatorRenderer(BlockEntityRendererProvider.Context context) {
        itemModels = context.itemModelResolver();
    }

    static final class State extends BlockEntityRenderState {
        final ItemStackRenderState wand = new ItemStackRenderState();
        final boolean[] pillars = new boolean[4];
        float age;
        float bob;
        boolean transferringVis;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public AABB getRenderBoundingBox(NodeManipulatorBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 3, 0);
    }

    @Override
    public void extractRenderState(NodeManipulatorBlockEntity entity, State state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, overlay);
        var level = entity.getLevel();
        state.wand.clear();
        var viewer = Minecraft.getInstance().getCameraEntity();
        state.age = (viewer == null ? 0 : viewer.tickCount) + partialTicks;
        state.bob = Mth.sin(state.age % 32767.0F / 16.0F) * 0.05F;
        state.transferringVis = entity.isTransferringVis();
        for (int i = 0; i < state.pillars.length; i++) {
            var corner = entity.getBlockPos().offset(NodeManipulatorBlockEntity.CORNERS.get(i));
            state.pillars[i] = level != null && entity.isAssembled()
                && level.getBlockState(corner).is(NodeManipulators.PILLAR_BASE.get());
        }
        if (level != null && !entity.installedWand().isEmpty()) {
            itemModels.updateForTopItem(state.wand, entity.installedWand(), ItemDisplayContext.GROUND, level, null,
                (int) entity.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var model = ObjModel.get(MODEL);
        for (int i = 0; i < state.pillars.length; i++) {
            if (!state.pillars[i]) continue;
            var corner = NodeManipulatorBlockEntity.CORNERS.get(i);
            pose.pushPose();
            pose.translate(corner.getX() + 0.5, 0, corner.getZ() + 0.5);
            pose.rotateDegrees(Axis.XP, -90);
            pose.rotateDegrees(Axis.ZP, ROTATIONS[i]);
            collector.submitCustomGeometry(pose, TcRenderTypes.entityCutout(TEXTURE),
                (entry, vertices) -> model.renderPart("Box001", entry, vertices, 0xFFFFFFFF, state.lightCoords));
            pose.popPose();
        }
        if (!state.wand.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.15 + state.bob, 0.5);
            pose.rotateDegrees(Axis.YP, state.age % 360.0F);
            state.wand.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            if (state.transferringVis) {
                var pos = state.blockPos;
                var primals = Aspect.getPrimalAspects();
                int color = primals.get((int) (state.age / 5) % primals.size()).getColor();
                WorldRendering.submitFloatyLine(collector, pose, pos,
                    pos.getX() + 0.5, pos.getY() + 1.65 + state.bob, pos.getZ() + 0.5,
                    pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, color, 1.0F, System.nanoTime());
            }
        }
    }
}
