package makeo.gadomancy;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.client.render.EssentiaJarRenderer;
import thaumcraft.client.render.ArcanePedestalRenderer;

@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
final class RemoteJarRenderer {
    private RemoteJarRenderer() {
    }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RemoteJars.TYPE.get(), EssentiaJarRenderer::new);
        event.registerBlockEntityRenderer(StickyJars.TYPE.get(), StickyJarRenderer::new);
        event.registerBlockEntityRenderer(StickyJars.REMOTE_TYPE.get(), StickyJarRenderer::new);
        event.registerBlockEntityRenderer(AuraStructures.PEDESTAL_TYPE.get(), ArcanePedestalRenderer::new);
        event.registerBlockEntityRenderer(AuraStructures.CONDENSER_TYPE.get(), CondenserRenderer::new);
        event.registerBlockEntityRenderer(KnowledgeBooks.TYPE.get(), KnowledgeBookRenderer::new);
        event.registerBlockEntityRenderer(NodeManipulators.TYPE.get(), NodeManipulatorRenderer::new);
        event.registerBlockEntityRenderer(NodeManipulators.PORTAL_TYPE.get(), VoidwalkerPortalRenderer::new);
        AuraStructureBlock.visualTick = AuraStructureVisuals::tick;
    }
    @SubscribeEvent
    static void creativeNodeRendering(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        var game = net.minecraft.client.Minecraft.getInstance();
        var renderer = new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(game.getBlockEntityRenderDispatcher(), game.getEntityModels()) {
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
                thaumcraft.client.render.NodeItemRenderer.render(poses, buffers);
            }
        };
        for (var entry : net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getValue() instanceof CreativeNodeItem) {
                event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                    @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
                }, entry.getValue());
            }
        }
    }
}
