package cn.breezeth.ordertocook.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ExpiredBagEatenCriterion extends AbstractCriterion<ExpiredBagEatenCriterion.Conditions> {
    public static final Identifier ID = new Identifier("ordertocook", "expired_bag_eaten");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    protected Conditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return new Conditions(ID, playerPredicate);
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> true);
    }

    public static class Conditions extends AbstractCriterionConditions {
        public Conditions(Identifier id, LootContextPredicate playerPredicate) {
            super(id, playerPredicate);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            return super.toJson(predicateSerializer);
        }
    }
}
