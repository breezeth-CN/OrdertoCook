package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<MotorcycleEntity> MOTORCYCLE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "motorcycle"),
            EntityType.Builder.create(MotorcycleEntity::new, SpawnGroup.MISC)
                    .dimensions(MotorcycleEntity.WIDTH, MotorcycleEntity.HEIGHT)
                    .maxTrackingRange(8)
                    .trackingTickInterval(1)
                    .build(Identifier.of(ModConstants.MOD_ID, "motorcycle").toString())
    );

    public static final EntityType<SeatEntity> SEAT = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "seat"),
            EntityType.Builder.create(SeatEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .disableSaving()
                    .disableSummon()
                    .build(Identifier.of(ModConstants.MOD_ID, "seat").toString())
    );

    public static final EntityType<CustomerEntity> CUSTOMER = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "customer"),
            EntityType.Builder.create(CustomerEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.95f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(1)
                    .build(Identifier.of(ModConstants.MOD_ID, "customer").toString())
    );

    public static void registerModEntities() {
        // Just to trigger static init
    }
}
