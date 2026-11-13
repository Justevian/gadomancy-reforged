package makeo.gadomancy.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import thaumcraft.entity.monster.ChampionModifier;

@Mixin(ChampionModifier.class)
public interface ChampionWhitelistInvoker {
    @Invoker("whitelistLevel")
    static int gadomancy$whitelistLevel(Mob mob) {
        throw new AssertionError();
    }
}
