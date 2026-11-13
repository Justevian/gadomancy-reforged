package makeo.gadomancy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.BundleContents;

final class PackageContents {
    private static final int MAX_BYTES = 65536;

    private PackageContents() {
    }

    static boolean load(ServerLevel level, ItemStack packageStack, List<ItemStack> inputs) {
        if (inputs.isEmpty() || inputs.size() > 9 || !inputs.stream().allMatch(item -> fitsDepth(item, 1, 0))) {
            return false;
        }
        packageStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inputs));
        Tag encoded = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, level.registryAccess()), packageStack)
            .result().orElse(null);
        if (encoded == null) {
            return false;
        }
        try (DataOutputStream output = new DataOutputStream(new SizeLimit())) {
            NbtIo.writeAnyTag(encoded, output);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean fitsDepth(ItemStack stack, int packages, int depth) {
        int nested = packages + (stack.getItem() instanceof ArcanePackageItem ? 1 : 0);
        if (nested > 2 || depth > 16) {
            return false;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && !contents.nonEmptyItemCopyStream().allMatch(item -> fitsDepth(item, nested, depth + 1))) {
            return false;
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        return bundle == null || bundle.itemCopies().allMatch(item -> fitsDepth(item, nested, depth + 1));
    }

    private static final class SizeLimit extends OutputStream {
        private int remaining = MAX_BYTES;

        @Override
        public void write(int value) throws IOException {
            consume(1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            consume(length);
        }

        private void consume(int count) throws IOException {
            if (count > remaining) {
                throw new IOException("Arcane package exceeds its storage limit");
            }
            remaining -= count;
        }
    }
}
