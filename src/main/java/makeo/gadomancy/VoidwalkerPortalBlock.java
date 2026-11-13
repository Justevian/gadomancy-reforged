package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.eldritch.EldritchPortalTravel;

final class VoidwalkerPortalBlock extends Block {
    VoidwalkerPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effects, boolean intersects) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player
            && player.getPortalCooldown() == 0 && server.getBlockState(pos).is(this)
            && EldritchPortalTravel.tryTravel(server, pos, player)) {
            server.removeBlock(pos, false);
        }
    }
}
