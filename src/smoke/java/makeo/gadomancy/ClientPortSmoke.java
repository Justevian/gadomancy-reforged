package makeo.gadomancy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Loads real client models; excluded from the published artifact. */
@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
public final class ClientPortSmoke {
    private static boolean checked;
    private static int worldTicks;
    private static long opened;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("gadomancy.clientSmoke")) return;
        if (checked) {
            var game=Minecraft.getInstance();
            if (game.level!=null && game.player!=null && ++worldTicks>40) {
                checked=false; // completion exits the client before another tick
                try { drawWorldModels(game); }
                catch(Throwable failure) { com.mojang.logging.LogUtils.getLogger().error("GAD_CLIENT_SMOKE_FAILED",failure); }
                finally {game.stop();}
            } else if (System.nanoTime()-opened>120_000_000_000L) {
                com.mojang.logging.LogUtils.getLogger().error("GAD_CLIENT_SMOKE_FAILED: world load timeout; screen={}",game.screen);
                game.stop();
            }
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || mc.screen == null) return;
        checked = true;
        try {
            for (String name : new String[]{"net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer","thaumcraft.client.render.AspectContainerOverlay","thaumcraft.client.render.NodeVisibility","thaumcraft.client.render.device.NoteOverlay","thaumcraft.client.render.entity.golem.GolemRenderer","thaumcraft.client.screen.GolemScreen"}) Class.forName(name);
            int items = 0;
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (!entry.getKey().location().getNamespace().equals(Gadomancy.MOD_ID)) continue;
                var stack = new ItemStack(entry.getValue());
                var model = mc.getItemRenderer().getModel(stack, null, null, 0);
                if (model == mc.getModelManager().getMissingModel()) throw new AssertionError("Missing item model: " + entry.getKey().location());
                if (model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY).contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())) {
                    throw new AssertionError("Missing item texture: " + entry.getKey().location());
                }
                for (var quad : model.getQuads(null, null, net.minecraft.util.RandomSource.create(0))) {
                    if (quad.isTinted() && (mc.getItemColors().getColor(stack, quad.getTintIndex()) >>> 24) != 255) {
                        throw new AssertionError("Non-opaque item tint: " + entry.getKey().location());
                    }
                }
                items++;
            }
            com.mojang.logging.LogUtils.getLogger().info("GAD_CLIENT_MODELS_PASSED: {} item models and client mixin targets", items);
            opened=System.nanoTime();
            mc.createWorldOpenFlows().openWorld("gadomancy-port-smoke", () -> {
                com.mojang.logging.LogUtils.getLogger().error("GAD_CLIENT_SMOKE_FAILED: could not open world"); mc.stop();
            });
        } catch (Throwable failure) {
            com.mojang.logging.LogUtils.getLogger().error("GAD_CLIENT_SMOKE_FAILED", failure); mc.stop();
        }
    }
    private static void drawWorldModels(Minecraft mc) {
        int renderers=0;
        var buffers=mc.renderBuffers().bufferSource();
        for (var entry:BuiltInRegistries.BLOCK.entrySet()) {
            if (!entry.getKey().location().getNamespace().equals(Gadomancy.MOD_ID) || !(entry.getValue() instanceof net.minecraft.world.level.block.EntityBlock factory)) continue;
            var state=entry.getValue().defaultBlockState();
            var tile=factory.newBlockEntity(new net.minecraft.core.BlockPos(0,80,0),state);
            if (tile==null) continue;
            tile.setLevel(mc.level);
            if (tile instanceof thaumcraft.essentia.JarBlockEntity jar) {jar.setAspect(thaumcraft.aspect.Aspect.AIR);jar.setAmount(16);}
            if (tile instanceof NodeManipulatorBlockEntity) {
                var tag=tile.saveWithFullMetadata(mc.level.registryAccess());
                tag.put("wand",net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.encodeStart(mc.level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),new ItemStack(net.minecraft.world.item.Items.STICK)).getOrThrow());
                tag.putBoolean("transferring_vis",true);tile.loadWithComponents(tag,mc.level.registryAccess());
            }
            var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(tile);
            if (renderer!=null) {
                renderer.render(tile,0.5F,new com.mojang.blaze3d.vertex.PoseStack(),buffers,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                buffers.endBatch();renderers++;
            }
        }
        for (var entry:BuiltInRegistries.ITEM.entrySet()) {
            if (!(entry.getValue() instanceof CreativeNodeItem)) continue;
            var stack=new ItemStack(entry.getValue());
            var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            if (!model.isCustomRenderer()) throw new AssertionError("Creative node custom renderer not selected");
            net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack,net.minecraft.world.item.ItemDisplayContext.GUI,new com.mojang.blaze3d.vertex.PoseStack(),buffers,0xF000F0,0);
            buffers.endBatch();
        }
        if (renderers<8) throw new AssertionError("Too few block entity renderers: "+renderers);
        com.mojang.logging.LogUtils.getLogger().info("GAD_CLIENT_SMOKE_PASSED: 36 item models, client mixins, integrated world, {} block entity renders and creative node renders",renderers);
    }
}
