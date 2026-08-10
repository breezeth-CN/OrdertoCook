package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.block.entity.TakeoutBoxBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Player-independent, simulatable countertop operations for automation integrations. */
public final class CountertopAutomationApi {
    public enum Action { PACK, PLATE }
    public enum Status { SUCCESS, INVALID_TARGET, CLIENT_SIDE, NO_ORDER, EXPIRED, DELIVERY_REQUIRES_PACKAGING, MISSING_FOOD, MISSING_PACKAGING, MISSING_PLATE, OUTPUT_BLOCKED }
    public record Result(Status status) { public boolean success() { return status == Status.SUCCESS; } }
    public static Result execute(Level level, BlockPos pos, Action action, @Nullable Player player, boolean simulate) { if (level == null || pos == null || action == null || !(level.getBlockEntity(pos) instanceof TakeoutBoxBlockEntity countertop)) return new Result(Status.INVALID_TARGET); if (!simulate && level.isClientSide) return new Result(Status.CLIENT_SIDE); Status status = countertop.getAutomationStatus(action); if (simulate) return new Result(status); if (action == Action.PACK) countertop.tryPackOrder(player); else countertop.tryPlateOrder(player); return new Result(status); }
    private CountertopAutomationApi() {}
}
