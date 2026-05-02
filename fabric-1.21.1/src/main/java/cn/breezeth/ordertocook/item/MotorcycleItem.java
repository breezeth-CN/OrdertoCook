package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.util.DataCompat;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class MotorcycleItem extends Item {
    public static final String MOTORCYCLE_COLOR_KEY = "MotorcycleColor";

    public MotorcycleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) return ActionResult.SUCCESS;
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        MotorcycleEntity entity = ModEntities.MOTORCYCLE.create(world);
        if (entity == null) return ActionResult.PASS;
        entity.setMotorcycleColor(getMotorcycleColor(context.getStack()));
        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, context.getPlayer().getYaw() + 180.0f, 0.0f);
        world.spawnEntity(entity);
        PlayerEntity player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            context.getStack().decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, net.minecraft.util.Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        int color = getMotorcycleColor(stack);
        if (color == 0) {
            return;
        }

        tooltip.add(Text.translatable("tooltip.ordertocook.motorcycle_color", Text.translatable(getColorTranslationKey(color)))
                .formatted(Formatting.GRAY));
    }

    public static ItemStack createColoredStack(int color) {
        ItemStack stack = new ItemStack(cn.breezeth.ordertocook.registry.ModItems.MOTORCYCLE);
        setMotorcycleColor(stack, color);
        return stack;
    }

    public static void setMotorcycleColor(ItemStack stack, int color) {
        color = normalizeColor(color);
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) {
            nbt = new NbtCompound();
        }

        if (color == 0) {
            nbt.remove(MOTORCYCLE_COLOR_KEY);
            DataCompat.set(stack, nbt);
            return;
        }

        nbt.putInt(MOTORCYCLE_COLOR_KEY, color);
        DataCompat.set(stack, nbt);
    }

    public static int getMotorcycleColor(ItemStack stack) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) {
            return 0;
        }
        return normalizeColor(nbt.getInt(MOTORCYCLE_COLOR_KEY));
    }

    public static String getColorTranslationKey(int color) {
        return switch (color) {
            case 1 -> "color.ordertocook.red";
            case 2 -> "color.ordertocook.blue";
            case 3 -> "color.ordertocook.yellow";
            default -> "color.ordertocook.original";
        };
    }

    private static int normalizeColor(int color) {
        return color >= 1 && color <= 3 ? color : 0;
    }
}
