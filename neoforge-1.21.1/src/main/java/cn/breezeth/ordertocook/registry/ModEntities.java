package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ModConstants.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MotorcycleEntity>> MOTORCYCLE = ENTITY_TYPES.register("motorcycle",
            () -> EntityType.Builder.of(MotorcycleEntity::new, MobCategory.MISC)
                    .sized(MotorcycleEntity.WIDTH, MotorcycleEntity.HEIGHT)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "motorcycle").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT = ENTITY_TYPES.register("seat",
            () -> EntityType.Builder.of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .noSave()
                    .noSummon()
                    .build(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "seat").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CustomerEntity>> CUSTOMER = ENTITY_TYPES.register("customer",
            () -> EntityType.Builder.of(CustomerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "customer").toString()));

    public static void registerModEntities(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
