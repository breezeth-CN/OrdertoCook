package cn.breezeth.ordertocook.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class OrderCompletedCriterion extends SimpleCriterionTrigger<OrderCompletedCriterion.Conditions> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ordertocook", "order_completed");

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, int coin, boolean urgent, boolean longDistance) {
        this.trigger(player, c -> c.matches(urgent, longDistance));
    }

    public static record Conditions(Optional<ContextAwarePredicate> player, boolean urgent, boolean longDistance) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
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
