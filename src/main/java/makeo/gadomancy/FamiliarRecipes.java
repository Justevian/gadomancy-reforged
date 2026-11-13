package makeo.gadomancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class FamiliarRecipes {
    static final String ITEM_ID = "gadomancy:ethereal_familiar";
    private static final String RESEARCH = "GADOMANCY.ETHEREAL_FAMILIAR";
    private static final String ASPECT = "gadomancy_familiar_aspect";
    private static final String REQUIREMENT = "gadomancy_familiar_requirement";
    private static final String PREVIEW = "gadomancy_familiar_preview";
    private static final Map<String, Tier> UPGRADES = new HashMap<>();

    private record Tier(FamiliarAugment augment, int rank, int instability, Function<Aspect, AspectList> cost,
        boolean repeat, String... ingredients) {
        String key(Aspect aspect) {
            return "gadomancy:familiar_" + augment.id() + "_" + rank + "_" + aspect.getTag();
        }
    }

    private static final List<Tier> TIERS = List.of(
        new Tier(FamiliarAugment.SHOCK, 1, 3, aspect -> new AspectList().add(Aspect.MOTION, 14).add(aspect, 16).add(Aspect.AIR, 20),
            true, "essence", "thaumcraft:alumentum", "essence", "thaumcraft:amber"),
        new Tier(FamiliarAugment.SHOCK, 2, 5, aspect -> new AspectList().add(Aspect.MOTION, 22).add(aspect, 28).add(Aspect.AURA, 20).add(Aspect.WEAPON, 18).add(Aspect.AIR, 38),
            true, "essence", "thaumcraft:alumentum", "minecraft:glowstone_dust", "essence", "thaumcraft:alumentum", "minecraft:ghast_tear"),
        new Tier(FamiliarAugment.SHOCK, 3, 8, aspect -> new AspectList().add(Aspect.MOTION, 40).add(Aspect.AURA, 28).add(aspect, 38).add(Aspect.WEAPON, 20).add(Aspect.AIR, 36),
            true, "essence", "thaumcraft:alumentum", "thaumcraft:air_shard", "thaumcraft:alumentum", "thaumcraft:quicksilver", "thaumcraft:alumentum", "minecraft:nether_star", "thaumcraft:alumentum"),
        new Tier(FamiliarAugment.FIRE, 1, 4, aspect -> new AspectList().add(Aspect.FIRE, 12).add(aspect, 14).add(Aspect.WEAPON, 8),
            true, "essence", "minecraft:blaze_powder", "essence", "minecraft:blaze_powder"),
        new Tier(FamiliarAugment.FIRE, 2, 6, aspect -> new AspectList().add(Aspect.FIRE, 38).add(aspect, 24).add(Aspect.ENERGY, 32).add(Aspect.WEAPON, 18),
            true, "essence", "minecraft:redstone", "minecraft:coal_block", "essence", "minecraft:magma_cream", "minecraft:blaze_powder"),
        new Tier(FamiliarAugment.FIRE, 3, 9, aspect -> new AspectList().add(Aspect.FIRE, 48).add(aspect, 32).add(Aspect.ENERGY, 42).add(Aspect.WEAPON, 32).add(Aspect.AURA, 30),
            true, "essence", "minecraft:blaze_powder", "thaumcraft:fire_shard", "minecraft:blaze_powder", "minecraft:nether_star", "minecraft:blaze_powder", "thaumcraft:nitor", "minecraft:blaze_powder"),
        new Tier(FamiliarAugment.POISON, 1, 5, aspect -> new AspectList().add(Aspect.WATER, 16).add(aspect, 14).add(Aspect.POISON, 8).add(Aspect.ELDRITCH, 4),
            true, "essence", "minecraft:spider_eye", "essence", "minecraft:rotten_flesh"),
        new Tier(FamiliarAugment.POISON, 2, 7, aspect -> new AspectList().add(aspect, 26).add(Aspect.POISON, 28).add(Aspect.ELDRITCH, 12).add(Aspect.WATER, 22).add(Aspect.AURA, 8),
            true, "essence", "minecraft:fermented_spider_eye", "minecraft:ender_pearl", "essence", "minecraft:sugar_cane", "minecraft:pumpkin_seeds"),
        new Tier(FamiliarAugment.POISON, 3, 9, aspect -> new AspectList().add(aspect, 36).add(Aspect.POISON, 38).add(Aspect.ELDRITCH, 22).add(Aspect.WATER, 48).add(Aspect.AURA, 36),
            true, "essence", "minecraft:nether_wart", "minecraft:rotten_flesh", "minecraft:nether_wart", "minecraft:spider_eye", "minecraft:nether_wart", "minecraft:nether_star", "minecraft:nether_wart"),
        new Tier(FamiliarAugment.WEAKNESS, 1, 6, aspect -> new AspectList().add(aspect, 12).add(Aspect.DEATH, 18).add(Aspect.AURA, 6).add(Aspect.ELDRITCH, 18).add(Aspect.ENTROPY, 8),
            true, "essence", "minecraft:dead_bush", "essence", "minecraft:string"),
        new Tier(FamiliarAugment.WEAKNESS, 2, 8, aspect -> new AspectList().add(aspect, 30).add(Aspect.DEATH, 28).add(Aspect.AURA, 26).add(Aspect.ELDRITCH, 28).add(Aspect.ENTROPY, 48).add(Aspect.TRAP, 14),
            true, "essence", "minecraft:rotten_flesh", "minecraft:wither_skeleton_skull", "minecraft:rotten_flesh"),
        new Tier(FamiliarAugment.WEAKNESS, 3, 10, aspect -> new AspectList().add(aspect, 46).add(Aspect.DEATH, 42).add(Aspect.AURA, 38).add(Aspect.ELDRITCH, 20).add(Aspect.ENTROPY, 56).add(Aspect.TRAP, 32),
            true, "essence", "minecraft:ender_eye", "thaumcraft:entropy_shard", "thaumcraft:entropy_shard", "essence", "minecraft:ender_eye", "minecraft:nether_star", "thaumcraft:entropy_shard"),
        new Tier(FamiliarAugment.DAMAGE, 1, 6, aspect -> new AspectList().add(aspect, 14).add(Aspect.WEAPON, 10).add(Aspect.ENERGY, 12).add(Aspect.FIRE, 16),
            true, "essence", "thaumcraft:balanced_shard", "essence", "minecraft:gunpowder"),
        new Tier(FamiliarAugment.DAMAGE, 2, 10, aspect -> new AspectList().add(aspect, 22).add(Aspect.WEAPON, 28).add(Aspect.ENERGY, 34).add(Aspect.FIRE, 30).add(Aspect.AURA, 14),
            true, "essence", "thaumcraft:balanced_shard", "thaumcraft:primal_charm", "essence", "thaumcraft:void_seed", "thaumcraft:amber_block"),
        new Tier(FamiliarAugment.DAMAGE, 3, 10, aspect -> new AspectList().add(aspect, 34).add(Aspect.WEAPON, 38).add(Aspect.ENERGY, 46).add(Aspect.FIRE, 58).add(Aspect.AURA, 38),
            true, "essence", "minecraft:blaze_powder", "thaumcraft:void_sword", "minecraft:blaze_powder", "minecraft:nether_star", "minecraft:blaze_powder", "thaumcraft:alumentum", "minecraft:blaze_powder"),
        new Tier(FamiliarAugment.RANGE, 1, 6, aspect -> new AspectList().add(aspect, 24).add(Aspect.AURA, 30).add(Aspect.ELDRITCH, 22).add(Aspect.WEAPON, 12),
            true, "essence", "thaumcraft:order_shard", "essence", "thaumcraft:primal_charm"),
        new Tier(FamiliarAugment.RANGE, 2, 9, aspect -> new AspectList().add(aspect, 42).add(Aspect.AURA, 36).add(Aspect.ELDRITCH, 34).add(Aspect.WEAPON, 28).add(Aspect.AIR, 40),
            true, "essence", "thaumcraft:nitor", "thaumcraft:balanced_shard", "thaumcraft:quicksilver", "minecraft:nether_star", "thaumcraft:alumentum", "thaumcraft:balanced_shard", "thaumcraft:quicksilver"),
        new Tier(FamiliarAugment.SPEED, 1, 6, aspect -> new AspectList().add(aspect, 30).add(Aspect.AURA, 28).add(Aspect.MOTION, 38).add(Aspect.ELDRITCH, 20).add(Aspect.AIR, 36),
            false, "thaumcraft:elemental_sword", "thaumcraft:balanced_shard", "essence", "thaumcraft:balanced_shard", "essence", "thaumcraft:balanced_shard", "essence", "thaumcraft:balanced_shard"),
        new Tier(FamiliarAugment.SPEED, 2, 9, aspect -> new AspectList().add(aspect, 46).add(Aspect.AURA, 38).add(Aspect.MOTION, 52).add(Aspect.ELDRITCH, 38).add(Aspect.AIR, 48).add(Aspect.WEAPON, 28),
            true, "essence", "thaumcraft:primal_charm", "minecraft:nether_star", "thaumcraft:salis_mundus", "essence", "thaumcraft:primal_charm", "thaumcraft:primordial_pearl", "thaumcraft:salis_mundus")
    );

    private FamiliarRecipes() {}

    static void register() {
        TcStack.registerDataHandler(ASPECT, (stack, tag) -> {
            Aspect aspect = Aspect.getAspect(tag);
            if (aspect != null) {
                EtherealFamiliarItem.setAspect(stack, aspect);
            }
        });
        TcStack.registerDataMatcher(ASPECT, (stack, tag) -> EtherealFamiliarItem.aspect(stack).getTag().equals(tag));
        TcStack.registerDataMatcher(REQUIREMENT, (stack, spec) -> {
            FamiliarAugment augment = augment(spec);
            return EtherealFamiliarItem.canUpgrade(stack, augment, rank(spec) - 1);
        });
        TcStack.registerDataHandler(PREVIEW, (stack, spec) -> {
            FamiliarAugment augment = augment(spec);
            if (!augment.element) {
                EtherealFamiliarItem.setRank(stack, FamiliarAugment.SHOCK, 1);
            }
            EtherealFamiliarItem.setRank(stack, augment, rank(spec));
        });

        List<String> baseKeys = new ArrayList<>();
        for (Aspect aspect : Aspect.values()) {
            String base = "gadomancy:ethereal_familiar_" + aspect.getTag();
            TcIngredient essence = essence(aspect);
            TcIngredient salis = TcIngredient.item("thaumcraft:salis_mundus");
            TcIngredient nitor = TcIngredient.item("thaumcraft:nitor");
            TcRecipes.register(new InfusionRecipe(base, RESEARCH, stack(aspect, Map.of()), 4,
                new AspectList().add(aspect, 10).add(Aspect.AURA, 12).add(Aspect.MAGIC, 18),
                TcIngredient.item("thaumcraft:runic_shielding_amulet"),
                List.of(essence, salis, nitor, essence, salis, nitor)));
            baseKeys.add(base);
            for (Tier tier : TIERS) {
                registerTier(aspect, tier);
            }
        }
        ResearchCategories.addResearch(new ResearchItem(RESEARCH, Gadomancy.MOD_ID,
            new AspectList().add(Aspect.MAGIC, 8).add(Aspect.FIRE, 6).add(Aspect.AURA, 4), 5, 1, 2,
            stack(Aspect.MAGIC, Map.of())
        ).setConcealed().setSpecial().setParents("RUNICARMOR").setPages(
            page(RESEARCH), ResearchPage.infusion(baseKeys.toArray(String[]::new)),
            ResearchPage.text("tc.research_page.GADOMANCY.ETHEREAL_FAMILIAR.3"),
            ResearchPage.text("tc.research_page.GADOMANCY.ETHEREAL_FAMILIAR.4")
        ));
        for (FamiliarAugment augment : FamiliarAugment.values()) {
            registerResearch(augment);
        }
    }

    private static void registerTier(Aspect aspect, Tier tier) {
        String key = tier.key(aspect);
        String spec = tier.augment.name() + ":" + tier.rank;
        String previous = tier.augment.name() + ":" + (tier.rank - 1);
        TcStack central = stack(aspect, Map.of(REQUIREMENT, spec, PREVIEW, previous));
        TcStack result = stack(aspect, Map.of(PREVIEW, spec));
        List<TcIngredient> ingredients = new ArrayList<>();
        for (String id : tier.ingredients) {
            ingredients.add("essence".equals(id) ? essence(aspect) : TcIngredient.item(id));
        }
        if (tier.repeat) {
            ingredients.addAll(List.copyOf(ingredients));
        }
        TcRecipes.register(new InfusionRecipe(key, tier.augment.research(), result, tier.instability,
            tier.cost.apply(aspect), TcIngredient.of(central), ingredients));
        UPGRADES.put(key, tier);
    }

    private static void registerResearch(FamiliarAugment augment) {
        AspectList discovery = switch (augment) {
            case SHOCK -> new AspectList().add(Aspect.WEATHER, 4).add(Aspect.WEAPON, 4).add(Aspect.AIR, 8);
            case FIRE -> new AspectList().add(Aspect.FIRE, 8).add(Aspect.WEAPON, 4).add(Aspect.MAGIC, 4);
            case POISON -> new AspectList().add(Aspect.POISON, 8).add(Aspect.WEAPON, 4).add(Aspect.ENTROPY, 4);
            case WEAKNESS -> new AspectList().add(Aspect.TRAP, 8).add(Aspect.WEAPON, 8).add(Aspect.TAINT, 6);
            case DAMAGE -> new AspectList().add(Aspect.WEAPON, 8).add(Aspect.ENERGY, 8).add(Aspect.MAGIC, 4);
            case RANGE -> new AspectList().add(Aspect.MAGIC, 8).add(Aspect.ELDRITCH, 8).add(Aspect.ORDER, 4);
            case SPEED -> new AspectList().add(Aspect.MOTION, 10).add(Aspect.MAGIC, 4).add(Aspect.AURA, 4);
        };
        List<ResearchPage> pages = new ArrayList<>();
        pages.add(page(augment.research()));
        for (Tier tier : TIERS) {
            if (tier.augment == augment) {
                pages.add(ResearchPage.infusion(tier.key(Aspect.MAGIC)));
            }
        }
        ResearchItem research = new ResearchItem(augment.research(), Gadomancy.MOD_ID, discovery, augment.column,
            augment.row, 2, Identifier.parse("gadomancy:textures/misc/familiar_" + augment.icon.toLowerCase(Locale.ROOT) + ".png"))
            .setConcealed().setPages(pages.toArray(ResearchPage[]::new));
        if (augment == FamiliarAugment.SPEED) {
            research.setParents(FamiliarAugment.DAMAGE.research(), FamiliarAugment.RANGE.research(), "PRIMPEARL");
        } else if (augment == FamiliarAugment.DAMAGE) {
            research.setParents(RESEARCH, "VOIDMETAL");
        } else {
            research.setParents(RESEARCH);
        }
        ResearchCategories.addResearch(research);
    }

    public static @Nullable ItemStack upgraded(String key, ItemStack central) {
        Tier tier = UPGRADES.get(key);
        if (tier == null) {
            return null;
        }
        if (!central.is(EtherealFamiliarItem.ITEM.get()) || !tier.key(EtherealFamiliarItem.aspect(central)).equals(key)
            || !EtherealFamiliarItem.canUpgrade(central, tier.augment, tier.rank - 1)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = central.copyWithCount(1);
        EtherealFamiliarItem.setRank(result, tier.augment, tier.rank);
        return result;
    }

    private static TcIngredient essence(Aspect aspect) {
        return TcIngredient.of(TcStack.withAspects("thaumcraft:wisp_essence", 1, new AspectList().add(aspect, 2)));
    }

    private static TcStack stack(Aspect aspect, Map<String, String> fields) {
        Map<String, String> data = new HashMap<>(fields);
        data.put(ASPECT, aspect.getTag());
        return TcStack.withData(ITEM_ID, 1, data);
    }

    private static FamiliarAugment augment(String spec) {
        return FamiliarAugment.valueOf(spec.substring(0, spec.indexOf(':')));
    }

    private static int rank(String spec) {
        return Integer.parseInt(spec.substring(spec.indexOf(':') + 1));
    }

    private static ResearchPage page(String research) {
        return ResearchPage.text("tc.research_page." + research + ".1");
    }
}
