package cn.breezeth.ordertocook.client;

import cn.breezeth.ordertocook.client.renderer.CustomerEntityRenderer;
import cn.breezeth.ordertocook.client.renderer.SeatEntityRenderer;
import cn.breezeth.ordertocook.client.renderer.WashingTableWaterWorldRenderer;
import cn.breezeth.ordertocook.network.ModClientNetworking;
import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.breezeth.ordertocook.core.ModConstants;

import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import cn.breezeth.ordertocook.screen.OrderMachineScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

import cn.breezeth.ordertocook.screen.TakeoutBoxScreen;
import cn.breezeth.ordertocook.screen.BoardScreen;
import cn.breezeth.ordertocook.command.ModClientCommands;

public class OrderToCookModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.MOD_ID);

    @Override
    public void onInitializeClient() {
        WashingTableWaterWorldRenderer.register();

        HandledScreens.register(ModScreenHandlers.ORDER_MACHINE_SCREEN_HANDLER, OrderMachineScreen::new);
        HandledScreens.register(ModScreenHandlers.COUNTERTOP_SCREEN_HANDLER, TakeoutBoxScreen::new);
        HandledScreens.register(ModScreenHandlers.BOARD_SCREEN_HANDLER, BoardScreen::new);
        ModClientNetworking.registerClientReceivers();
        ModClientCommands.register();
        new OrderToCookClient().onInitializeClient();

        EntityRendererRegistry.register(ModEntities.SEAT, SeatEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CUSTOMER, CustomerEntityRenderer::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TAKEOUT_BAG, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WASHINGTABLE, RenderLayer.getCutout());
    }
}
