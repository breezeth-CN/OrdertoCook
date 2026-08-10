package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class TakeoutBoxScreen extends AbstractContainerScreen<TakeoutBoxScreenHandler> {
    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/takeout_box.png");
    private static final ResourceLocation QUERY_NONE_TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/takeout_query_none.png");
    private static final ResourceLocation QUERY_MISSING_TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/takeout_query_missing.png");
    private static final ResourceLocation QUERY_READY_TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/takeout_query_ready.png");
    private static final ResourceLocation QUERY_HOVER_TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/takeout_query_hover.png");
    private static final int ACTION_BUTTON_X = 134, PACK_BUTTON_Y = 23, PLATE_BUTTON_Y = 45;
    private static final int ACTION_BUTTON_WIDTH = 38, ACTION_BUTTON_HEIGHT = 20;
    private static final int QUERY_BUTTON_X = 156, QUERY_BUTTON_Y = 4, QUERY_TEXTURE_SIZE = 27, QUERY_BUTTON_SIZE = 16;
    private Button packButton, plateButton, queryButton;

    public TakeoutBoxScreen(TakeoutBoxScreenHandler handler, Inventory inventory, Component title) { super(handler, inventory, title); }

    @Override protected void init() {
        super.init();
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        packButton = addRenderableWidget(Button.builder(Component.translatable("screen.ordertocook.countertop.pack"), b -> clickButton(0)).bounds(x + ACTION_BUTTON_X, y + PACK_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT).build());
        plateButton = addRenderableWidget(Button.builder(Component.translatable("screen.ordertocook.countertop.plate"), b -> clickButton(1)).bounds(x + ACTION_BUTTON_X, y + PLATE_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT).build());
        queryButton = addRenderableWidget(Button.builder(Component.empty(), b -> clickButton(2)).bounds(x + QUERY_BUTTON_X, y + QUERY_BUTTON_Y, QUERY_BUTTON_SIZE, QUERY_BUTTON_SIZE).build());
        queryButton.visible = false;
        queryButton.setAlpha(0.0F);
    }
    private void clickButton(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.setShaderTexture(0, BG_TEXTURE);
        context.blit(BG_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
    @Override public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        if (queryButton != null && minecraft != null && minecraft.level != null) queryButton.visible = menu.getIngredientStatus() != 0 && menu.isQueryButtonVisible(minecraft.level);
        super.render(context, mouseX, mouseY, delta); renderQueryIcon(context);
        if (queryButton != null && queryButton.visible && queryButton.isHovered()) context.renderComponentTooltip(font, buildQueryTooltip(), mouseX, mouseY);
        else if (packButton != null && packButton.isHovered()) context.renderTooltip(font, Component.translatable("screen.ordertocook.countertop.available_bags", menu.getAvailablePackagingCount()), mouseX, mouseY);
        else if (plateButton != null && plateButton.isHovered()) context.renderTooltip(font, Component.translatable("screen.ordertocook.countertop.available_plates", menu.getAvailablePlateCount()), mouseX, mouseY);
        renderTooltip(context, mouseX, mouseY);
    }
    private List<Component> buildQueryTooltip() {
        List<Component> lines = new ArrayList<>();
        if (menu.getIngredientStatus() == 1) lines.add(Component.translatable("message.ordertocook.countertop.query_ready"));
        else if (menu.getMissingIngredientCount() == 0) lines.add(Component.translatable("message.ordertocook.countertop.query_invalid"));
        else { lines.add(Component.translatable("message.ordertocook.countertop.query_missing_header"));
            for (int i = 0; i < menu.getMissingIngredientCount(); i++) { ItemStack food = new ItemStack(BuiltInRegistries.ITEM.byId(menu.getMissingIngredientId(i))); int required = menu.getMissingIngredientRequired(i), available = menu.getMissingIngredientAvailable(i); lines.add(Component.translatable("screen.ordertocook.countertop.query.tooltip_detail", food.getHoverName(), available, required, Math.max(0, required - available))); }
        }
        lines.add(Component.translatable("screen.ordertocook.countertop.query.click_hint")); return lines;
    }
    private void renderQueryIcon(GuiGraphics context) {
        if (queryButton == null || !queryButton.visible) return;
        ResourceLocation texture = menu.getIngredientStatus() == 1 ? QUERY_READY_TEXTURE : (menu.getMissingIngredientCount() > 0 ? QUERY_MISSING_TEXTURE : QUERY_NONE_TEXTURE);
        float scale = (float) QUERY_BUTTON_SIZE / QUERY_TEXTURE_SIZE;
        context.pose().pushPose();
        context.pose().translate(queryButton.getX(), queryButton.getY(), 0.0F);
        context.pose().scale(scale, scale, 1.0F);
        context.blit(texture, 0, 0, 0, 0, QUERY_TEXTURE_SIZE, QUERY_TEXTURE_SIZE, QUERY_TEXTURE_SIZE, QUERY_TEXTURE_SIZE);
        if (queryButton.isHovered()) context.blit(QUERY_HOVER_TEXTURE, 0, 0, 0, 0, QUERY_TEXTURE_SIZE, QUERY_TEXTURE_SIZE, QUERY_TEXTURE_SIZE, QUERY_TEXTURE_SIZE);
        context.pose().popPose();
    }
}
