package cn.breezeth.ordertocook.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class TotalCoinCriterion extends SimpleCriterionTrigger<TotalCoinCriterion.Conditions> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ordertocook", "total_coin");

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, int total) {
        this.trigger(player, c -> total >= c.minTotal);
    }

    public static record Conditions(Optional<ContextAwarePredicate> player, int minTotal) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.INT.optionalFieldOf("min_total", 0).forGetter(Conditions::minTotal)
        ).apply(inst, Conditions::new));
    }
}
