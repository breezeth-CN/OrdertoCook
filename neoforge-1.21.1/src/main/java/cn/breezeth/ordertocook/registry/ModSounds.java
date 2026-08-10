package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ModConstants.MOD_ID);

    public static final ResourceLocation PRESTIGE_GET_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "prestige_get");
    public static final ResourceLocation ORDER_REFRESH_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "order_refresh");
    public static final ResourceLocation HORN_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "horn");
    public static final ResourceLocation BAG_PLACE_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "bag_place");
    public static final ResourceLocation BAG_PICKUP_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "bag_pickup");
    public static final ResourceLocation PLATE_PLACE_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "plate_place");
    public static final ResourceLocation FOOD_PLATE_PLACE_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "food_plate_place");
    public static final ResourceLocation DISHWASHER_IN_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "dishwasher_in");
    public static final ResourceLocation DISHWASHER_OUT_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "dishwasher_out");
    public static final ResourceLocation DISHWASHER_CLEAR_OUT_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "dishwasher_clear_out");
    public static final ResourceLocation DISHWASHER_WASHING_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "dishwasher_washing");

    public static final DeferredHolder<SoundEvent, SoundEvent> PRESTIGE_GET = SOUNDS.register("prestige_get", () -> SoundEvent.createVariableRangeEvent(PRESTIGE_GET_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_REFRESH = SOUNDS.register("order_refresh", () -> SoundEvent.createVariableRangeEvent(ORDER_REFRESH_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN = SOUNDS.register("horn", () -> SoundEvent.createVariableRangeEvent(HORN_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> BAG_PLACE = SOUNDS.register("bag_place", () -> SoundEvent.createVariableRangeEvent(BAG_PLACE_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> BAG_PICKUP = SOUNDS.register("bag_pickup", () -> SoundEvent.createVariableRangeEvent(BAG_PICKUP_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> PLATE_PLACE = SOUNDS.register("plate_place", () -> SoundEvent.createVariableRangeEvent(PLATE_PLACE_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> FOOD_PLATE_PLACE = SOUNDS.register("food_plate_place", () -> SoundEvent.createVariableRangeEvent(FOOD_PLATE_PLACE_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> DISHWASHER_IN = SOUNDS.register("dishwasher_in", () -> SoundEvent.createVariableRangeEvent(DISHWASHER_IN_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> DISHWASHER_OUT = SOUNDS.register("dishwasher_out", () -> SoundEvent.createVariableRangeEvent(DISHWASHER_OUT_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> DISHWASHER_CLEAR_OUT = SOUNDS.register("dishwasher_clear_out", () -> SoundEvent.createVariableRangeEvent(DISHWASHER_CLEAR_OUT_ID));
    public static final DeferredHolder<SoundEvent, SoundEvent> DISHWASHER_WASHING = SOUNDS.register("dishwasher_washing", () -> SoundEvent.createVariableRangeEvent(DISHWASHER_WASHING_ID));

    public static void registerSounds(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
