package cn.breezeth.ordertocook.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RefrigeratorScreen extends AbstractContainerScreen<RefrigeratorScreenHandler> {
    private static final ResourceLocation BG_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation SORT_TEXTURE = ResourceLocation.fromNamespaceAndPath("ordertocook", "textures/gui/refrigerator_sort.png");
    private static final ResourceLocation SORT_HOVER_TEXTURE = ResourceLocation.fromNamespaceAndPath("ordertocook", "textures/gui/refrigerator_sort_hover.png");
    private static final int SORT_TEXTURE_SIZE = 16;
    private static final int SORT_BUTTON_SIZE = 12;
    private final int rows;
    private Button sortButton;

    public RefrigeratorScreen(RefrigeratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.rows = handler.rows();
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.sortButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, RefrigeratorScreenHandler.SORT_BUTTON_ID);
            }
        }).bounds(this.leftPos + (this.imageWidth - SORT_BUTTON_SIZE) / 2, this.topPos + 5, SORT_BUTTON_SIZE, SORT_BUTTON_SIZE).build());
        this.sortButton.setAlpha(0.0F);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        context.blit(BG_TEXTURE, x, y, 0, 0, this.imageWidth, this.rows * 18 + 17);
        context.blit(BG_TEXTURE, x, y + this.rows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    protected void renderSlotContents(GuiGraphics context, ItemStack stack, Slot slot, @Nullable String countString) {
        int menuSlot = this.menu.slots.indexOf(slot);
        int actualCount = menuSlot >= 0 && menuSlot < this.menu.containerSlotCount() ? this.menu.actualCount(menuSlot) : 0;
        if (actualCount > stack.getMaxStackSize()) {
            super.renderSlotContents(context, stack, slot, "");
            return;
        }
        super.renderSlotContents(context, stack, slot, countString);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderSortIcon(context);
        context.flush();
        renderLargeStackCounts(context);
        renderTooltip(context, mouseX, mouseY);
    }

    private void renderSortIcon(GuiGraphics context) {
        if (sortButton == null || !sortButton.visible) return;
        float scale = (float) SORT_BUTTON_SIZE / SORT_TEXTURE_SIZE;
        context.pose().pushPose();
        context.pose().translate(sortButton.getX(), sortButton.getY(), 0.0F);
        context.pose().scale(scale, scale, 1.0F);
        context.blit(SORT_TEXTURE, 0, 0, 0, 0, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE);
        if (sortButton.isHovered()) {
            context.blit(SORT_HOVER_TEXTURE, 0, 0, 0, 0, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE, SORT_TEXTURE_SIZE);
        }
        context.pose().popPose();
    }

    private void renderLargeStackCounts(GuiGraphics context) {
        context.pose().pushPose();
        context.pose().translate(0.0F, 0.0F, 300.0F);
        for (int menuSlot = 0; menuSlot < this.menu.containerSlotCount(); menuSlot++) {
            Slot slot = this.menu.slots.get(menuSlot);
            int actualCount = this.menu.actualCount(menuSlot);
            if (!slot.hasItem() || actualCount <= slot.getItem().getMaxStackSize()) continue;
            String text = String.valueOf(actualCount);
            context.drawString(this.font, text,
                    this.leftPos + slot.x + 19 - 2 - this.font.width(text),
                    this.topPos + slot.y + 6 + 3, 0xFFFFFF, true);
        }
        context.pose().popPose();
    }
}
