package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.config.ConfigManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Public rules for deciding whether an item stack can be placed on a restaurant menu. */
public final class MenuFoodCompatApi {
    public enum Decision { PASS, ALLOW, DENY }
    public enum Purpose { MENU, ORDER_GENERATION, REWARD }

    public record Context(Purpose purpose) {
        public Context {
            Objects.requireNonNull(purpose, "purpose");
        }
    }

    public record Result(Decision decision, int nutrition, Identifier handlerId) {
        public Result {
            Objects.requireNonNull(decision, "decision");
            nutrition = Math.max(0, nutrition);
        }

        public boolean allowed() { return decision == Decision.ALLOW && nutrition > 0; }
        public static Result pass() { return new Result(Decision.PASS, 0, null); }
        public static Result allow(int nutrition) { return new Result(Decision.ALLOW, nutrition, null); }
        public static Result deny() { return new Result(Decision.DENY, 0, null); }
    }

    @FunctionalInterface
    public interface Rule {
        Result evaluate(ItemStack stack, Context context);
    }

    private record Entry(Identifier id, int priority, Rule rule) {}
    private static final List<Entry> RULES = new ArrayList<>();

    public static synchronized void register(Identifier id, int priority, Rule rule) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rule, "rule");
        if (RULES.stream().anyMatch(entry -> entry.id().equals(id))) {
            throw new IllegalArgumentException("Duplicate menu compatibility rule: " + id);
        }
        RULES.add(new Entry(id, priority, rule));
        RULES.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e -> e.id().toString()));
    }

    public static Result resolve(ItemStack stack, Purpose purpose) {
        if (stack == null || stack.isEmpty()) return Result.deny();
        int configured = ConfigManager.getCustomMenuNutrition(stack);
        if (configured > 0) return new Result(Decision.ALLOW, configured, new Identifier("ordertocook", "custom_menu_config"));
        for (Entry entry : snapshot()) {
            try {
                Result result = entry.rule().evaluate(stack, new Context(purpose));
                if (result == null || result.decision() == Decision.PASS) continue;
                return new Result(result.decision(), result.nutrition(), entry.id());
            } catch (Throwable error) {
                OrderToCookMod.LOGGER.error("Menu compatibility rule {} failed", entry.id(), error);
            }
        }
        FoodComponent food = stack.getItem().getFoodComponent();
        int nutrition = food == null ? 0 : food.getHunger();
        return nutrition > 0 ? Result.allow(nutrition) : Result.deny();
    }

    private static synchronized List<Entry> snapshot() { return List.copyOf(RULES); }
    private MenuFoodCompatApi() {}
}
