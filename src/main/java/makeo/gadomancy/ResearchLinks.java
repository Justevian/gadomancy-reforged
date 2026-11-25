package makeo.gadomancy;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.aspect.AspectList;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;

@EventBusSubscriber(modid = Gadomancy.MOD_ID)
final class ResearchLinks {
    private record Link(String original, int x, int y) {
        String key() { return link(original); }
    }

    private static final List<Link> LINKS = List.of(
        new Link("GOLEMTALLOW", 0, -5), new Link("BELLOWS", -2, -5),
        new Link("COREHARVEST", 2, -6), new Link("COREGUARD", 4, -5),
        new Link("VOIDMETAL", -8, -6), new Link("WANDPED", -9, -8),
        new Link("OCULUS", -11, -2), new Link("ESSENTIARESERVOIR", -5, 9),
        new Link("INFUSIONENCHANTMENT", -1, 2), new Link("JARLABEL", -3, -7)
    );

    static final String FAMILIAR_ELEMENT = "GADOMANCY.FAMILIAR_ELEMENT_KNOWN";

    static String link(String original) { return "GADOMANCY.PSEUDO." + original; }

    static void revealScannedCoreResearch(ServerPlayer player) {
        var knowledge = thaumcraft.research.Knowledge.get(player);
        for (var research : ResearchCategories.allResearch()) {
            if (!research.key().startsWith("GADOMANCY.") || !research.isHidden()) continue;
            for (var trigger : research.itemTriggers()) {
                if (trigger.getNamespace().equals(Gadomancy.MOD_ID) && trigger.getPath().startsWith("aura_core_")
                    && knowledge.isScanned(thaumcraft.research.ScanKind.OBJECT, trigger.toString())) {
                    ResearchManager.createClue(player, research.key());
                }
            }
        }
    }

    static void register() {

        for (Link link : LINKS) {
            var original = ResearchCategories.getResearch(link.original());
            if (original == null) throw new IllegalStateException("Missing Thaumcraft research " + link.original());
            var item = original.iconItem() == null
                ? new ResearchItem(link.key(), Gadomancy.MOD_ID, new AspectList(), link.x(), link.y(), original.complexity(), original.iconTexture())
                : new ResearchItem(link.key(), Gadomancy.MOD_ID, new AspectList(), link.x(), link.y(), original.complexity(), original.iconItem());
            item.setStub().setParentsHidden(link.original()).setPages(original.pages().toArray(ResearchPage[]::new));
            if (original.isSecondary()) item.setSecondary();
            if (link.original().equals("OCULUS")) item.setRound().setSpecial();
            ResearchCategories.addResearch(item);
        }
    }

    @SubscribeEvent
    static void update(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        revealScannedCoreResearch(player);
        if (!ResearchManager.isComplete(player, FAMILIAR_ELEMENT)
            && List.of(FamiliarAugment.FIRE, FamiliarAugment.SHOCK, FamiliarAugment.POISON, FamiliarAugment.WEAKNESS)
                .stream().anyMatch(element -> ResearchManager.isComplete(player, element.research()))) {
            ResearchManager.complete(player, FAMILIAR_ELEMENT, false);
        }
        for (Link link : LINKS) {
            if (ResearchManager.isComplete(player, link.original()) && !ResearchManager.isComplete(player, link.key())) {
                ResearchManager.complete(player, link.key(), false);
            }
        }
    }
}
