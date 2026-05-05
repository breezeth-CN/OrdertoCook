package cn.breezeth.ordertocook.advancement;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ExpiredBagEatenCriterion extends SimpleCriterionTrigger<ExpiredBagEatenCriterion.Conditions> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ordertocook", "expired_bag_eaten");

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, c -> true);
    }

    public static record Conditions(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = Codec.unit(new Conditions(Optional.empty()));
    }
}
