package makeo.gadomancy;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public final class PortNbt {
    public static int getIntOr(CompoundTag tag, String key, int fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback; }
    public static long getLongOr(CompoundTag tag, String key, long fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback; }
    public static float getFloatOr(CompoundTag tag, String key, float fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback; }
    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback; }
    public static String getStringOr(CompoundTag tag, String key, String fallback) { return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback; }
    public record Input(CompoundTag tag, HolderLookup.Provider registries) {
        public <T> Optional<T> read(String key, Codec<T> codec) {
            Tag value = tag.get(key);
            return value == null ? Optional.empty() : codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), value).result();
        }
        public int getIntOr(String key, int fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback; }
        public boolean getBooleanOr(String key, boolean fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback; }
    }
    public record Output(CompoundTag tag, HolderLookup.Provider registries) {
        public <T> void store(String key, Codec<T> codec, T value) {
            tag.put(key, codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value).getOrThrow());
        }
        public <T> void storeNullable(String key, Codec<T> codec, T value) { if (value != null) store(key, codec, value); }
        public void putInt(String key, int value) { tag.putInt(key, value); }
        public void putBoolean(String key, boolean value) { tag.putBoolean(key, value); }
    }
}
