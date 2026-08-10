package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.block.entity.TakeoutBoxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Player-independent, simulatable countertop operations for automation integrations. */
public final class CountertopAutomationApi {
    public enum Action { PACK, PLATE }
    public enum Status { SUCCESS, INVALID_TARGET, CLIENT_SIDE, NO_ORDER, EXPIRED, DELIVERY_REQUIRES_PACKAGING, MISSING_FOOD, MISSING_PACKAGING, MISSING_PLATE, OUTPUT_BLOCKED }
    public record Result(Status status) { public boolean success() { return status == Status.SUCCESS; } }
    public static Result execute(World world, BlockPos pos, Action action, @Nullable PlayerEntity player, boolean simulate) { if (world == null || pos == null || action == null || !(world.getBlockEntity(pos) instanceof TakeoutBoxBlockEntity countertop)) return new Result(Status.INVALID_TARGET); if (!simulate && world.isClient) return new Result(Status.CLIENT_SIDE); Status status = countertop.getAutomationStatus(action); if (simulate) return new Result(status); if (action == Action.PACK) countertop.tryPackOrder(player); else countertop.tryPlateOrder(player); return new Result(status); }
    private CountertopAutomationApi() {}
}
