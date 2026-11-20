package makeo.gadomancy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.platform.CameraRenderState;
import thaumcraft.client.platform.ImmediateGeometry;
/** Keeps render data local to a draw with the 1.21.1 immediate renderer. */
abstract class PortBlockRenderer<T extends BlockEntity,S extends PortBlockRenderer.State> implements BlockEntityRenderer<T> {
    static class State { int lightCoords; BlockPos blockPos; }
    public abstract S createRenderState();
    public void extractRenderState(T entity,S state,float partialTick,Vec3 camera,Object overlay) {}
    public abstract void submit(S state,PoseStack poses,ImmediateGeometry geometry,CameraRenderState camera);
    @Override public final void render(T entity,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        S state=createRenderState(); state.lightCoords=light; state.blockPos=entity.getBlockPos();
        extractRenderState(entity,state,partialTick,Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),null);
        submit(state,poses,new ImmediateGeometry(buffers),new CameraRenderState());
    }
}
