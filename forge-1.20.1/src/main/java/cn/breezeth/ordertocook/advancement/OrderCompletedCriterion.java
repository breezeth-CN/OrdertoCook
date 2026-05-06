package cn.breezeth.ordertocook.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class OrderCompletedCriterion extends SimpleCriterionTrigger<OrderCompletedCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("ordertocook", "order_completed");

    @Override
    protected Conditions createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        boolean urgent = json.has("urgent") && json.get("urgent").getAsBoolean();
        boolean longDistance = json.has("long_distance") && json.get("long_distance").getAsBoolean();
        return new Conditions(player, urgent, longDistance);
    }

    public void trigger(ServerPlayer player, int coin, boolean urgent, boolean longDistance) {
        this.trigger(player, c -> c.matches(urgent, longDistance));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final boolean urgent;
        private final boolean longDistance;

        public Conditions(ContextAwarePredicate player, boolean urgent, boolean longDistance) {
            super(ID, player);
            this.urgent = urgent;
            this.longDistance = longDistance;
        }

        public boolean matches(boolean isUrgent, boolean isLongDistance) {
            if (urgent && !isUrgent) return false;
            return !longDistance || isLongDistance;
        }
    }
}
