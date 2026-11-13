package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeGenerator;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;
import thaumcraft.registry.ModBlocks;

final class CreativeNodeItem extends Item {
    private final NodeType type;

    CreativeNodeItem(Properties properties, NodeType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.isInWorldBounds(pos) || !level.isEmptyBlock(pos)
            || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            if (!server.mayInteract(player, pos)) {
                return InteractionResult.FAIL;
            }
            if (type == NodeType.NORMAL) {
                AspectList aspects = new AspectList().add(Aspect.AIR, 500).add(Aspect.EARTH, 500)
                    .add(Aspect.FIRE, 500).add(Aspect.WATER, 500).add(Aspect.ORDER, 500).add(Aspect.ENTROPY, 500);
                NodeGenerator.createNodeAt(server, pos, type, NodeModifier.BRIGHT, aspects);
            } else {
                NodeGenerator.createRandomNodeAt(server, pos, server.getRandom(), false, false, false);
                if (server.getBlockEntity(pos) instanceof NodeBlockEntity node) {
                    node.setNodeType(type);
                    node.nodeChange();
                }
            }
            return server.getBlockState(pos).is(ModBlocks.AURA_NODE.get())
                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }
}
