package makeo.gadomancy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;


import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.client.render.EssentiaJarRenderer;
import thaumcraft.essentia.JarBlockEntity;

final class StickyJarRenderer extends EssentiaJarRenderer {
    StickyJarRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override
    public void render(JarBlockEntity entity, float partialTick, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        Direction mount = entity.getBlockState().getValue(StickyJarBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (mount == Direction.DOWN) {
            pose.mulPose(Axis.XP.rotationDegrees(180));
        } else if (!mount.getAxis().isVertical()) {
            pose.mulPose(Axis.YP.rotationDegrees(switch (mount) {
                case EAST -> -90;
                case SOUTH -> 180;
                case WEST -> 90;
                default -> 0;
            }));
            pose.mulPose(Axis.XP.rotationDegrees(-90));
        }
        pose.translate(-0.5, -0.5, -0.5);
        super.render(entity, partialTick, pose, buffers, light, overlay);
        pose.popPose();
    }
}
