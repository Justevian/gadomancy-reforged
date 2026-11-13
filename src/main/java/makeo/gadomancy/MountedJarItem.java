package makeo.gadomancy;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import thaumcraft.essentia.FilledJarItem;

class MountedJarItem extends FilledJarItem {
    MountedJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        map.put(jar(), item);
    }
}
