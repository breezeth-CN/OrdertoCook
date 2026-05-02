package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.util.DataCompat;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MotorcycleItem extends Item {
    public static final String MOTORCYCLE_COLOR_KEY = "MotorcycleColor";

    public MotorcycleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        MotorcycleEntity entity = MotorcycleEntity.create(
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.05,
                pos.getZ() + 0.5
        );
        entity.setMotorcycleColor(getMotorcycleColor(context.getStack()));
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            entity.setYaw(player.getYaw() + 180.0f);
        }
        if (!world.spawnEntity(entity)) {
            return ActionResult.FAIL;
        }

        ItemStack itemStack = context.getStack();
        if (itemStack.hasCustomName()) {
            entity.setCustomName(itemStack.getName());
        }

        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.NEUTRAL, 0.5f, 0.8f);

        PlayerEntity p = context.getPlayer();
        if (p != null && !p.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }

        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, net.minecraft.util.Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int color = getMotorcycleColor(stack);
        if (color == 0) {
            return;
        }
        tooltip.add(Text.translatable(
                "tooltip.ordertocook.motorcycle_color",
                Text.translatable(getColorTranslationKey(color))
        ).formatted(Formatting.GRAY));
    }

    public static ItemStack createColoredStack(int color) {
        ItemStack stack = new ItemStack(ModItems.MOTORCYCLE);
        setMotorcycleColor(stack, color);
        return stack;
    }

    /** 与旧版多物品 ID 掉落兼容：语义色 → 带 NBT 的单一物品。 */
    public static ItemStack stackForMotorcycleColor(int motorcycleColor) {
        return createColoredStack(motorcycleColor);
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

    /**
     * 旧实体/存档中「染料 ID」字段迁移到语义色（仅用于 NBT 读取，非物品 ID）。
     */
    public static int semanticFromLegacyDyeId(int dyeId) {
        if (dyeId == 14) {
            return 1;
        }
        if (dyeId == 11) {
            return 2;
        }
        if (dyeId == 4) {
            return 3;
        }
        return 0;
    }

}
