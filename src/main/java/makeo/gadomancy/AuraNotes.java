package makeo.gadomancy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import thaumcraft.aspect.Aspect;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;

final class AuraNotes {
    static final String RESEARCH = "GADOMANCY.AURA_EFFECTS";
    private static final List<String> TAGS = List.of(
        "aer", "alienis", "aqua", "arbor", "auram", "bestia", "cognitio", "exanimis", "fabrico", "fames", "gelum", "herba", "humanus", "ignis", "instrumentum", "iter", "limus", "lucrum", "lux", "mortuus", "motus", "ordo", "pannus", "perditio", "perfodio", "potentia", "praecantatio", "sano", "sensus", "spiritus", "telum", "tempestas", "tenebrae", "terra", "tutamen", "venenum", "victus", "vinculum", "vitium", "vitreus", "volatus"
    );

    private static String key(String tag) { return "GADOMANCY.AURA." + tag; }

    static ResearchPage[] pages() {
        var pages = new ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + RESEARCH + ".1"));
        for (String tag : TAGS) {

            pages.add(ResearchPage.concealed(key(tag), "gadomancy.aura.notes." + tag));
        }
        return pages.toArray(ResearchPage[]::new);
    }

    static void register() {
        for (String tag : TAGS) ResearchCategories.addResearch(new ResearchItem(key(tag), Gadomancy.MOD_ID));
    }

    static void observe(ServerLevel level, Vec3 flame, Aspect aspect) {
        String tag = aspect.getTag();
        if (!TAGS.contains(tag)) return;
        for (var player : level.players()) {
            if (player.distanceToSqr(flame) <= 64 && ResearchManager.isComplete(player, RESEARCH)
                && !ResearchManager.isComplete(player, key(tag))
                && Knowledge.get(player).hasDiscoveredAspect(aspect)) {
                ResearchManager.complete(player, key(tag));
            }
        }
    }
}
