package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Public adapters for packaging and plate supplies used by countertops. */
public final class RestaurantSupplyCompatApi {
    public enum SupplyType { PACKAGING, CLEAN_PLATE, DIRTY_PLATE }
    public enum Probe { NOT_SOURCE, SOURCE }
    public interface Handler { Probe probe(World world, BlockPos pos, SupplyType type); int count(World world, BlockPos pos, SupplyType type); int extract(World world, BlockPos pos, SupplyType type, int amount, boolean simulate); }
    private record Entry(Identifier id, int priority, Handler handler) {} private record Target(BlockPos pos, Entry entry, int distance) {} private static final List<Entry> HANDLERS = new ArrayList<>();
    static { register(Identifier.of("ordertocook", "shelves"), 0, new Handler() {
        public Probe probe(World world, BlockPos pos, SupplyType type) { BlockState state=world.getBlockState(pos); return (type==SupplyType.PACKAGING&&state.getBlock() instanceof ShelfBlock)||(type==SupplyType.CLEAN_PLATE&&state.getBlock() instanceof PlateShelfBlock)?Probe.SOURCE:Probe.NOT_SOURCE; }
        public int count(World world, BlockPos pos, SupplyType type) { BlockState state=world.getBlockState(pos); if(type==SupplyType.PACKAGING&&state.getBlock() instanceof ShelfBlock)return state.get(ShelfBlock.PAPERS); if(type==SupplyType.CLEAN_PLATE&&state.getBlock() instanceof PlateShelfBlock)return state.get(PlateShelfBlock.PLATES); return 0; }
        public int extract(World world, BlockPos pos, SupplyType type, int amount, boolean simulate) { BlockState state=world.getBlockState(pos); int moved=Math.min(Math.max(0,amount),count(world,pos,type)); if(moved<=0||simulate)return moved; if(type==SupplyType.PACKAGING)world.setBlockState(pos,state.with(ShelfBlock.PAPERS,state.get(ShelfBlock.PAPERS)-moved),Block.NOTIFY_ALL); else if(type==SupplyType.CLEAN_PLATE)world.setBlockState(pos,state.with(PlateShelfBlock.PLATES,state.get(PlateShelfBlock.PLATES)-moved),Block.NOTIFY_ALL); return moved; }
    }); }
    public static synchronized void register(Identifier id,int priority,Handler handler){Objects.requireNonNull(id,"id");Objects.requireNonNull(handler,"handler");if(HANDLERS.stream().anyMatch(e->e.id().equals(id)))throw new IllegalArgumentException("Duplicate restaurant supply handler: "+id);HANDLERS.add(new Entry(id,priority,handler));HANDLERS.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e->e.id().toString()));}
    public static int countNearby(World world,BlockPos origin,int radius,SupplyType type){int total=0;for(Target target:targets(world,origin,radius,type))try{total+=Math.max(0,target.entry().handler().count(world,target.pos(),type));}catch(Throwable error){log(target,error);}return total;}
    public static int extractNearby(World world,BlockPos origin,int radius,SupplyType type,int amount,boolean simulate){if(world==null||amount<=0||(!simulate&&world.isClient))return 0;int moved=0;for(Target target:targets(world,origin,radius,type)){if(moved>=amount)break;try{moved+=Math.min(amount-moved,Math.max(0,target.entry().handler().extract(world,target.pos(),type,amount-moved,simulate)));}catch(Throwable error){log(target,error);}}return moved;}
    private static List<Target> targets(World world,BlockPos origin,int radius,SupplyType type){List<Target> result=new ArrayList<>();if(world==null||origin==null||type==null||radius<0)return result;BlockPos.Mutable cursor=new BlockPos.Mutable();for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){cursor.set(origin.getX()+dx,origin.getY()+dy,origin.getZ()+dz);BlockPos pos=cursor.toImmutable();for(Entry entry:snapshot())try{if(entry.handler().probe(world,pos,type)==Probe.NOT_SOURCE)continue;result.add(new Target(pos,entry,dx*dx+dy*dy+dz*dz));break;}catch(Throwable error){OrderToCookMod.LOGGER.error("Restaurant supply handler {} failed at {}",entry.id(),pos,error);}}result.sort(Comparator.comparingInt(Target::distance));return result;}
    private static void log(Target target,Throwable error){OrderToCookMod.LOGGER.error("Restaurant supply handler {} failed at {}",target.entry().id(),target.pos(),error);}private static synchronized List<Entry> snapshot(){return List.copyOf(HANDLERS);}private RestaurantSupplyCompatApi(){}
}
