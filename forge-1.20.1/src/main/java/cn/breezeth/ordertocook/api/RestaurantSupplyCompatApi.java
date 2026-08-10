package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Public adapters for packaging and plate supplies used by countertops. */
public final class RestaurantSupplyCompatApi {
    public enum SupplyType { PACKAGING, CLEAN_PLATE, DIRTY_PLATE } public enum Probe { NOT_SOURCE, SOURCE }
    public interface Handler { Probe probe(Level level,BlockPos pos,SupplyType type);int count(Level level,BlockPos pos,SupplyType type);int extract(Level level,BlockPos pos,SupplyType type,int amount,boolean simulate); }
    private record Entry(ResourceLocation id,int priority,Handler handler){} private record Target(BlockPos pos,Entry entry,int distance){} private static final List<Entry> HANDLERS=new ArrayList<>();
    static{register(new ResourceLocation("ordertocook","shelves"),0,new Handler(){public Probe probe(Level level,BlockPos pos,SupplyType type){BlockState state=level.getBlockState(pos);return(type==SupplyType.PACKAGING&&state.getBlock() instanceof ShelfBlock)||(type==SupplyType.CLEAN_PLATE&&state.getBlock() instanceof PlateShelfBlock)?Probe.SOURCE:Probe.NOT_SOURCE;}public int count(Level level,BlockPos pos,SupplyType type){BlockState state=level.getBlockState(pos);if(type==SupplyType.PACKAGING&&state.getBlock() instanceof ShelfBlock)return state.getValue(ShelfBlock.PAPERS);if(type==SupplyType.CLEAN_PLATE&&state.getBlock() instanceof PlateShelfBlock)return state.getValue(PlateShelfBlock.PLATES);return 0;}public int extract(Level level,BlockPos pos,SupplyType type,int amount,boolean simulate){BlockState state=level.getBlockState(pos);int moved=Math.min(Math.max(0,amount),count(level,pos,type));if(moved<=0||simulate)return moved;if(type==SupplyType.PACKAGING)level.setBlock(pos,state.setValue(ShelfBlock.PAPERS,state.getValue(ShelfBlock.PAPERS)-moved),Block.UPDATE_ALL);else if(type==SupplyType.CLEAN_PLATE)level.setBlock(pos,state.setValue(PlateShelfBlock.PLATES,state.getValue(PlateShelfBlock.PLATES)-moved),Block.UPDATE_ALL);return moved;}});}
    public static synchronized void register(ResourceLocation id,int priority,Handler handler){Objects.requireNonNull(id,"id");Objects.requireNonNull(handler,"handler");if(HANDLERS.stream().anyMatch(e->e.id().equals(id)))throw new IllegalArgumentException("Duplicate restaurant supply handler: "+id);HANDLERS.add(new Entry(id,priority,handler));HANDLERS.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e->e.id().toString()));}
    public static int countNearby(Level level,BlockPos origin,int radius,SupplyType type){int total=0;for(Target target:targets(level,origin,radius,type))try{total+=Math.max(0,target.entry().handler().count(level,target.pos(),type));}catch(Throwable error){log(target,error);}return total;}public static int extractNearby(Level level,BlockPos origin,int radius,SupplyType type,int amount,boolean simulate){if(level==null||amount<=0||(!simulate&&level.isClientSide))return 0;int moved=0;for(Target target:targets(level,origin,radius,type)){if(moved>=amount)break;try{moved+=Math.min(amount-moved,Math.max(0,target.entry().handler().extract(level,target.pos(),type,amount-moved,simulate)));}catch(Throwable error){log(target,error);}}return moved;}
    private static List<Target> targets(Level level,BlockPos origin,int radius,SupplyType type){List<Target> result=new ArrayList<>();if(level==null||origin==null||type==null||radius<0)return result;BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){cursor.set(origin.getX()+dx,origin.getY()+dy,origin.getZ()+dz);BlockPos pos=cursor.immutable();for(Entry entry:snapshot())try{if(entry.handler().probe(level,pos,type)==Probe.NOT_SOURCE)continue;result.add(new Target(pos,entry,dx*dx+dy*dy+dz*dz));break;}catch(Throwable error){OrderToCookMod.LOGGER.error("Restaurant supply handler {} failed at {}",entry.id(),pos,error);}}result.sort(Comparator.comparingInt(Target::distance));return result;}private static void log(Target target,Throwable error){OrderToCookMod.LOGGER.error("Restaurant supply handler {} failed at {}",target.entry().id(),target.pos(),error);}private static synchronized List<Entry> snapshot(){return List.copyOf(HANDLERS);}private RestaurantSupplyCompatApi(){}
}
