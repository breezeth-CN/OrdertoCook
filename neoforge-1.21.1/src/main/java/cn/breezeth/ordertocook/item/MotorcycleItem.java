package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.util.DataCompat;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MotorcycleItem extends Item {
    public static final String MOTORCYCLE_COLOR_KEY = "MotorcycleColor";

    public MotorcycleItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        MotorcycleEntity entity = ModEntities.MOTORCYCLE.get().create(world);
        if (entity == null) return InteractionResult.PASS;
        entity.setMotorcycleColor(getMotorcycleColor(context.getItemInHand()));
        entity.moveTo(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, context.getPlayer().getYRot() + 180.0f, 0.0f);
        world.addFreshEntity(entity);
        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, net.minecraft.world.InteractionHand hand) {
        return InteractionResultHolder.pass(user.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        int color = getMotorcycleColor(stack);
        if (color == 0) {
            return;
        }

        tooltip.add(Component.translatable("tooltip.ordertocook.motorcycle_color", Component.translatable(getColorTranslationKey(color)))
                .withStyle(ChatFormatting.GRAY));
    }

    public static ItemStack createColoredStack(int color) {
        ItemStack stack = new ItemStack(cn.breezeth.ordertocook.registry.ModItems.MOTORCYCLE.get());
        setMotorcycleColor(stack, color);
        return stack;
    }

    public static void setMotorcycleColor(ItemStack stack, int color) {
        color = normalizeColor(color);
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) {
            nbt = new CompoundTag();
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
        CompoundTag nbt = DataCompat.copy(stack);
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
