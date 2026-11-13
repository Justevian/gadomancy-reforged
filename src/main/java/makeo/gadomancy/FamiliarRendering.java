package makeo.gadomancy;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.platform.Billboards;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.platform.Baubles;

@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
final class FamiliarRendering {
    private static final Identifier WISP = Identifier.parse("thaumcraft:textures/misc/wisp.png");
    private static final Identifier CORE = Identifier.parse("thaumcraft:textures/misc/particles.png");

    @SubscribeEvent
    static void registerTint(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.parse("gadomancy:familiar"), Tint.CODEC);
    }

    @SubscribeEvent
    static void submit(SubmitCustomGeometryEvent event) {
        var game = Minecraft.getInstance();
        if (game.level == null || game.player == null) return;
        var camera = event.getLevelRenderState().cameraRenderState;
        var pose = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        float partial = game.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long tick = game.level.getGameTime();
        float time = tick % 24000 + partial;
        int frame = (int) time & 15;
        float u = (frame % 4) / 4F;
        float v = (frame / 4) / 4F;
        for (var player : game.level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isInvisibleTo(game.player)
                || player.distanceToSqr(camera.pos) > 4096) continue;
            ItemStack familiar = Baubles.find(player, stack -> stack.is(EtherealFamiliarItem.ITEM.get()));
            if (familiar.isEmpty()) continue;
            var offset = EtherealFamiliarItem.orbit(player, tick, partial).subtract(camera.pos);
            int color = 0xFF000000 | EtherealFamiliarItem.visualColor(familiar);
            pose.pushPose();
            pose.translate(offset.x, offset.y, offset.z);
            Billboards.submit(pose, collector, camera, TcRenderTypes.additive(WISP),
                u + 0.0001F, u + 0.2499F, v + 0.0001F, v + 0.2499F, 0.72F, color, 240);
            Billboards.submit(pose, collector, camera, TcRenderTypes.additive(CORE),
                frame / 16F, (frame + 1) / 16F, 5 / 16F, 6 / 16F, 0.28F, 0xFFFFFFFF, 240);
            pose.popPose();
        }
    }

    record Tint() implements ItemTintSource {
        static final MapCodec<Tint> CODEC = MapCodec.unit(new Tint());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return 0xFF000000 | EtherealFamiliarItem.visualColor(stack);
        }

        @Override
        public MapCodec<Tint> type() {
            return CODEC;
        }
    }
}
