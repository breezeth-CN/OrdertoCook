package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StoveScreen extends AbstractContainerScreen<StoveScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/gui/stove.png");

    public StoveScreen(StoveScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        context.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int flame = this.menu.litProgress();
        if (flame > 0) {
            context.blit(TEXTURE, x + 57, y + 36 + 12 - flame, 176, 12 - flame, 14, flame + 1);
        }
        int progress = this.menu.cookProgress(0);
        if (progress > 0) {
            context.blit(TEXTURE, x + 79, y + 35, 176, 14, progress + 1, 16);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}
