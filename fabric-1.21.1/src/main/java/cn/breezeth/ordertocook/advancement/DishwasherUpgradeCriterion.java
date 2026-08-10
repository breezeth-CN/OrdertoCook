package cn.breezeth.ordertocook.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class DishwasherUpgradeCriterion extends AbstractCriterion<DishwasherUpgradeCriterion.Conditions> {
    public static final Identifier ID = Identifier.of("ordertocook", "dishwasher_upgrade");

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, c -> true);
    }

    public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = Codec.unit(new Conditions(Optional.empty()));
    }
}