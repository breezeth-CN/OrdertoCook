package cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid;

import cn.breezeth.ordertocook.OrderToCookMod;

import java.util.Map;
import java.util.WeakHashMap;

public final class MaidEatingAnimationState {
    private static final long DURATION_NANOS = 350_000_000L;
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static boolean failureReported;

    public static synchronized void start(Object maid, boolean left) {
        STATES.put(maid, new State(left, System.nanoTime()));
    }

    public static synchronized Sample sample(Object maid) {
        State state = STATES.get(maid);
        if (state == null) return null;
        float progress = (System.nanoTime() - state.startedAt) / (float) DURATION_NANOS;
        if (progress >= 1.0f) {
            STATES.remove(maid);
            return null;
        }
        return new Sample(state.left, Math.max(0.0f, progress));
    }

    public static synchronized void reportFailure(String stage, ReflectiveOperationException exception) {
        if (failureReported) return;
        failureReported = true;
        OrderToCookMod.LOGGER.error("Failed to apply Touhou maid eating animation at {}", stage, exception);
    }

    private record State(boolean left, long startedAt) {}
    public record Sample(boolean left, float progress) {}
}
