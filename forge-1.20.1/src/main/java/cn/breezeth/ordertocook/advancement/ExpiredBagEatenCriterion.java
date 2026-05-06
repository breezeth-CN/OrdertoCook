package cn.breezeth.ordertocook.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ExpiredBagEatenCriterion extends SimpleCriterionTrigger<ExpiredBagEatenCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("ordertocook", "expired_bag_eaten");

    @Override
    protected Conditions createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Conditions(player);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, c -> true);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        public Conditions(ContextAwarePredicate player) {
            super(ID, player);
        }
    }
}
