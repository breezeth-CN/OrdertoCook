package cn.breezeth.ordertocook.compat;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.screen.BoardScreen;
import cn.breezeth.ordertocook.screen.OrderMachineScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.math.Rect2i;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JEI 集成：与 fabric-1.21.1 中同名类保持同一职责（容器「排除区」+ 列表层显隐）。
 * 反射访问 JEI 内部 API 是为兼容多版本 JEI 包名差异；排查两版本行为时请对照 1.21 的 {@code ensureGuiHandlersRegistered} 与 GUI 侧 {@code oc$get*}。
 */
public final class JeiCompat {
    private static volatile Object registeredRuntime;
    private static volatile boolean registrationFailedLogged;

    private JeiCompat() {
    }

    public static boolean isJeiLoaded() {
        return FabricLoader.getInstance().isModLoaded("jei");
    }

    public static synchronized void ensureGuiHandlersRegistered() {
        if (!isJeiLoaded()) {
            return;
        }
        try {
            Object runtime = getJeiRuntime();
            if (runtime == null || runtime == registeredRuntime) {
                return;
            }
            Object screenHelper = runtime.getClass().getMethod("getScreenHelper").invoke(runtime);
            Object guiContainerHandlers = readDeclaredField(screenHelper, "guiContainerHandlers");
            Class<?> guiContainerHandlerClass = Class.forName("mezz.jei.api.gui.handlers.IGuiContainerHandler");
            Method addMethod = guiContainerHandlers.getClass().getMethod("add", Class.class, guiContainerHandlerClass);
            addMethod.invoke(guiContainerHandlers, OrderMachineScreen.class, createGuiContainerHandler(guiContainerHandlerClass, JeiCompat::getOrderMachineExtraAreas));
            addMethod.invoke(guiContainerHandlers, BoardScreen.class, createGuiContainerHandler(guiContainerHandlerClass, JeiCompat::getBoardExtraAreas));
            registeredRuntime = runtime;
            registrationFailedLogged = false;
            OrderToCookMod.LOGGER.info("Registered JEI gui exclusion areas for OrderToCook");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (!(cause instanceof IllegalStateException)) {
                logRegistrationFailure(cause != null ? cause : e);
            }
        } catch (Throwable t) {
            logRegistrationFailure(t);
        }
    }

    private static Object getJeiRuntime() throws ReflectiveOperationException {
        Class<?> internalClass = tryClass("mezz.jei.common.Internal");
        if (internalClass == null) {
            internalClass = tryClass("mezz.jei.Internal");
        }
        if (internalClass == null) {
            internalClass = tryClass("mezz.jei.fabric.Internal");
        }
        if (internalClass == null) {
            return null;
        }
        try {
            return internalClass.getMethod("getJeiRuntime").invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalStateException) {
                return null;
            }
            throw e;
        }
    }

    private static Class<?> tryClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object readDeclaredField(Object target, String fieldName) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object createGuiContainerHandler(Class<?> guiContainerHandlerClass, ExtraAreaProvider provider) {
        return Proxy.newProxyInstance(
                guiContainerHandlerClass.getClassLoader(),
                new Class<?>[]{guiContainerHandlerClass},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGuiExtraAreas" -> provider.get(args[0]);
                    case "getClickableIngredientUnderMouse" -> Optional.empty();
                    case "getGuiClickableAreas" -> Collections.emptyList();
                    case "toString" -> "OrderToCookJeiGuiHandler";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                }
        );
    }

    private static List<Rect2i> getOrderMachineExtraAreas(Object screen) {
        OrderMachineScreen orderMachineScreen = (OrderMachineScreen) screen;
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(
                orderMachineScreen.oc$getGuiLeft() + orderMachineScreen.oc$getBackgroundWidth() + 8,
                orderMachineScreen.oc$getGuiTop() + 20,
                60,
                98
        ));
        addIfPresent(areas, orderMachineScreen.oc$getRenameFieldRect());
        addIfPresent(areas, orderMachineScreen.oc$getConfirmRect());
        addIfPresent(areas, orderMachineScreen.oc$getCancelRect());
        return areas;
    }

    private static List<Rect2i> getBoardExtraAreas(Object screen) {
        BoardScreen boardScreen = (BoardScreen) screen;
        return List.of(new Rect2i(
                boardScreen.oc$getGuiLeft() + boardScreen.oc$getBackgroundWidth() + 4,
                boardScreen.oc$getGuiTop() + 18,
                56,
                40
        ));
    }

    private static void addIfPresent(List<Rect2i> areas, Rect2i rect) {
        if (rect != null) {
            areas.add(rect);
        }
    }

    private static void logRegistrationFailure(Throwable t) {
        if (registrationFailedLogged) {
            return;
        }
        registrationFailedLogged = true;
        OrderToCookMod.LOGGER.warn("Failed to register JEI gui exclusion areas", t);
    }

    /** 与 1.21 一致：打开部分 GUI 时隐藏 JEI 列表层，避免遮挡右侧按钮（仍依赖 {@link #ensureGuiHandlersRegistered} 排除点击穿透）。 */
    public static void hideOverlay() {
        toggleOverlay(false);
    }

    public static void showOverlay() {
        toggleOverlay(true);
    }

    private static void toggleOverlay(boolean visible) {
        if (!isJeiLoaded()) {
            return;
        }
        try {
            Class<?> internal = tryClass("mezz.jei.common.Internal");
            if (internal == null) {
                internal = tryClass("mezz.jei.Internal");
            }
            if (internal == null) {
                internal = tryClass("mezz.jei.fabric.Internal");
            }
            if (internal == null) {
                internal = tryClass("mezz.jei.forge.Internal");
            }
            if (internal == null) {
                return;
            }
            Object runtime = Objects.requireNonNullElse(tryStatic(internal, "getRuntime"), tryStatic(internal, "getJeiRuntime"));
            if (runtime == null) {
                return;
            }
            Object overlay = tryInvoke(runtime, "getIngredientListOverlay");
            if (overlay == null) {
                overlay = tryInvoke(runtime, "getOverlay");
            }
            if (overlay == null) {
                overlay = tryField(runtime, "ingredientListOverlay");
            }
            if (overlay == null) {
                return;
            }
            // void 方法 invoke 返回 null，不能靠返回值短路；按版本依次尝试。
            tryInvoke(overlay, "setVisible", new Class[]{boolean.class}, new Object[]{visible});
            tryInvoke(overlay, "setEnabled", new Class[]{boolean.class}, new Object[]{visible});
            trySetBooleanField(overlay, "visible", visible);
            trySetBooleanField(overlay, "enabled", visible);
        } catch (Throwable ignored) {
        }
    }

    private static Object tryInvoke(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object tryInvoke(Object target, String method, Class<?>[] types, Object[] args) {
        try {
            return target.getClass().getMethod(method, types).invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object tryStatic(Class<?> clazz, String method) {
        try {
            return clazz.getMethod(method).invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object tryField(Object target, String name) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object trySetBooleanField(Object target, String name, boolean value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setBoolean(target, value);
            return Boolean.TRUE;
        } catch (Throwable t) {
            return null;
        }
    }

    @FunctionalInterface
    private interface ExtraAreaProvider {
        List<Rect2i> get(Object screen);
    }
}
