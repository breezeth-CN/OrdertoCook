package cn.breezeth.ordertocook.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class TotalCoinCriterion extends AbstractCriterion<TotalCoinCriterion.Conditions> {
    public static final Identifier ID = Identifier.of("ordertocook", "total_coin");

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, int total) {
        this.trigger(player, c -> total >= c.minTotal);
    }

    public static record Conditions(Optional<LootContextPredicate> player, int minTotal) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                LootContextPredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.INT.optionalFieldOf("min_total", 0).forGetter(Conditions::minTotal)
        ).apply(inst, Conditions::new));
    }
}
