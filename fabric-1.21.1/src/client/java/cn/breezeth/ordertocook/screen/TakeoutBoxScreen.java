package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TakeoutBoxScreen extends HandledScreen<TakeoutBoxScreenHandler> {
    private static final Identifier BG_TEXTURE = Identifier.of(ModConstants.MOD_ID, "textures/gui/takeout_box.png");
    private static final int ACTION_BUTTON_X = 134;
    private static final int PACK_BUTTON_Y = 23;
    private static final int PLATE_BUTTON_Y = 45;
    private static final int ACTION_BUTTON_WIDTH = 38;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private ButtonWidget packButton;
    private ButtonWidget plateButton;

    public TakeoutBoxScreen(TakeoutBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        this.packButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ordertocook.countertop.pack"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        })
        .dimensions(x + ACTION_BUTTON_X, y + PACK_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
        .build());

        this.plateButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ordertocook.countertop.plate"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        })
        .dimensions(x + ACTION_BUTTON_X, y + PLATE_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
        .build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BG_TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(BG_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        if (this.packButton != null && this.packButton.isHovered()) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.translatable("screen.ordertocook.countertop.available_bags", this.handler.getAvailablePackagingCount())
            ), mouseX, mouseY);
        } else if (this.plateButton != null && this.plateButton.isHovered()) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.translatable("screen.ordertocook.countertop.available_plates", this.handler.getAvailablePlateCount())
            ), mouseX, mouseY);
        }
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
