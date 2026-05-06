package cn.breezeth.ordertocook.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class TotalCoinCriterion extends SimpleCriterionTrigger<TotalCoinCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("ordertocook", "total_coin");

    @Override
    protected Conditions createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        int minTotal = json.has("min_total") ? json.get("min_total").getAsInt() : 0;
        return new Conditions(player, minTotal);
    }

    public void trigger(ServerPlayer player, int total) {
        this.trigger(player, c -> total >= c.minTotal);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final int minTotal;

        public Conditions(ContextAwarePredicate player, int minTotal) {
            super(ID, player);
            this.minTotal = minTotal;
        }
    }
}
