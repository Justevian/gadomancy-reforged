package makeo.gadomancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.infusion.InfusionKeys;
import thaumcraft.network.ShieldFxPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

@EventBusSubscriber(modid = Gadomancy.MOD_ID)
public final class RunicGolemShields {
    private static final String RESEARCH = "GADOMANCY.GOLEMRUNICSHIELD";
    private static final String SHIELD = "gadomancy_runic_shield";
    private static final String CHARGE = "gadomancy_runic_charge";
    private static final String LAST_UPDATE = "gadomancy_runic_updated";

    private RunicGolemShields() {}

    static void registerResearch() {
        TcStack.registerDataMatcher(SHIELD, (stack, requested) -> shielded(stack) == Boolean.parseBoolean(requested));
        TcIngredient girdle = TcIngredient.item("thaumcraft:runic_shielding_girdle");
        TcIngredient salis = TcIngredient.item("thaumcraft:salis_mundus");
        List<String> recipes = new ArrayList<>();
        List<String> items = new ArrayList<>();
        for (GolemType type : GolemType.values()) {
            items.add("thaumcraft:" + type.itemId());
        }
        items.add(SilverwoodGolems.ID);
        for (String golem : items) {
            String key = "gadomancy:runic_" + golem.substring(golem.indexOf(':') + 1);
            recipes.add(key);
            TcRecipes.register(new InfusionRecipe(key, RESEARCH, TcStack.withData(golem, 1, Map.of(SHIELD, "true")), 5,
                new AspectList().add(Aspect.MAGIC, 16).add(Aspect.ENERGY, 16).add(Aspect.ARMOR, 8).add(Aspect.AURA, 4),
                TcIngredient.of(TcStack.withData(golem, 1, Map.of(SHIELD, "false"))), List.of(girdle, salis, salis)));
        }
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.AURA, 16).add(Aspect.ARMOR, 6).add(Aspect.MAGIC, 4).add(Aspect.MOTION, 8),
            1, 1, 4, TcStack.of("thaumcraft:runic_shielding_girdle")
        ).setParents("RUNICARMOR", SilverwoodGolems.RESEARCH).setConcealed().setPages(
            ResearchPage.text("tc.research_page." + RESEARCH + ".1"),
            ResearchPage.infusion(recipes.toArray(String[]::new))
        ));
    }

    public static boolean shielded(ItemStack stack) {
        return "true".equals(InfusionKeys.TAGS.getOrDefault(stack, Map.of()).get(SHIELD));
    }

    public static void equip(GolemBase golem, ItemStack stack) {
        if (shielded(stack)) {
            var data = golem.getPersistentData();
            data.putBoolean(SHIELD, true);
            data.putFloat(CHARGE, 0);
            data.putLong(LAST_UPDATE, golem.level().getGameTime());
        }
    }

    public static void preserve(GolemBase golem, ItemStack stack) {
        if (golem.getPersistentData().getBooleanOr(SHIELD, false)) {
            var tags = new HashMap<>(InfusionKeys.TAGS.getOrDefault(stack, Map.of()));
            tags.put(SHIELD, "true");
            InfusionKeys.TAGS.set(stack, Map.copyOf(tags));
        }
    }

    @SubscribeEvent
    static void showShield(ItemTooltipEvent event) {
        if (shielded(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.gadomancy.golem_shield").withStyle(ChatFormatting.AQUA));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void absorb(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof GolemBase golem) || !(golem.level() instanceof ServerLevel level)
            || event.getAmount() <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        var data = golem.getPersistentData();
        if (!data.getBooleanOr(SHIELD, false)) {
            return;
        }
        long tick = level.getGameTime();
        long elapsed = Math.max(0, tick - data.getLongOr(LAST_UPDATE, tick));
        int capacity = (int) (SilverwoodGolems.isSilverwood(golem) ? golem.getMaxHealth() : golem.getMaxHealth() / 2.6F);
        float charge = Math.min(capacity, data.getFloatOr(CHARGE, 0) + elapsed * 0.02F);
        float absorbed = Math.min(event.getAmount(), (int) charge);
        if (absorbed <= 0) {
            return;
        }
        data.putLong(LAST_UPDATE, tick);
        data.putFloat(CHARGE, Math.max(0, charge - (int) Math.ceil(absorbed)));
        event.setAmount(event.getAmount() - absorbed);
        var source = event.getSource();
        int target = source.is(DamageTypeTags.IS_FALL) ? -2
            : source.is(DamageTypes.FALLING_BLOCK) ? -3 : source.getEntity() == null ? -1 : source.getEntity().getId();
        ModNetwork.sendToNear(level, null, golem.getX(), golem.getY(), golem.getZ(), 64, new ShieldFxPayload(golem.getId(), target));
    }
}
