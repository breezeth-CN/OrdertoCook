package cn.breezeth.ordertocook.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class OrderCompletedCriterion extends AbstractCriterion<OrderCompletedCriterion.Conditions> {
    public static final Identifier ID = Identifier.of("ordertocook", "order_completed");

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, int coin, boolean urgent, boolean longDistance) {
        this.trigger(player, c -> c.matches(urgent, longDistance));
    }

    public static record Conditions(Optional<LootContextPredicate> player, boolean urgent, boolean longDistance) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                LootContextPredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.BOOL.optionalFieldOf("urgent", false).forGetter(Conditions::urgent),
                Codec.BOOL.optionalFieldOf("long_distance", false).forGetter(Conditions::longDistance)
        ).apply(inst, Conditions::new));

        public boolean matches(boolean isUrgent, boolean isLongDistance) {
            if (urgent && !isUrgent) return false;
            if (longDistance && !isLongDistance) return false;
            return true;
        }
    }
}
