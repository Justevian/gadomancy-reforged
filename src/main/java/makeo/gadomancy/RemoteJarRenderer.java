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
        AuraStructureBlock.visualTick = AuraStructureVisuals::tick;
    }
}
