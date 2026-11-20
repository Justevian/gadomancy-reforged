package makeo.gadomancy;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Adapts the property-transforming registration helpers used by the 26.3 port. */
final class PortRegistration {
    static final class Blocks extends DeferredRegister.Blocks {
        Blocks(String namespace) { super(namespace); }

        <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
                                                        UnaryOperator<BlockBehaviour.Properties> properties) {
            return super.registerBlock(name, factory, properties.apply(BlockBehaviour.Properties.of()));
        }

        DeferredBlock<Block> registerSimpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
            return super.registerSimpleBlock(name, properties.apply(BlockBehaviour.Properties.of()));
        }
    }

    static final class Items extends DeferredRegister.Items {
        Items(String namespace) { super(namespace); }

        DeferredItem<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> properties) {
            return super.registerSimpleItem(name, properties.apply(new Item.Properties()));
        }
        <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory,
                                                    UnaryOperator<Item.Properties> properties) {
            return super.registerItem(name, factory, properties.apply(new Item.Properties()));
        }
    }
}
