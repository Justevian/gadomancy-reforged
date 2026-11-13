package makeo.gadomancy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class DarknessElementItem extends Item {
    DarknessElementItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity carrier, EquipmentSlot slot) {
        if ((level.getGameTime() & 15) == 0) {
            applyBlindness(level, carrier.position());
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity item) {
        if (item.level() instanceof ServerLevel level) {
            // Let the temporary item spawned by /give expire normally.
            if (item.hasPickUpDelay() && item.getAge() == stack.getEntityLifespan(level) - 1) {
                return false;
            }
            item.setUnlimitedLifetime();
            if ((item.tickCount & 15) == 0) {
                Vec3 pos = item.position();
                applyBlindness(level, pos);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.2, pos.z, 8, 0.2, 0.2, 0.2, 0.01);
            }
        }
        return false;
    }

    private static void applyBlindness(ServerLevel level, Vec3 pos) {
        AABB area = AABB.ofSize(pos, 17, 17, 17);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, true));
        }
    }
}
