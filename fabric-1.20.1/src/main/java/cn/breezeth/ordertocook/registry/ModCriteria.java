package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.advancement.ExpiredBagEatenCriterion;
import cn.breezeth.ordertocook.advancement.OrderCompletedCriterion;
import cn.breezeth.ordertocook.advancement.TotalCoinCriterion;
import net.minecraft.advancement.criterion.Criteria;

public class ModCriteria {
    public static final OrderCompletedCriterion ORDER_COMPLETED = Criteria.register(new OrderCompletedCriterion());
    public static final TotalCoinCriterion TOTAL_COIN = Criteria.register(new TotalCoinCriterion());
    public static final ExpiredBagEatenCriterion EXPIRED_BAG_EATEN = Criteria.register(new ExpiredBagEatenCriterion());

    public static void register() {
        // 静态字段初始化完成注册
    }
}
