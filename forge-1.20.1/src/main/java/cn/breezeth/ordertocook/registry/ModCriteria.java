package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.advancement.ExpiredBagEatenCriterion;
import cn.breezeth.ordertocook.advancement.OrderCompletedCriterion;
import cn.breezeth.ordertocook.advancement.TotalCoinCriterion;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModCriteria {
    public static final OrderCompletedCriterion ORDER_COMPLETED = new OrderCompletedCriterion();
    public static final TotalCoinCriterion TOTAL_COIN = new TotalCoinCriterion();
    public static final ExpiredBagEatenCriterion EXPIRED_BAG_EATEN = new ExpiredBagEatenCriterion();

    private static boolean registered = false;

    public static void register(IEventBus modBus) {
        if (registered) return;
        CriteriaTriggers.register(ORDER_COMPLETED);
        CriteriaTriggers.register(TOTAL_COIN);
        CriteriaTriggers.register(EXPIRED_BAG_EATEN);
        registered = true;
    }
}
