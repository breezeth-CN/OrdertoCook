package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TakeoutBoxScreen extends AbstractContainerScreen<TakeoutBoxScreenHandler> {
    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/takeout_box.png");
    private static final int ACTION_BUTTON_X = 134;
    private static final int PACK_BUTTON_Y = 23;
    private static final int PLATE_BUTTON_Y = 45;
    private static final int ACTION_BUTTON_WIDTH = 38;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private Button packButton;
    private Button plateButton;

    public TakeoutBoxScreen(TakeoutBoxScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.packButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.ordertocook.countertop.pack"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        })
        .bounds(x + ACTION_BUTTON_X, y + PACK_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
        .build());

        this.plateButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.ordertocook.countertop.plate"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        })
        .bounds(x + ACTION_BUTTON_X, y + PLATE_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
        .build());
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BG_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        context.blit(BG_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        super.renderLabels(context, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        if (this.packButton != null && this.packButton.isHovered()) {
            context.renderTooltip(this.font,
                    Component.translatable("screen.ordertocook.countertop.available_bags", this.menu.getAvailablePackagingCount()),
                    mouseX, mouseY);
        } else if (this.plateButton != null && this.plateButton.isHovered()) {
            context.renderTooltip(this.font,
                    Component.translatable("screen.ordertocook.countertop.available_plates", this.menu.getAvailablePlateCount()),
                    mouseX, mouseY);
        }
        renderTooltip(context, mouseX, mouseY);
    }
}
