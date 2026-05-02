package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.advancement.OrderCompletedCriterion;
import cn.breezeth.ordertocook.advancement.ExpiredBagEatenCriterion;
import cn.breezeth.ordertocook.advancement.TotalCoinCriterion;
import net.minecraft.advancement.criterion.Criteria;

public class ModCriteria {
    public static final OrderCompletedCriterion ORDER_COMPLETED = Criteria.register(OrderCompletedCriterion.ID.toString(), new OrderCompletedCriterion());
    public static final TotalCoinCriterion TOTAL_COIN = Criteria.register(TotalCoinCriterion.ID.toString(), new TotalCoinCriterion());
    public static final ExpiredBagEatenCriterion EXPIRED_BAG_EATEN = Criteria.register(ExpiredBagEatenCriterion.ID.toString(), new ExpiredBagEatenCriterion());

    public static void register() {
        // Class loading will trigger static initialization
    }
}
