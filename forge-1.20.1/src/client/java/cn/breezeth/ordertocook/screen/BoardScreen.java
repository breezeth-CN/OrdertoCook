package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class BoardScreen extends AbstractContainerScreen<BoardScreenHandler> {
    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation(ModConstants.MOD_ID, "textures/gui/board_ui.png");
    private static final ResourceLocation SLOT_BG =
            new ResourceLocation(ModConstants.MOD_ID, "textures/gui/board_item_bg.png");
    private static final ResourceLocation SLOT_HOVER_NORMAL =
            new ResourceLocation(ModConstants.MOD_ID, "textures/gui/board_highlight.png");
    private static final ResourceLocation SLOT_HOVER_DELETE =
            new ResourceLocation(ModConstants.MOD_ID, "textures/gui/board_delete.png");

    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 6;
    private static final int COLS = 4;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_M = 2;
    private static final int ROW_HEIGHT = 19;

    private boolean editMode = false;
    private Button editButton;
    private Button sortButton;

    private int guiLeft = 0;
    private int guiTop = 0;
    private int innerX = 0;
    private int innerY = 0;
    private int innerW = 0;
    private int colWidth = 0;

    public BoardScreen(BoardScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        cn.breezeth.ordertocook.compat.JeiCompat.ensureGuiHandlersRegistered();
        updateButtonPosition();
    }

    public int oc$getGuiLeft() {
        return (width - imageWidth) / 2;
    }

    public int oc$getGuiTop() {
        return (height - imageHeight) / 2;
    }

    public int oc$getBackgroundWidth() {
        return imageWidth;
    }

    public int oc$getBackgroundHeight() {
        return imageHeight;
    }

    private void updateButtonPosition() {
        int currentGuiLeft = oc$getGuiLeft();
        int currentGuiTop = oc$getGuiTop();
        int btnW = 56;
        int btnX = currentGuiLeft + this.imageWidth + 4;
        int btnY = currentGuiTop + 18;
        int btnH = 18;

        if (editButton != null) {
            this.removeWidget(editButton);
        }
        if (sortButton != null) {
            this.removeWidget(sortButton);
        }

        editButton = Button.builder(getEditButtonText(), button -> {
            editMode = !editMode;
            button.setMessage(getEditButtonText());
            if (sortButton != null) {
                sortButton.active = editMode;
            }
        }).bounds(btnX, btnY, btnW, btnH).build();
        this.addRenderableWidget(editButton);

        sortButton = Button.builder(getSortButtonText(), button -> {
            if (!editMode || this.minecraft == null || this.minecraft.gameMode == null) {
                return;
            }
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1000);
            button.setMessage(getSortButtonText());
        }).bounds(btnX, btnY + btnH + 4, btnW, btnH).build();
        sortButton.active = editMode;
        this.addRenderableWidget(sortButton);
    }

    private Component getEditButtonText() {
        return editMode
                ? Component.translatable("screen.ordertocook.board.edit.on")
                : Component.translatable("screen.ordertocook.board.edit.off");
    }

    private Component getSortButtonText() {
        int mode = this.menu.getSortMode();
        Component modeText = mode == 2
                ? Component.translatable("screen.ordertocook.board.sort_last_time")
                : mode == 1
                ? Component.translatable("screen.ordertocook.board.sort_mod_id")
                : Component.translatable("screen.ordertocook.board.sort_hunger");
        return Component.translatable("screen.ordertocook.board.sort_mode", modeText);
    }

    private void updateCachedPositions() {
        guiLeft = (this.width - this.imageWidth) / 2;
        guiTop = (this.height - this.imageHeight) / 2;
        innerX = guiLeft + 8;
        innerY = guiTop + 18;
        innerW = this.imageWidth - 16 - (SCROLL_W + SCROLL_M);
        colWidth = innerW / COLS;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        updateCachedPositions();
        graphics.blit(BG_TEXTURE, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);
        drawItemSlots(graphics, mouseX, mouseY);
        drawScrollBar(graphics);
    }

    private void drawItemSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        int total = this.menu.getTemplates().getContainerSize();
        int maxRows = (int) Math.ceil(total / (double) COLS);
        int maxOffset = Math.max(0, maxRows - VISIBLE_ROWS);
        if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }

        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(total, startIndex + VISIBLE_ROWS * COLS);
        for (int i = startIndex; i < endIndex; i++) {
            int local = i - startIndex;
            int col = local % COLS;
            int row = local / COLS;
            int gx = innerX + col * colWidth;
            int gy = innerY + row * ROW_HEIGHT;
            ItemStack stack = this.menu.getTemplates().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            int slotW = colWidth - 1;
            int slotH = 18;
            graphics.blit(SLOT_BG, gx, gy, 0, 0, slotW, slotH, slotW, slotH);
            graphics.renderItem(stack, gx + 1, gy + 1);

            int nutrition = nutritionOf(stack);
            if (nutrition > 0) {
                String text = String.valueOf(nutrition);
                int textWidth = this.font.width(text);
                graphics.drawString(this.font, text, gx + slotW - 2 - textWidth, gy + 6, 0x404040, false);
            }

            if (isMouseOverSlot(mouseX, mouseY, gx, gy, slotW, slotH)) {
                graphics.blit(editMode ? SLOT_HOVER_DELETE : SLOT_HOVER_NORMAL, gx, gy, 0, 0, slotW, slotH, slotW, slotH);
            }
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int trackX = innerX + innerW + SCROLL_M;
        int trackY = innerY;
        int trackH = VISIBLE_ROWS * ROW_HEIGHT;
        graphics.fill(trackX, trackY, trackX + SCROLL_W, trackY + trackH, 0x33000000);

        int totalRows = (int) Math.ceil(this.menu.getTemplates().getContainerSize() / (double) COLS);
        int maxOffset = Math.max(0, totalRows - VISIBLE_ROWS);
        int knobH = Math.max(8, (int) Math.round((VISIBLE_ROWS / (double) Math.max(VISIBLE_ROWS, totalRows)) * trackH));
        int knobY = trackY + (maxOffset == 0 ? 0
                : (int) Math.round((scrollOffset / (double) maxOffset) * (trackH - knobH)));
        graphics.fill(trackX, knobY, trackX + SCROLL_W, knobY + knobH, 0xAA808080);
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY, int slotX, int slotY, int width, int height) {
        return mouseX >= slotX && mouseX < slotX + width
                && mouseY >= slotY && mouseY < slotY + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            updateCachedPositions();
            if (handleTemplateSlotsClick(mouseX, mouseY) || handlePlayerSlotsClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTemplateSlotsClick(double mouseX, double mouseY) {
        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(this.menu.getTemplates().getContainerSize(), startIndex + VISIBLE_ROWS * COLS);
        for (int i = startIndex; i < endIndex; i++) {
            int local = i - startIndex;
            int col = local % COLS;
            int row = local / COLS;
            int gx = innerX + col * colWidth;
            int gy = innerY + row * ROW_HEIGHT;
            if (isMouseOverSlot(mouseX, mouseY, gx, gy, colWidth - 1, 18)) {
                if (editMode && this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100 + i);
                }
                return true;
            }
        }
        return false;
    }

    private boolean handlePlayerSlotsClick(double mouseX, double mouseY) {
        for (int slotIndex = this.menu.getTemplates().getContainerSize(); slotIndex < this.menu.slots.size(); slotIndex++) {
            Slot slot = this.menu.slots.get(slotIndex);
            int sx = guiLeft + slot.x;
            int sy = guiTop + slot.y;
            if (isMouseOverSlot(mouseX, mouseY, sx, sy, 16, 16)) {
                int playerIndex = slot.getContainerSlot();
                if (editMode && playerIndex >= 0 && playerIndex < 36 && this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, playerIndex);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        int total = this.menu.getTemplates().getContainerSize();
        int maxRows = (int) Math.ceil(total / (double) COLS);
        int maxOffset = Math.max(0, maxRows - VISIBLE_ROWS);
        if (scrollY < 0 && scrollOffset < maxOffset) {
            scrollOffset++;
            return true;
        }
        if (scrollY > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        int sum = calculateTotalNutrition();
        Component totalText = Component.translatable("screen.ordertocook.board.total_hunger", String.valueOf(sum));
        int tx = this.imageWidth - 8 - this.font.width(totalText);
        graphics.drawString(this.font, totalText, tx, 6, 0x404040, false);
    }

    private int calculateTotalNutrition() {
        int sum = 0;
        for (int i = 0; i < this.menu.getTemplates().getContainerSize(); i++) {
            ItemStack stack = this.menu.getTemplates().getItem(i);
            if (!stack.isEmpty()) {
                sum += nutritionOf(stack);
            }
        }
        return sum;
    }

    private int nutritionOf(ItemStack stack) {
        int custom = ConfigManager.getCustomMenuNutrition(stack);
        if (custom > 0) {
            return custom;
        }
        FoodProperties food = stack.getItem().getFoodProperties();
        return (food != null && food.getNutrition() > 0) ? food.getNutrition() : 0;
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        updateCachedPositions();
        if (editButton != null && editButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tips = new ArrayList<>();
            tips.add(Component.translatable("screen.ordertocook.board.tooltip.title").withStyle(ChatFormatting.WHITE));
            tips.add(Component.translatable("screen.ordertocook.board.tooltip.add").withStyle(ChatFormatting.GRAY));
            tips.add(Component.translatable("screen.ordertocook.board.tooltip.remove").withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(this.font, tips, mouseX, mouseY);
        }
        renderItemTooltips(graphics, mouseX, mouseY);
    }

    private void renderItemTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(this.menu.getTemplates().getContainerSize(), startIndex + VISIBLE_ROWS * COLS);
        for (int i = startIndex; i < endIndex; i++) {
            int local = i - startIndex;
            int col = local % COLS;
            int row = local / COLS;
            int gx = innerX + col * colWidth;
            int gy = innerY + row * ROW_HEIGHT;
            ItemStack stack = this.menu.getTemplates().getItem(i);
            if (!stack.isEmpty() && isMouseOverSlot(mouseX, mouseY, gx, gy, colWidth - 1, 18)) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        cn.breezeth.ordertocook.compat.JeiCompat.ensureGuiHandlersRegistered();
        int expectedButtonX = oc$getGuiLeft() + this.imageWidth + 4;
        int expectedButtonY = oc$getGuiTop() + 18;
        if (editButton != null && (editButton.getX() != expectedButtonX || editButton.getY() != expectedButtonY)) {
            updateButtonPosition();
        }
        if (sortButton != null) {
            sortButton.active = editMode;
            sortButton.setMessage(getSortButtonText());
        }
    }
}
