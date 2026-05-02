package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FoodPlateItem extends Item {
    public FoodPlateItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient && world instanceof ServerWorld sw) {
            if (TakeoutBagItem.trySubmitDineInNearby(sw, user, stack, nbt, ModSounds.FOOD_PLATE_PLACE)) {
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, net.minecraft.entity.player.PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient || !(user.getWorld() instanceof ServerWorld sw)) {
            return ActionResult.PASS;
        }
        return TakeoutBagItem.trySubmitDineInFromEntityUse(sw, user, stack, (Entity) entity, ModSounds.FOOD_PLATE_PLACE);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient || !(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = context.getStack();
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            return ActionResult.PASS;
        }
        if (context.getPlayer() == null) {
            return ActionResult.PASS;
        }
        net.minecraft.util.math.BlockPos clickedPos = context.getBlockPos();
        ItemPlacementContext placementContext = new ItemPlacementContext(context);
        net.minecraft.util.math.BlockPos intendedPos = world.getBlockState(clickedPos).canReplace(placementContext)
                ? clickedPos
                : clickedPos.offset(context.getSide());
        return TakeoutBagItem.trySubmitDineInFromBlockUse(sw, context.getPlayer(), stack, intendedPos, ModSounds.FOOD_PLATE_PLACE);
    }

    public static ActionResult trySubmitDineInFromEntityUse(ServerWorld world, net.minecraft.entity.player.PlayerEntity player, ItemStack stack, LivingEntity entity) {
        return TakeoutBagItem.trySubmitDineInFromEntityUse(world, player, stack, (Entity) entity, ModSounds.FOOD_PLATE_PLACE);
    }
}
