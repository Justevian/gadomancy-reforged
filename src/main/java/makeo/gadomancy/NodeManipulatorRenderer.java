package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import thaumcraft.client.platform.ImmediateGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;




import thaumcraft.client.platform.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.WorldRendering;
import thaumcraft.client.render.model.ObjModel;

final class NodeManipulatorRenderer extends PortBlockRenderer<NodeManipulatorBlockEntity, NodeManipulatorRenderer.State> {
    private static final ResourceLocation MODEL = ResourceLocation.parse("thaumcraft:models/obj/pillar.obj");
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("gadomancy:textures/block/manipulator_pillar.png");
    private static final int[] ROTATIONS = {0, 270, 90, 180};
    private final net.minecraft.client.renderer.entity.ItemRenderer itemModels;

    NodeManipulatorRenderer(BlockEntityRendererProvider.Context context) {
        itemModels = context.getItemRenderer();
    }

    static final class State extends PortBlockRenderer.State {
        net.minecraft.world.item.ItemStack wand = net.minecraft.world.item.ItemStack.EMPTY;
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
        Vec3 cameraPosition, Object overlay) {
        super.extractRenderState(entity, state, partialTicks, cameraPosition, overlay);
        var level = entity.getLevel();
        state.wand = net.minecraft.world.item.ItemStack.EMPTY;
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
            state.wand = entity.installedWand().copy();
        }
    }

    @Override
    public void submit(State state, PoseStack pose, ImmediateGeometry collector, CameraRenderState camera) {
        var model = ObjModel.get(MODEL);
        for (int i = 0; i < state.pillars.length; i++) {
            if (!state.pillars[i]) continue;
            var corner = NodeManipulatorBlockEntity.CORNERS.get(i);
            pose.pushPose();
            pose.translate(corner.getX() + 0.5, 0, corner.getZ() + 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(ROTATIONS[i]));
            collector.submitCustomGeometry(pose, TcRenderTypes.entityCutout(TEXTURE),
                (entry, vertices) -> model.renderPart("Box001", entry, vertices, 0xFFFFFFFF, state.lightCoords));
            pose.popPose();
        }
        if (!state.wand.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.15 + state.bob, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(state.age % 360.0F));
            itemModels.renderStatic(state.wand, ItemDisplayContext.GROUND, state.lightCoords, OverlayTexture.NO_OVERLAY, pose, collector.buffers, Minecraft.getInstance().level, 0);
            pose.popPose();
            if (state.transferringVis) {
                var pos = state.blockPos;
                var primals = Aspect.getPrimalAspects();
                int color = primals.get((int) (state.age / 5) % primals.size()).getColor();
                WorldRendering.submitFloatyLine(collector.buffers, pose, pos,
                    pos.getX() + 0.5, pos.getY() + 1.65 + state.bob, pos.getZ() + 0.5,
                    pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, color, 1.0F, System.nanoTime());
            }
        }
    }
}
