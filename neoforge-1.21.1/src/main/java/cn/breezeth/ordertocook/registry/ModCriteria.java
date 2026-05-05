package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.advancement.ExpiredBagEatenCriterion;
import cn.breezeth.ordertocook.advancement.OrderCompletedCriterion;
import cn.breezeth.ordertocook.advancement.TotalCoinCriterion;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriteria {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES, ModConstants.MOD_ID);

    public static final OrderCompletedCriterion ORDER_COMPLETED = new OrderCompletedCriterion();
    public static final TotalCoinCriterion TOTAL_COIN = new TotalCoinCriterion();
    public static final ExpiredBagEatenCriterion EXPIRED_BAG_EATEN = new ExpiredBagEatenCriterion();

    static {
        TRIGGERS.register("order_completed", () -> ORDER_COMPLETED);
        TRIGGERS.register("total_coin", () -> TOTAL_COIN);
        TRIGGERS.register("expired_bag_eaten", () -> EXPIRED_BAG_EATEN);
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }
}
