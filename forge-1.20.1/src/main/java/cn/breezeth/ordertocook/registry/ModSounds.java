package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModConstants.MOD_ID);

    public static final ResourceLocation PRESTIGE_GET_ID = new ResourceLocation(ModConstants.MOD_ID, "prestige_get");
    public static final ResourceLocation ORDER_REFRESH_ID = new ResourceLocation(ModConstants.MOD_ID, "order_refresh");
    public static final ResourceLocation HORN_ID = new ResourceLocation(ModConstants.MOD_ID, "horn");
    public static final ResourceLocation BAG_PLACE_ID = new ResourceLocation(ModConstants.MOD_ID, "bag_place");
    public static final ResourceLocation BAG_PICKUP_ID = new ResourceLocation(ModConstants.MOD_ID, "bag_pickup");
    public static final ResourceLocation PLATE_PLACE_ID = new ResourceLocation(ModConstants.MOD_ID, "plate_place");
    public static final ResourceLocation FOOD_PLATE_PLACE_ID = new ResourceLocation(ModConstants.MOD_ID, "food_plate_place");

    public static final RegistryObject<SoundEvent> PRESTIGE_GET = SOUNDS.register("prestige_get", () -> SoundEvent.createVariableRangeEvent(PRESTIGE_GET_ID));
    public static final RegistryObject<SoundEvent> ORDER_REFRESH = SOUNDS.register("order_refresh", () -> SoundEvent.createVariableRangeEvent(ORDER_REFRESH_ID));
    public static final RegistryObject<SoundEvent> HORN = SOUNDS.register("horn", () -> SoundEvent.createVariableRangeEvent(HORN_ID));
    public static final RegistryObject<SoundEvent> BAG_PLACE = SOUNDS.register("bag_place", () -> SoundEvent.createVariableRangeEvent(BAG_PLACE_ID));
    public static final RegistryObject<SoundEvent> BAG_PICKUP = SOUNDS.register("bag_pickup", () -> SoundEvent.createVariableRangeEvent(BAG_PICKUP_ID));
    public static final RegistryObject<SoundEvent> PLATE_PLACE = SOUNDS.register("plate_place", () -> SoundEvent.createVariableRangeEvent(PLATE_PLACE_ID));
    public static final RegistryObject<SoundEvent> FOOD_PLATE_PLACE = SOUNDS.register("food_plate_place", () -> SoundEvent.createVariableRangeEvent(FOOD_PLATE_PLACE_ID));

    public static void registerSounds(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
