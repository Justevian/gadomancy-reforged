package makeo.gadomancy.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.platform.GolemActions;

@Mixin(GolemActions.class)
public interface GolemActionsInvoker {
    @Invoker("prepare")
    static ServerPlayer gadomancy$prepare(GolemBase golem, ItemStack stack) {
        throw new AssertionError();
    }

    @Invoker("finish")
    static ItemStack gadomancy$finish(GolemBase golem, ServerPlayer actor) {
        throw new AssertionError();
    }
}
