package cn.breezeth.ordertocook.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class OrderCompletedCriterion extends AbstractCriterion<OrderCompletedCriterion.Conditions> {
    public static final Identifier ID = new Identifier("ordertocook", "order_completed");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    protected Conditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        boolean urgent = JsonHelper.getBoolean(obj, "urgent", false);
        boolean longDistance = JsonHelper.getBoolean(obj, "long_distance", false);
        return new Conditions(ID, playerPredicate, urgent, longDistance);
    }

    public void trigger(ServerPlayerEntity player, int coin, boolean urgent, boolean longDistance) {
        this.trigger(player, conditions -> conditions.matches(urgent, longDistance));
    }

    public static class Conditions extends AbstractCriterionConditions {
        private final boolean urgent;
        private final boolean longDistance;

        public Conditions(Identifier id, LootContextPredicate playerPredicate, boolean urgent, boolean longDistance) {
            super(id, playerPredicate);
            this.urgent = urgent;
            this.longDistance = longDistance;
        }

        public boolean matches(boolean isUrgent, boolean isLongDistance) {
            if (urgent && !isUrgent) {
                return false;
            }
            if (longDistance && !isLongDistance) {
                return false;
            }
            return true;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.addProperty("urgent", this.urgent);
            jsonObject.addProperty("long_distance", this.longDistance);
            return jsonObject;
        }
    }
}
