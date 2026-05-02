package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.entity.SeatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<SeatEntity> SEAT = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(ModConstants.MOD_ID, "seat"),
            EntityType.Builder.create(SeatEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .disableSaving()
                    .disableSummon()
                    .build(new Identifier(ModConstants.MOD_ID, "seat").toString())
    );

    public static final EntityType<CustomerEntity> CUSTOMER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(ModConstants.MOD_ID, "customer"),
            EntityType.Builder.create(CustomerEntity::new, SpawnGroup.CREATURE)
                    .setDimensions(0.6f, 1.8f)
                    .build(new Identifier(ModConstants.MOD_ID, "customer").toString())
    );

    public static final EntityType<cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity> MOTORCYCLE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(ModConstants.MOD_ID, "motorcycle"),
            EntityType.Builder.create(cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity::new, SpawnGroup.MISC)
                    .setDimensions(cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity.WIDTH, cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity.HEIGHT)
                    .build(new Identifier(ModConstants.MOD_ID, "motorcycle").toString())
    );

    public static void registerModEntities() {
        // Just to trigger static init
    }
}
