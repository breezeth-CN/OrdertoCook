package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier PRESTIGE_GET_ID = new Identifier(ModConstants.MOD_ID, "prestige_get");
    public static final SoundEvent PRESTIGE_GET = SoundEvent.of(PRESTIGE_GET_ID);
    public static final Identifier ORDER_REFRESH_ID = new Identifier(ModConstants.MOD_ID, "order_refresh");
    public static final SoundEvent ORDER_REFRESH = SoundEvent.of(ORDER_REFRESH_ID);
    public static final Identifier FOOD_PLATE_PLACE_ID = new Identifier(ModConstants.MOD_ID, "food_plate_place");
    public static final SoundEvent FOOD_PLATE_PLACE = SoundEvent.of(FOOD_PLATE_PLACE_ID);
    public static final Identifier PLATE_PLACE_ID = new Identifier(ModConstants.MOD_ID, "plate_place");
    public static final SoundEvent PLATE_PLACE = SoundEvent.of(PLATE_PLACE_ID);
    public static final Identifier WASHING_PLATE_ID = new Identifier(ModConstants.MOD_ID, "washing_plate");
    public static final SoundEvent WASHING_PLATE = SoundEvent.of(WASHING_PLATE_ID);
    public static final Identifier HORN_ID = new Identifier(ModConstants.MOD_ID, "horn");
    public static final SoundEvent HORN = SoundEvent.of(HORN_ID);

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, PRESTIGE_GET_ID, PRESTIGE_GET);
        Registry.register(Registries.SOUND_EVENT, ORDER_REFRESH_ID, ORDER_REFRESH);
        Registry.register(Registries.SOUND_EVENT, FOOD_PLATE_PLACE_ID, FOOD_PLATE_PLACE);
        Registry.register(Registries.SOUND_EVENT, PLATE_PLACE_ID, PLATE_PLACE);
        Registry.register(Registries.SOUND_EVENT, WASHING_PLATE_ID, WASHING_PLATE);
        Registry.register(Registries.SOUND_EVENT, HORN_ID, HORN);
    }
}
