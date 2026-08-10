package cn.breezeth.ordertocook.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RefrigeratorScreen extends HandledScreen<RefrigeratorScreenHandler> {
    private static final Identifier BG_TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final Identifier SORT_TEXTURE = Identifier.of("ordertocook", "textures/gui/refrigerator_sort.png");
    private static final Identifier SORT_HOVER_TEXTURE = Identifier.of("ordertocook", "textures/gui/refrigerator_sort_hover.png");
    private static final int SORT_TEXTURE_SIZE = 16;
    private static final int SORT_BUTTON_SIZE = 12;
    private final int rows;
    private ButtonWidget sortButton;

    public RefrigeratorScreen(RefrigeratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.rows = handler.rows();
        this.backgroundHeight = 114 + this.rows * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        sortButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            if (client != null && client.interactionManager != null) {
                client.interactionManager.clickButton(handler.syncId, RefrigeratorScreenHandler.SORT_BUTTON_ID);
            }
        }).dimensions(x + (backgroundWidth - SORT_BUTTON_SIZE) / 2, y + 5, SORT_BUTTON_SIZE, SORT_BUTTON_SIZE).build());
        sortButton.setAlpha(0.0F);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BG_TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(BG_TEXTURE, x, y, 0, 0, backgroundWidth, rows * 18 + 17);
        context.drawTexture(BG_TEXTURE, x, y + rows * 18 + 17, 0, 126, backgroundWidth, 96);
    }

    @Override
    protected void drawSlot(DrawContext context, Slot slot) {
        int actualCount = slot.id < handler.containerSlotCount() ? handler.actualCount(slot.id) : 0;
        if (slot.hasStack() && actualCount > slot.getStack().getMaxCount()) {
            ItemStack stack = slot.getStack();
            int originalCount = stack.getCount();
            try {
                stack.setCount(1);
                super.drawSlot(context, slot);
            } finally {
                stack.setCount(originalCount);
            }
            return;
        }
        super.drawSlot(context, slot);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderSortIcon(context);
        context.draw();
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 300.0F);
        for (Slot slot : handler.slots) {
            if (slot.id >= handler.containerSlotCount()) {
                continue;
            }
            int actualCount = handler.actualCount(slot.id);
            if (!slot.hasStack() || actualCount <= slot.getStack().getMaxCount()) {
                continue;
            }
            String text = String.valueOf(actualCount);
            context.drawText(textRenderer, text, x + slot.x + 19 - 2 - textRenderer.getWidth(text), y + slot.y + 6 + 3, 0xFFFFFF, true);
        }
        context.getMatrices().pop();
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderSortIcon(DrawContext context) {
        if (sortButton == null || !sortButton.visible) return;
        float scale = (float) SORT_BUTTON_SIZE / SORT_TEXTURE_SIZE;
        context.getMatrices().push();
        context.getMatrices().translate(sortButton.getX(), sortButton.getY(), 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTexture(SORT_TEXTURE, 0, 0, 0, 0, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE);
        if (sortButton.isHovered()) {
            context.drawTexture(SORT_HOVER_TEXTURE, 0, 0, 0, 0, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE);
        }
        context.getMatrices().pop();
    }
}
