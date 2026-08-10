package cn.breezeth.ordertocook.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class DishwasherUpgradeCriterion extends SimpleCriterionTrigger<DishwasherUpgradeCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("ordertocook", "dishwasher_upgrade");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Conditions(player);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, c -> true);
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        public Conditions(ContextAwarePredicate player) {
            super(ID, player);
        }
    }
}
