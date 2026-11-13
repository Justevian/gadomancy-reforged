package makeo.gadomancy;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.essentia.EssentiaHandler;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchNote;
import thaumcraft.research.ResearchNoteKeys;

final class KnowledgeBookBlockEntity extends TcBlockEntity {
    private ItemStack note = ItemStack.EMPTY;
    private int work;
    private int fuel;
    private int bookshelves;
    private long nextPickup;
    private int lastSignal;
    private int animationTime;
    private float previousRotation;
    private float rotation;
    private float targetRotation;

    KnowledgeBookBlockEntity(BlockPos pos, BlockState state) {
        super(KnowledgeBooks.TYPE.get(), pos, state);
    }

    boolean isWorking() {
        return !note.isEmpty();
    }

    int signal() {
        return isWorking() ? (fuel > 0 ? 15 : 1) : 0;
    }

    void animateBook(Level level) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        Player player = level.getNearestPlayer(center.x, center.y, center.z, 3.0, false);
        targetRotation = player == null ? Mth.wrapDegrees(targetRotation + 1.146F)
            : (float) Math.toDegrees(Mth.atan2(player.getZ() - center.z, player.getX() - center.x));
        previousRotation = rotation;
        rotation = Mth.wrapDegrees(rotation + Mth.wrapDegrees(targetRotation - rotation) * 0.4F);
        animationTime++;
    }

    float bookRotation(float partialTick) {
        return previousRotation + Mth.wrapDegrees(rotation - previousRotation) * partialTick;
    }

    float bookAnimationTime(float partialTick) {
        return animationTime + partialTick;
    }

    void tick(ServerLevel level) {
        long time = level.getGameTime();
        if (isWorking()) {
            if (time % 100 == 0) {
                countShelves(level);
            }
            if (fuel <= 40 && time % 32 == 0 && EssentiaHandler.drainEssentia(this, Aspect.MIND, null, 4, false)) {
                fuel += 150;
                setChanged();
            }
            if (fuel > 0) {
                fuel--;
                if (level.getRandom().nextInt(300 - Math.min(bookshelves, 200)) == 0 && --work <= 0) {
                    returnNote(level, true);
                }
                if (time % 20 == 0) {
                    Vec3 center = Vec3.atCenterOf(worldPosition);
                    level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 5, 0.3, 0.2, 0.3, 0.02);
                }
                setChanged();
            }
        } else if (time >= nextPickup && time % 10 == 0) {
            findNote(level);
        }
        if (signal() != lastSignal) {
            lastSignal = signal();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void findNote(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        ItemEntity found = level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(8), item ->
            !item.hasPickUpDelay() && isUnfinished(item.getItem())
        ).stream().min(Comparator.comparingDouble(item -> item.distanceToSqr(center))).orElse(null);
        if (found == null) {
            return;
        }
        ItemStack stack = found.getItem();
        ResearchNote data = ResearchNoteKeys.NOTE.get(stack);
        ResearchItem research = data == null ? null : ResearchCategories.getResearch(data.key());
        if (research == null) {
            return;
        }
        note = stack.copyWithCount(1);
        work = Math.max(10, research.tags().getAspects().stream().mapToInt(aspect ->
            Math.max(10, research.tags().getAmount(aspect) * 4)).sum());
        if (stack.getCount() == 1) {
            found.discard();
        } else {
            found.setItem(stack.copyWithCount(stack.getCount() - 1));
        }
        countShelves(level);
        sync();
    }

    private static boolean isUnfinished(ItemStack stack) {
        if (!stack.is(ModItems.RESEARCH_NOTES.get())) {
            return false;
        }
        ResearchNote data = ResearchNoteKeys.NOTE.get(stack);
        return data != null && !data.complete() && ResearchCategories.getResearch(data.key()) != null;
    }

    private void countShelves(ServerLevel level) {
        bookshelves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-4, -3, -4), worldPosition.offset(4, 3, 4))) {
            if (!level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
                continue;
            }
            if (level.getBlockState(pos).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
                bookshelves++;
            } else if (!worldPosition.equals(pos) && level.getBlockEntity(pos) instanceof KnowledgeBookBlockEntity) {
                bookshelves += 2;
            }
        }
    }

    void returnNote(ServerLevel level, boolean finish) {
        if (!isWorking()) {
            return;
        }
        ItemStack output = note.copy();
        ResearchNote data = ResearchNoteKeys.NOTE.get(output);
        if (finish && data != null) {
            ResearchNoteKeys.NOTE.set(output, new ResearchNote(data.key(), data.color(), true, data.copies(), data.hexes()));
        }
        Vec3 pos = Vec3.atCenterOf(worldPosition).add(0, 0.2, 0);
        ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, output);
        drop.setDefaultPickUpDelay();
        if (level.addFreshEntity(drop)) {
            note = ItemStack.EMPTY;
            work = 0;
            nextPickup = level.getGameTime() + 80;
            sync();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            returnNote(server, false);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        note = input.read("note", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        work = input.getIntOr("work", 0);
        fuel = input.getIntOr("fuel", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("note", ItemStack.OPTIONAL_CODEC, note);
        output.putInt("work", work);
        output.putInt("fuel", fuel);
    }
}
