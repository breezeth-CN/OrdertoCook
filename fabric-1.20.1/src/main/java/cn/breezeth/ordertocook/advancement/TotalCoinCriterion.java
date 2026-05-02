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

public class TotalCoinCriterion extends AbstractCriterion<TotalCoinCriterion.Conditions> {
    public static final Identifier ID = new Identifier("ordertocook", "total_coin");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    protected Conditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        int minTotal = JsonHelper.getInt(obj, "min_total", 0);
        return new Conditions(ID, playerPredicate, minTotal);
    }

    public void trigger(ServerPlayerEntity player, int total) {
        this.trigger(player, conditions -> total >= conditions.minTotal);
    }

    public static class Conditions extends AbstractCriterionConditions {
        private final int minTotal;

        public Conditions(Identifier id, LootContextPredicate playerPredicate, int minTotal) {
            super(id, playerPredicate);
            this.minTotal = minTotal;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.addProperty("min_total", this.minTotal);
            return jsonObject;
        }
    }
}
