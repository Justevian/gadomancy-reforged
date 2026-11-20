package makeo.gadomancy;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.client.platform.Billboards;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.entity.golem.Marker;
import thaumcraft.item.golem.GolemBellItem;

@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
final class GolemBellTrail {
    private static final ResourceLocation SPARK = ResourceLocation.parse("thaumcraft:textures/misc/mark.png");

    @SubscribeEvent
    static void submit(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null || game.options.hideGui) return;
        var bell = game.player.getMainHandItem();
        if (!(bell.getItem() instanceof GolemBellItem)) bell = game.player.getOffhandItem();
        if (!(bell.getItem() instanceof GolemBellItem)) return;
        var selection = GolemKeys.BELL.get(bell);
        if (selection == null || !selection.home().dimension().equals(game.level.dimension())
            || !(java.util.stream.StreamSupport.stream(game.level.entitiesForRendering().spliterator(), false).filter(entity -> entity.getUUID().equals(selection.entity())).findFirst().orElse(null) instanceof GolemBase golem) || !golem.isAlive()) return;
        float partial = game.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 start = golem.getPosition(partial).add(0, golem.getBbHeight() * 0.65, 0);
        double time = game.level.getGameTime() % 2400 + partial;
        trail(event, start, selection.homeMarker(), 0xFFF5C2, time);
        for (Marker marker : selection.markers()) {
            if (marker.inLevel(game.level)) {
                int color = marker.color() < 0 ? 0xEEE4FF : DyeColor.byId(marker.color()).getTextColor();
                trail(event, start, marker, color, time);
            }
        }
        game.renderBuffers().bufferSource().endBatch(TcRenderTypes.additive(SPARK));
    }

    private static void trail(RenderLevelStageEvent event, Vec3 start, Marker marker, int color, double time) {
        var side = marker.side();
        Vec3 end = Vec3.atCenterOf(marker.pos()).add(side.getStepX() * 0.53, side.getStepY() * 0.53, side.getStepZ() * 0.53);
        double length = start.distanceTo(end);
        if (length < 0.1 || length > 48) return;
        var camera = event.getCamera();
        var pose = event.getPoseStack();
        int count = Math.max(2, (int) Math.ceil(length / 0.23));
        double phase = (time % 10) / 10;
        for (int i = 0; i < count; i++) {
            double step = (i + phase) / count;
            Vec3 point = start.lerp(end, step).add(0, Math.sin(Math.PI * step) * 0.3, 0).subtract(camera.getPosition());
            pose.pushPose();
            pose.translate(point.x, point.y, point.z);
            Billboards.submit(pose, Minecraft.getInstance().renderBuffers().bufferSource(), camera, TcRenderTypes.additive(SPARK),
                0, 1, 0, 1, 0.095F, 0xFF000000 | color, 240);
            pose.popPose();
        }
    }
}
