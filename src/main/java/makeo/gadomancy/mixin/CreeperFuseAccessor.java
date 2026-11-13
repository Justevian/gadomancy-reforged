package makeo.gadomancy.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperFuseAccessor {
    @Accessor("swell")
    void gadomancy$setSwell(int value);
}
