package makeo.gadomancy;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
@EventBusSubscriber(modid=Gadomancy.MOD_ID)
public final class PortSmoke {
 @SubscribeEvent public static void started(ServerStartedEvent event) {
  if (!Boolean.getBoolean("gadomancy.smoke")) return;
  var server=event.getServer();
  server.execute(() -> {
   try {
    var registries=server.registryAccess(); var level=server.overworld();
    int recipes=0,entities=0;
    for (var path:server.getResourceManager().listResources("recipe",id->id.getNamespace().equals("gadomancy") && id.getPath().endsWith(".json")).keySet()) {
     var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gadomancy",path.getPath().substring(7,path.getPath().length()-5));
     if (server.getRecipeManager().byKey(id).isEmpty()) throw new AssertionError("Missing recipe "+id); recipes++;
    }
    for (var entry:BuiltInRegistries.BLOCK.entrySet()) {
     if (!entry.getKey().location().getNamespace().equals("gadomancy") || !(entry.getValue() instanceof EntityBlock factory)) continue;
     var state=entry.getValue().defaultBlockState(); var tile=factory.newBlockEntity(BlockPos.ZERO,state); if (tile==null) continue;
     var tag=tile.saveWithFullMetadata(registries); var restored=BlockEntity.loadStatic(BlockPos.ZERO,state,tag,registries);
     if (restored==null || restored.getType()!=tile.getType()) throw new AssertionError("Block entity "+entry.getKey()); entities++;
    }
    var packager=new PackagerBlockEntity(BlockPos.ZERO,BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gadomancy:arcane_packager")).defaultBlockState());
    packager.setItem(0,new ItemStack(Items.DIAMOND,5));
    packager.setItem(PackagerBlockEntity.LEATHER,new ItemStack(Items.LEATHER,3));
    packager.addEssentia(thaumcraft.aspect.Aspect.CLOTH,4,Direction.UP);
    var tag=packager.saveWithFullMetadata(registries);packager.setRemoved();packager.clearRemoved();
    var restored=(PackagerBlockEntity)BlockEntity.loadStatic(BlockPos.ZERO,packager.getBlockState(),tag,registries);
    if (restored==null || restored.getItem(0).getCount()!=5 || restored.getItem(PackagerBlockEntity.LEATHER).getCount()!=3 || restored.saveWithFullMetadata(registries).getInt("fuel")!=4) throw new AssertionError("Packager persistence");
    var parcel=new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gadomancy:arcane_package")));
    if (!PackageContents.load(level,parcel,java.util.List.of(new ItemStack(Items.DIAMOND,4)))) throw new AssertionError("Package codec");
    var ops=registries.createSerializationContext(NbtOps.INSTANCE);
    var decoded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,parcel).getOrThrow()).getOrThrow();
    if (!ItemStack.isSameItemSameComponents(parcel,decoded)) throw new AssertionError("Parcel round trip");
    var testPos = new BlockPos(0, 80, 0);
    level.getChunkAt(testPos);
    var inputEntity = new net.minecraft.world.entity.item.ItemEntity(level, 0.5, 80.5, -1.5, new ItemStack(Items.DIAMOND, 8));
    inputEntity.setNoPickUpDelay();
    if (!level.addFreshEntity(inputEntity)) throw new AssertionError("Test item spawn");
    var handler = new ArcaneDropperTransfer(level, testPos, Direction.NORTH);
    int slot = -1;
    for (int i=0;i<handler.getSlots();i++) if (handler.getStackInSlot(i).is(Items.DIAMOND)) { slot=i;break; }
    if (slot<0 || handler.extractItem(slot,3,true).getCount()!=3 || inputEntity.getItem().getCount()!=8) throw new AssertionError("Simulated extraction mutated items");
    if (handler.extractItem(slot,3,false).getCount()!=3 || inputEntity.getItem().getCount()!=5) throw new AssertionError("Real extraction");
    handler.extractItem(slot,5,false);
    if (!inputEntity.isRemoved()) throw new AssertionError("Empty input not removed");
    var box = new net.minecraft.world.phys.AABB(testPos).inflate(4);
    int before = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).size();
    int output=handler.getSlots()-1;
    if (!handler.insertItem(output,new ItemStack(Items.EMERALD,4),true).isEmpty() || level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).size()!=before) throw new AssertionError("Simulated insertion spawned an item");
    if (!handler.insertItem(output,new ItemStack(Items.EMERALD,4),false).isEmpty()) throw new AssertionError("Real insertion rejected");
    int emeralds=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum();
    if (emeralds!=4) throw new AssertionError("Insertion duplicated or lost items: "+emeralds);
    level.setBlockAndUpdate(testPos,packager.getBlockState());
    var placed=(PackagerBlockEntity)level.getBlockEntity(testPos);
    placed.setItem(0,new ItemStack(Items.DIAMOND,5));
    placed.setRemoved();placed.clearRemoved();
    if (placed.getItem(0).getCount()!=5) throw new AssertionError("Chunk unload dropped inventory");
    level.setBlockAndUpdate(testPos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    int diamonds=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
    if (diamonds!=5) throw new AssertionError("Block removal duplicated or lost inventory: "+diamonds);
    var golem=thaumcraft.registry.ModEntities.GOLEM.get().create(level);
    if (golem==null) throw new AssertionError("Golem creation");
    // Loading every common mixin target catches linkage errors beyond startup registry classes.
    for (String name:new String[]{"net.minecraft.world.entity.monster.Creeper","thaumcraft.block.NodeJarBlock","thaumcraft.block.PurifyingFluidBlock","thaumcraft.blockentity.NodeJarBlockEntity","thaumcraft.blockentity.NodeJarTrigger","thaumcraft.crafting.InfusionCrafting","thaumcraft.entity.golem.GolemBase","thaumcraft.entity.golem.GolemSnapshot","thaumcraft.entity.golem.LogisticsGoals","thaumcraft.entity.golem.LogisticsGoals$EssentiaDepositGoal","thaumcraft.entity.golem.LogisticsGoals$EssentiaGatherGoal","thaumcraft.entity.golem.LogisticsGoals$EssentiaSourceGoal","thaumcraft.entity.golem.WorkGoals","thaumcraft.entity.monster.ChampionModifier","thaumcraft.infusion.InfusionMatrixBlockEntity","thaumcraft.item.golem.GolemBellItem","thaumcraft.item.golem.GolemItems","thaumcraft.item.golem.GolemPlacerItem","thaumcraft.node.NodeBlockEntity","thaumcraft.platform.GolemActions","thaumcraft.wand.WandManager"}) Class.forName(name);
    com.mojang.logging.LogUtils.getLogger().info("GAD_PORT_SMOKE_PASSED: {} recipes, {} block entities, packager inventory/fuel, package codec, golems, simulated/real transfers and block removal",recipes,entities);
   } catch(Throwable failure) {com.mojang.logging.LogUtils.getLogger().error("GAD_PORT_SMOKE_FAILED",failure);}
   finally {server.halt(false);}
  });
 }
}
