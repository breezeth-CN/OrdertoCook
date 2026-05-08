package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.config.ConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class BoardScreen extends HandledScreen<BoardScreenHandler> {
        private static final Identifier BG_TEXTURE = Identifier.of("ordertocook", "textures/gui/board_ui.png");
        private static final Identifier SLOT_BG = Identifier.of("ordertocook", "textures/gui/board_item_bg.png");
        private static final Identifier SLOT_HOVER_NORMAL = Identifier.of("ordertocook", "textures/gui/board_highlight.png");
        private static final Identifier SLOT_HOVER_DELETE = Identifier.of("ordertocook", "textures/gui/board_delete.png");
    
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 6;
    private static final int COLS = 4;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_M = 2;
    private boolean editMode = false;
    private ButtonWidget editButton;
    private ButtonWidget sortButton;
    
    // 缓存坐标计算
    private int guiLeft = 0;
    private int guiTop = 0;
    private int innerX = 0;
    private int innerY = 0;
    private int innerW = 0;
    private int colWidth = 0;
    private static final int ROW_HEIGHT = 19;

    public BoardScreen(BoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        cn.breezeth.ordertocook.compat.JeiCompat.ensureGuiHandlersRegistered();
        updateButtonPosition();
    }
    
    public int oc$getGuiLeft() {
        return (width - backgroundWidth) / 2;
    }

    public int oc$getGuiTop() {
        return (height - backgroundHeight) / 2;
    }

    public int oc$getBackgroundWidth() {
        return backgroundWidth;
    }

    public int oc$getBackgroundHeight() {
        return backgroundHeight;
    }
    
    private void updateButtonPosition() {
        int currentGuiLeft = oc$getGuiLeft();
        int currentGuiTop = oc$getGuiTop();
        int btnW = 56;
        int btnX = currentGuiLeft + this.backgroundWidth + 4;
        int btnY = currentGuiTop + 18;
        int btnH = 18;
        
        Text label = getEditButtonText();
        
        if (editButton != null) {
            this.remove(editButton);
        }
        if (sortButton != null) {
            this.remove(sortButton);
        }
        
        editButton = ButtonWidget.builder(label, b -> {
            editMode = !editMode;
            b.setMessage(getEditButtonText());
            if (sortButton != null) sortButton.active = editMode;
        }).dimensions(btnX, btnY, btnW, btnH).build();
        
        this.addDrawableChild(editButton);

        sortButton = ButtonWidget.builder(getSortButtonText(), b -> {
            if (!editMode) return;
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1000);
                b.setMessage(getSortButtonText());
            }
        }).dimensions(btnX, btnY + btnH + 4, btnW, btnH).build();
        sortButton.active = editMode;
        this.addDrawableChild(sortButton);
    }
    
    private Text getEditButtonText() {
        return editMode ? Text.translatable("screen.ordertocook.board.edit.on") : Text.translatable("screen.ordertocook.board.edit.off");
    }

    private Text getSortButtonText() {
        int mode = this.handler.getSortMode();
        Text modeText = (mode == 2)
                ? Text.translatable("screen.ordertocook.board.sort_last_time")
                : (mode == 1
                    ? Text.translatable("screen.ordertocook.board.sort_mod_id")
                    : Text.translatable("screen.ordertocook.board.sort_hunger"));
        return Text.translatable("screen.ordertocook.board.sort_mode", modeText);
    }
    
    private void updateCachedPositions() {
        guiLeft = (this.width - this.backgroundWidth) / 2;
        guiTop = (this.height - this.backgroundHeight) / 2;
        innerX = guiLeft + 8;
        innerY = guiTop + 18;
        innerW = this.backgroundWidth - 16 - (SCROLL_W + SCROLL_M);
        colWidth = innerW / COLS;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        updateCachedPositions();
        
        // 绘制主背景
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTexture(BG_TEXTURE, guiLeft, guiTop, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // 绘制物品槽位
        drawItemSlots(context, mouseX, mouseY);
        
        // 绘制滚动条
        drawScrollBar(context);
    }
    
    private void drawItemSlots(DrawContext context, int mouseX, int mouseY) {
        int total = this.handler.getTemplates().size();
        int maxRows = (int)Math.ceil(total / (double)COLS);
        int maxOffset = Math.max(0, maxRows - VISIBLE_ROWS);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;

        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(total, startIndex + VISIBLE_ROWS * COLS);
        
        for (int i = startIndex; i < endIndex; i++) {
            int local = i - startIndex;
            int col = local % COLS;
            int row = local / COLS;
            int gx = innerX + col * colWidth;
            int gy = innerY + row * ROW_HEIGHT;
            
            ItemStack stack = this.handler.getTemplates().getStack(i);
            if (!stack.isEmpty()) {
                int slotW = colWidth - 1;
                int slotH = 18;
                
                // 绘制槽位背景
                context.drawTexture(SLOT_BG, gx, gy, 0, 0, slotW, slotH, slotW, slotH);
                
                // 绘制物品
                context.drawItem(stack, gx + 1, gy + 1);
                
                // 绘制食物营养值
                int nutrition = nutritionOf(stack);
                if (nutrition > 0) {
                    String s = String.valueOf(nutrition);
                    int sw = this.textRenderer.getWidth(s);
                    context.drawText(this.textRenderer, s, gx + slotW - 2 - sw, gy + 6, 0x404040, false);
                }
                
                // 绘制悬停效果
                if (isMouseOverSlot(mouseX, mouseY, gx, gy, slotW, slotH)) {
                    context.drawTexture(editMode ? SLOT_HOVER_DELETE : SLOT_HOVER_NORMAL, 
                        gx, gy, 0, 0, slotW, slotH, slotW, slotH);
                }
            }
        }
    }
    
    private void drawScrollBar(DrawContext context) {
        int trackX = innerX + innerW + SCROLL_M;
        int trackY = innerY;
        int trackH = VISIBLE_ROWS * ROW_HEIGHT;
        
        // 滚动条轨道
        context.fill(trackX, trackY, trackX + SCROLL_W, trackY + trackH, 0x33000000);
        
        int totalRows = (int)Math.ceil(this.handler.getTemplates().size() / (double)COLS);
        int maxOffset = Math.max(0, totalRows - VISIBLE_ROWS);
        int knobH = Math.max(8, (int)Math.round((VISIBLE_ROWS / (double)Math.max(VISIBLE_ROWS, totalRows)) * trackH));
        int knobY = trackY + (maxOffset == 0 ? 0 : 
            (int)Math.round((scrollOffset / (double)maxOffset) * (trackH - knobH)));
        
        // 滚动条滑块
        context.fill(trackX, knobY, trackX + SCROLL_W, knobY + knobH, 0xAA808080);
    }
    
    private boolean isMouseOverSlot(double mouseX, double mouseY, int slotX, int slotY, int width, int height) {
        return mouseX >= slotX && mouseX < slotX + width && 
               mouseY >= slotY && mouseY < slotY + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            updateCachedPositions();
            
            // 处理模板槽位点击
            if (handleTemplateSlotsClick(mouseX, mouseY)) {
                return true;
            }
            
            // 处理玩家背包槽位点击
            if (handlePlayerSlotsClick(mouseX, mouseY)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean handleTemplateSlotsClick(double mouseX, double mouseY) {
        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(this.handler.getTemplates().size(), startIndex + VISIBLE_ROWS * COLS);
        
        for (int i = startIndex; i < endIndex; i++) {
            int local = i - startIndex;
            int col = local % COLS;
            int row = local / COLS;
            int gx = innerX + col * colWidth;
            int gy = innerY + row * ROW_HEIGHT;
            int w = colWidth - 1;
            int h = 18;
            
            if (isMouseOverSlot(mouseX, mouseY, gx, gy, w, h)) {
                if (this.client != null && this.client.interactionManager != null) {
                    if (editMode) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 100 + i);
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean handlePlayerSlotsClick(double mouseX, double mouseY) {
        for (int si = this.handler.getTemplates().size(); si < this.handler.slots.size(); si++) {
            Slot slot = this.handler.slots.get(si);
            int sx = guiLeft + slot.x;
            int sy = guiTop + slot.y;
            
            if (isMouseOverSlot(mouseX, mouseY, sx, sy, 16, 16)) {
                if (this.client != null && this.client.interactionManager != null) {
                    int playerIndex = slot.getIndex();
                    if (playerIndex >= 0 && playerIndex < 36) {
                        if (editMode) {
                            this.client.interactionManager.clickButton(this.handler.syncId, playerIndex);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int total = this.handler.getTemplates().size();
        int maxRows = (int)Math.ceil(total / (double)COLS);
        int maxOffset = Math.max(0, maxRows - VISIBLE_ROWS);
        if (verticalAmount < 0 && scrollOffset < maxOffset) {
            scrollOffset++;
            return true;
        }
        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        // 渲染物品提示和按钮提示
        renderTooltips(context, mouseX, mouseY);
        
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 0x404040, false);
        
        // 计算并绘制总营养值
        int sum = calculateTotalNutrition();
        String totalText = net.minecraft.client.resource.language.I18n.translate(
            "screen.ordertocook.board.total_hunger", String.valueOf(sum));
        int tx = this.backgroundWidth - 8 - this.textRenderer.getWidth(totalText);
        context.drawText(this.textRenderer, Text.literal(totalText), tx, 6, 0x404040, false);
    }
    
    private int calculateTotalNutrition() {
        int sum = 0;
        for (int i = 0; i < this.handler.getTemplates().size(); i++) {
            ItemStack s = this.handler.getTemplates().getStack(i);
            if (!s.isEmpty()) {
                sum += nutritionOf(s);
            }
        }
        return sum;
    }

    private int nutritionOf(ItemStack stack) {
        int custom = ConfigManager.getCustomMenuNutrition(stack);
        if (custom > 0) {
            return custom;
        }
        FoodComponent food = stack.get(net.minecraft.component.DataComponentTypes.FOOD);
        return (food != null && food.nutrition() > 0) ? food.nutrition() : 0;
    }
    
    private void renderTooltips(DrawContext context, int mouseX, int mouseY) {
        updateCachedPositions();
        
        // 渲染编辑按钮提示
        if (editButton != null && editButton.isMouseOver(mouseX, mouseY)) {
            renderEditButtonTooltip(context, mouseX, mouseY);
        }
        
        // 渲染物品提示
        renderItemTooltips(context, mouseX, mouseY);
    }
    
    private void renderEditButtonTooltip(DrawContext context, int mouseX, int mouseY) {
        List<Text> tips = new ArrayList<>();
        tips.add(Text.literal(net.minecraft.client.resource.language.I18n.translate(
            "screen.ordertocook.board.tooltip.title")).formatted(Formatting.WHITE));
        tips.add(Text.literal(net.minecraft.client.resource.language.I18n.translate(
            "screen.ordertocook.board.tooltip.add")).formatted(Formatting.GRAY));
        tips.add(Text.literal(net.minecraft.client.resource.language.I18n.translate(
            "screen.ordertocook.board.tooltip.remove")).formatted(Formatting.GRAY));
        context.drawTooltip(this.textRenderer, tips, mouseX, mouseY);
    }
    
    private void renderItemTooltips(DrawContext context, int mouseX, int mouseY) {
        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(this.handler.getTemplates().size(), startIndex + VISIBLE_ROWS * COLS);
        
        for (int i = startIndex; i < endIndex; i++) {
            int local = i - startIndex;
            int col = local % COLS;
            int row = local / COLS;
            int gx = innerX + col * colWidth;
            int gy = innerY + row * ROW_HEIGHT;
            int w = colWidth - 1;
            int h = 18;
            
            ItemStack stack = this.handler.getTemplates().getStack(i);
            if (!stack.isEmpty() && isMouseOverSlot(mouseX, mouseY, gx, gy, w, h)) {
                context.drawItemTooltip(this.textRenderer, stack, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        cn.breezeth.ordertocook.compat.JeiCompat.ensureGuiHandlersRegistered();
        int expectedButtonX = oc$getGuiLeft() + this.backgroundWidth + 4;
        int expectedButtonY = oc$getGuiTop() + 18;
        if (editButton != null) {
            if (editButton.getX() != expectedButtonX || editButton.getY() != expectedButtonY) {
                updateButtonPosition();
            }
        }
        if (sortButton != null) {
            sortButton.active = editMode;
            sortButton.setMessage(getSortButtonText());
        }
    }
    @Override
    public void removed() {
        super.removed();
    }
}
