package makeo.gadomancy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchGate;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class ArmorDisguises {
    private static final String RESEARCH = "GADOMANCY.ARMORDISGUISE";
    private static final String DISGUISE = "gadomancy:armor_disguise";
    private static final String INVISIBLE = "gadomancy:invisible_armor";
    private static final String PREVIEW = "gadomancy_disguise_preview";
    private static final TcStack SAMPLE = TcStack.of("minecraft:diamond_chestplate");
    private static final List<TcIngredient> MATERIALS = List.of(
        TcIngredient.item("minecraft:slime_ball"), TcIngredient.item("thaumcraft:quicksilver"),
        TcIngredient.item("minecraft:slime_ball"), TcIngredient.item("thaumcraft:quicksilver"));

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Gadomancy.MOD_ID);
    private static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> APPEARANCE =
        COMPONENTS.register("armor_disguise", () -> DataComponentType.<ItemStack>builder()
            .persistent(ItemStack.OPTIONAL_CODEC).networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC).build());

    private ArmorDisguises() {}

    static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    static void registerResearch() {
        TcStack mask = TcStack.of("minecraft:leather_chestplate");
        TcStack potion = TcStack.withData("minecraft:potion", 1, Map.of(PREVIEW, "potion"));
        TcStack disguised = TcStack.withData("minecraft:diamond_chestplate", 1, Map.of(PREVIEW, "leather"));
        TcStack invisible = TcStack.withData("minecraft:diamond_chestplate", 1, Map.of(PREVIEW, "invisible"));
        TcStack.registerDataHandler(PREVIEW, (stack, type) -> {
            switch (type) {
                case "potion" -> stack.set(DataComponents.POTION_CONTENTS,
                    new PotionContents(Potions.INVISIBILITY));
                case "leather" -> stack.set(APPEARANCE.get(), mask.create());
                case "invisible" -> stack.set(APPEARANCE.get(), ItemStack.EMPTY);
            }
        });
        TcRecipes.register(new InfusionRecipe(DISGUISE, RESEARCH, disguised, -20, cost(),
            TcIngredient.of(SAMPLE), ingredients(TcIngredient.of(mask))));
        TcRecipes.register(new InfusionRecipe(INVISIBLE, RESEARCH, invisible, -20, cost(),
            TcIngredient.of(SAMPLE), ingredients(TcIngredient.of(potion))));
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.SLIME, 8).add(Aspect.MAGIC, 4).add(Aspect.ARMOR, 4),
            -2, 0, 1, SAMPLE
        ).setParents("INFUSIONENCHANTMENT").setConcealed().setPages(
            ResearchPage.text("tc.research_page.GADOMANCY.ARMORDISGUISE.1"),
            ResearchPage.infusion(DISGUISE), ResearchPage.infusion(INVISIBLE),
            ResearchPage.text("tc.research_page.GADOMANCY.ARMORDISGUISE.2")
        ));
    }

    private static List<TcIngredient> ingredients(TcIngredient appearance) {
        List<TcIngredient> result = new ArrayList<>(MATERIALS.size() + 1);
        result.add(appearance);
        result.addAll(MATERIALS);
        return result;
    }

    private static AspectList cost() {
        return new AspectList().add(Aspect.SLIME, 12).add(Aspect.ARMOR, 10).add(Aspect.MAGIC, 8);
    }

    public static boolean isDisguiseRecipe(String key) {
        return DISGUISE.equals(key) || INVISIBLE.equals(key);
    }

    public static InfusionCrafting.@Nullable Plan findPlan(List<ItemStack> components, ItemStack central, Player player) {
        if (components.size() != MATERIALS.size() + 1 || armorSlot(central) == null
            || central.has(APPEARANCE.get()) || !ResearchGate.isComplete(player, RESEARCH)) {
            return null;
        }
        List<ItemStack> remaining = new ArrayList<>(components);
        for (TcIngredient material : MATERIALS) {
            int index = 0;
            while (index < remaining.size() && !material.test(remaining.get(index))) {
                index++;
            }
            if (index == remaining.size()) {
                return null;
            }
            remaining.remove(index);
        }
        ItemStack appearance = remaining.getFirst();
        String key = isInvisibilityPotion(appearance) ? INVISIBLE : DISGUISE;
        if (!isAppearance(key, central, appearance)) {
            return null;
        }
        InfusionRecipe recipe = (InfusionRecipe) TcRecipes.get(key);
        return new InfusionCrafting.Plan(InfusionCrafting.TYPE_RECIPE, key, recipe.components(),
            recipe.instability(), recipe.aspects().copy(), 0);
    }

    public static boolean isAppearanceIngredient(String key, TcIngredient ingredient) {
        return isDisguiseRecipe(key) && TcRecipes.get(key) instanceof InfusionRecipe recipe
            && ingredient == recipe.components().getFirst();
    }

    public static boolean isAppearance(String key, ItemStack central, ItemStack candidate) {
        if (candidate.isEmpty() || candidate.has(APPEARANCE.get()) || armorSlot(central) == null) {
            return false;
        }
        return INVISIBLE.equals(key) ? isInvisibilityPotion(candidate)
            : DISGUISE.equals(key) && armorSlot(candidate) == armorSlot(central);
    }

    private static boolean isInvisibilityPotion(ItemStack candidate) {
        var contents = candidate.get(DataComponents.POTION_CONTENTS);
        if (!candidate.is(Items.POTION) || contents == null) {
            return false;
        }
        for (var effect : contents.getAllEffects()) {
            if (effect.getEffect().equals(MobEffects.INVISIBILITY)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable EquipmentSlot armorSlot(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || stack.getMaxStackSize() != 1) {
            return null;
        }
        return switch (equippable.slot()) {
            case HEAD, CHEST, LEGS, FEET -> equippable.assetId().isPresent() ? equippable.slot() : null;
            default -> null;
        };
    }

    public static ItemStack apply(String key, ItemStack armor, ItemStack appearance) {
        if (!isAppearance(key, armor, appearance)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = armor.copy();
        result.set(APPEARANCE.get(), INVISIBLE.equals(key) ? ItemStack.EMPTY : appearance.copyWithCount(1));
        return result;
    }

    public static ItemStack appearance(ItemStack armor) {
        return armor.getOrDefault(APPEARANCE.get(), armor);
    }

    public static void purify(ServerLevel level, BlockPos pos, ItemEntity entity) {
        ItemStack armor = entity.getItem();
        ItemStack stored = armor.get(APPEARANCE.get());
        if (stored == null) {
            return;
        }
        ItemStack clean = armor.copy();
        clean.remove(APPEARANCE.get());
        entity.setItem(clean);
        if (!stored.isEmpty()) {
            ItemEntity recovered = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stored.copy());
            recovered.setDefaultPickUpDelay();
            level.addFreshEntity(recovered);
        }
        level.levelEvent(1501, pos, 0);
    }
}
