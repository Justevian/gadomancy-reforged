package makeo.gadomancy;

import java.util.Locale;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;

enum FamiliarAugment {
    SHOCK(true, 3, Aspect.AIR, 3, Aspect.ENTROPY, 2, "Lightning", 4, -1),
    FIRE(true, 3, Aspect.FIRE, 4, Aspect.ORDER, 2, "Fire", 6, -1),
    POISON(true, 3, Aspect.WATER, 3, Aspect.ENTROPY, 4, "Poison", 3, 2),
    WEAKNESS(true, 3, Aspect.ENTROPY, 3, Aspect.EARTH, 3, "Enervation", 2, 0),
    DAMAGE(false, 3, Aspect.FIRE, 3, null, 0, "Damage", 8, 2),
    RANGE(false, 2, Aspect.AIR, 2, Aspect.ORDER, 1, "Range", 6, 4),
    SPEED(false, 2, Aspect.ORDER, 2, Aspect.FIRE, 1, "Speed", 8, 5);

    final boolean element;
    final int maximum;
    final String icon;
    final int column;
    final int row;
    private final Aspect first;
    private final int firstAmount;
    private final @Nullable Aspect second;
    private final int secondAmount;

    FamiliarAugment(boolean element, int maximum, Aspect first, int firstAmount, @Nullable Aspect second, int secondAmount,
        String icon, int column, int row) {
        this.element = element;
        this.maximum = maximum;
        this.first = first;
        this.firstAmount = firstAmount;
        this.second = second;
        this.secondAmount = secondAmount;
        this.icon = icon;
        this.column = column;
        this.row = row;
    }

    String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    String research() {
        return "GADOMANCY.FAMILIAR_" + name();
    }

    void addVisCost(AspectList cost, int rank) {
        if (rank > 0) {
            cost.add(first, rank * firstAmount);
            if (second != null) {
                cost.add(second, rank * secondAmount);
            }
        }
    }
}
