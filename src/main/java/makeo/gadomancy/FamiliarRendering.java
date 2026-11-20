package makeo.gadomancy;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.client.platform.Billboards;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.platform.Baubles;

@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
final class FamiliarRendering {
    private static final ResourceLocation WISP = ResourceLocation.parse("thaumcraft:textures/misc/wisp.png");
    private static final ResourceLocation CORE = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");

    @SubscribeEvent
    static void registerTint(RegisterColorHandlersEvent.Item event) {
        for (int i = 0; i < AuraCores.CHARGED.size(); i++) {
            int color = 0xFF000000 | AuraCores.PRIMALS.get(i).getColor();
            event.register((stack, layer) -> layer == 0 ? color : -1, AuraCores.CHARGED.get(i).get());
        }
        event.register((stack, layer) -> layer == 0 ? 0xFF000000 : -1, Gadomancy.darknessItem());
        event.register((stack, layer) -> layer == 0 ? (0xFF000000 | EtherealFamiliarItem.visualColor(stack)) : -1, EtherealFamiliarItem.ITEM.get());
    }

    @SubscribeEvent
    static void submit(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var game = Minecraft.getInstance();
        if (game.level == null || game.player == null) return;
        var camera = event.getCamera();
        var pose = event.getPoseStack();
        var collector = Minecraft.getInstance().renderBuffers().bufferSource();
        float partial = game.getTimer().getGameTimeDeltaPartialTick(false);
        long tick = game.level.getGameTime();
        float time = tick % 24000 + partial;
        int frame = (int) time & 15;
        float u = (frame % 4) / 4F;
        float v = (frame / 4) / 4F;
        for (var player : game.level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isInvisibleTo(game.player)
                || player.distanceToSqr(camera.getPosition()) > 4096) continue;
            ItemStack familiar = Baubles.find(player, stack -> stack.is(EtherealFamiliarItem.ITEM.get()));
            if (familiar.isEmpty()) continue;
            var offset = EtherealFamiliarItem.orbit(player, tick, partial).subtract(camera.getPosition());
            int color = 0xFF000000 | EtherealFamiliarItem.visualColor(familiar);
            pose.pushPose();
            pose.translate(offset.x, offset.y, offset.z);
            Billboards.submit(pose, collector, camera, TcRenderTypes.additive(WISP),
                u + 0.0001F, u + 0.2499F, v + 0.0001F, v + 0.2499F, 0.72F, color, 240);
            Billboards.submit(pose, collector, camera, TcRenderTypes.additive(CORE),
                frame / 16F, (frame + 1) / 16F, 5 / 16F, 6 / 16F, 0.28F, 0xFFFFFFFF, 240);
            pose.popPose();
        }
        game.renderBuffers().bufferSource().endBatch(TcRenderTypes.additive(WISP));
        game.renderBuffers().bufferSource().endBatch(TcRenderTypes.additive(CORE));
    }

}
