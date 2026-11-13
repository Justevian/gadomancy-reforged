package makeo.gadomancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;

public final class PylonStatusEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, Gadomancy.MOD_ID);
    public static final DeferredHolder<MobEffect, MobEffect> VIS_DISCOUNT =
        EFFECTS.register("vis_discount", () -> new PassiveEffect(Aspect.MAGIC.getColor()));
    static final DeferredHolder<MobEffect, MobEffect> GOLEM_BUFF = EFFECTS.register("golem_buff", () ->
        new PassiveEffect(Aspect.CRAFT.getColor())
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("golem_speed"), 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.MAX_HEALTH, id("golem_health"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("golem_damage"), 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

    private PylonStatusEffects() {}

    static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Gadomancy.MOD_ID, path);
    }

    private static final class PassiveEffect extends MobEffect {
        PassiveEffect(int color) {
            super(MobEffectCategory.BENEFICIAL, color);
        }
    }
}
