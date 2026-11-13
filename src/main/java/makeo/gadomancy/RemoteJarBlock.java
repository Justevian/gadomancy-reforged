package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.EssentiaKeys;
import thaumcraft.essentia.JarBlock;
import thaumcraft.essentia.JarBlockEntity;

class RemoteJarBlock extends JarBlock {
    RemoteJarBlock(Properties properties) {
        this(false, properties);
    }

    RemoteJarBlock(boolean voidJar, Properties properties) {
        super(voidJar, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RemoteJarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != RemoteJars.TYPE.get()) {
            return null;
        }
        return (world, pos, jarState, entity) -> JarBlockEntity.serverTick(world, pos, jarState, (RemoteJarBlockEntity) entity);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RemoteJarBlockEntity jar) {
            jar.setNetwork(stack.get(RemoteJars.NETWORK.get()));
        }
    }

    @Override
    public ItemStack createDrop(JarBlockEntity jar) {
        ItemStack drop = new ItemStack(this);
        Aspect aspect = jar.getAspect();
        if (aspect != null && jar.getAmount() > 0) {
            AspectKeys.ASPECTS.set(drop, new AspectList().add(aspect, jar.getAmount()));
        }
        if (jar.getAspectFilter() != null) {
            EssentiaKeys.FILTER.set(drop, jar.getAspectFilter());
        }
        if (jar instanceof RemoteJarBlockEntity remote && remote.network() != null) {
            drop.set(RemoteJars.NETWORK.get(), remote.network());
        }
        return drop;
    }
}
