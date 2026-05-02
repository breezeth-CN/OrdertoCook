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
import java.util.Optional;

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
        try {
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            return internalClass.getMethod("getJeiRuntime").invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalStateException) {
                return null;
            }
            throw e;
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

    @FunctionalInterface
    private interface ExtraAreaProvider {
        List<Rect2i> get(Object screen);
    }
}
