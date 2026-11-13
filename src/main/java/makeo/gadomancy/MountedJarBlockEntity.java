package makeo.gadomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.Essentia;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.essentia.JarBlockEntity;

class MountedJarBlockEntity extends JarBlockEntity {
    private final boolean voidJar;

    MountedJarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean voidJar) {
        super(type, pos, state);
        this.voidJar = voidJar;
    }

    Direction port() {
        BlockState state = getBlockState();
        return state.hasProperty(StickyJarBlock.FACING) ? state.getValue(StickyJarBlock.FACING) : Direction.UP;
    }

    @Override
    public boolean isVoid() {
        return voidJar;
    }

    @Override
    protected boolean canFill() {
        return voidJar || super.canFill();
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (amount <= 0 || !doesContainerAccept(aspect)
            || getAmount() > 0 && getAspect() != aspect) {
            return amount;
        }
        if (!voidJar) {
            return super.addToContainer(aspect, amount);
        }
        int stored = Math.min(amount, getMaxAmount() - getAmount());
        if (stored > 0) {
            setAspect(aspect);
            setAmount(getAmount() + stored);
            sync();
        }
        return 0;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face == port();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return isConnectable(face);
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return isConnectable(face);
    }

    @Override
    public int getMinimumSuction() {
        return voidJar ? (getAspectFilter() != null ? 48 : 32) : super.getMinimumSuction();
    }

    @Override
    public int getSuctionAmount(@Nullable Direction face) {
        if (face != null && face != port()) {
            return 0;
        }
        return voidJar
            ? (getAspectFilter() != null && getAmount() < getMaxAmount() ? 48 : 32)
            : super.getSuctionAmount(face);
    }

    @Override
    protected void fillJar() {
        if (level == null) {
            return;
        }
        Direction port = port();
        Direction sourceSide = port.getOpposite();
        EssentiaTransport source = Essentia.getConnectable(level, worldPosition, port);
        int suction = getSuctionAmount(port);
        if (source == null || !source.canOutputTo(sourceSide) || source.getSuctionAmount(sourceSide) >= suction
            || source.getMinimumSuction() > suction) {
            return;
        }
        Aspect aspect = getSuctionType(port);
        if (aspect == null) {
            aspect = source.getEssentiaType(sourceSide);
        }
        if (aspect != null && doesContainerAccept(aspect) && (getAmount() == 0 || getAspect() == aspect)) {
            int amount = source.takeEssentia(aspect, 1, sourceSide);
            if (amount > 0) {
                addToContainer(aspect, amount);
            }
        }
    }
}
