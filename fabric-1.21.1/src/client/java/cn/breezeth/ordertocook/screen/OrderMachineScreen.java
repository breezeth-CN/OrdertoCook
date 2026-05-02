package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.config.ConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.util.math.Rect2i;

public class OrderMachineScreen extends HandledScreen<OrderMachineScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(ModConstants.MOD_ID, "textures/gui/order_machine.png");
    private ButtonWidget upgradeButton;
    private ButtonWidget toggleButton;
    private ButtonWidget renameButton;
    private TextFieldWidget renameField;
    private boolean renaming = false;
    private int confirmX, confirmY, confirmW, confirmH;
    private int cancelX, cancelY, cancelW, cancelH;

    public OrderMachineScreen(OrderMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        cn.breezeth.ordertocook.compat.JeiCompat.ensureGuiHandlersRegistered();
        cn.breezeth.ordertocook.network.ModClientNetworking.sendRestaurantNameQuery();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        int btnW = 60, btnH = 20;
        int bx = getSidePanelX(x);
        int by = y + 20;
        upgradeButton = ButtonWidget.builder(Text.translatable("screen.ordertocook.order_machine.upgrade"), b -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(bx, by, btnW, btnH).build();
        this.addDrawableChild(upgradeButton);

        // Toggle button below upgrade button, also on the right outside the UI
        int tbx = bx;
        by += (btnH + 4);
        toggleButton = ButtonWidget.builder(Text.empty(), b -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 2);
            }
        }).dimensions(tbx, by, btnW, btnH).build();
        this.addDrawableChild(toggleButton);

        int rx = bx;
        int ry = by + (int) Math.round(btnH * 1.5);
        ButtonWidget rankButton = ButtonWidget.builder(Text.translatable("screen.ordertocook.order_machine.rank"), b -> {
            if (this.client != null) {
                this.client.setScreen(new RestaurantRankingScreen(this));
            }
        }).dimensions(rx, ry, btnW, btnH).build();
        this.addDrawableChild(rankButton);
        int ny = ry + (btnH + 4);
        renameButton = ButtonWidget.builder(Text.translatable("screen.ordertocook.order_machine.rename"), b -> {
            if (renaming) return;
            renaming = true;
            int fx = rx;
            int fy = ny + btnH + 4;
            renameField = new TextFieldWidget(this.textRenderer, fx, fy, 140, 18, Text.translatable("screen.ordertocook.order_machine.rename.input"));
            renameField.setMaxLength(32);
            this.addDrawableChild(renameField);
            this.setFocused(renameField);
            renameField.setFocused(true);
        }).dimensions(rx, ny, btnW, btnH).build();
        this.addDrawableChild(renameButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        cn.breezeth.ordertocook.compat.JeiCompat.ensureGuiHandlersRegistered();
    }

    @Override
    public void removed() {
        super.removed();
    }
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        if (upgradeButton != null) {
            int level = handler.getLevel();
            upgradeButton.visible = level < 8;
            if (upgradeButton.visible && isPointWithinBounds(upgradeButton.getX() - ((width - backgroundWidth) / 2), upgradeButton.getY() - ((height - backgroundHeight) / 2), upgradeButton.getWidth(), upgradeButton.getHeight(), mouseX, mouseY)) {
                Text[] tips = UpgradeTooltipBuilder.build(level, level + 1);
                context.drawTooltip(this.textRenderer, java.util.Arrays.asList(tips), mouseX, mouseY);
            }
        }
        if (toggleButton != null) {
            boolean active = handler.isActive();
            toggleButton.setMessage(active ? Text.translatable("screen.ordertocook.order_machine.rest") : Text.translatable("screen.ordertocook.order_machine.accept"));
        }
        if (renameButton != null) {
            int gx = (width - backgroundWidth) / 2;
            int gy = (height - backgroundHeight) / 2;
            if (isPointWithinBounds(renameButton.getX() - gx, renameButton.getY() - gy, renameButton.getWidth(), renameButton.getHeight(), mouseX, mouseY)) {
                java.util.List<Text> tips = new java.util.ArrayList<>();
                String curName = cn.breezeth.ordertocook.network.ModClientNetworking.LAST_OPEN_SCREEN_NAME;
                String owner = cn.breezeth.ordertocook.network.ModClientNetworking.LAST_OPEN_SCREEN_OWNER;
                if ((curName == null || curName.isEmpty()) || (owner == null || owner.isEmpty())) {
                    cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats self =
                            cn.breezeth.ordertocook.network.ModClientNetworking.findSelfRestaurant();
                    if (self != null) {
                        if (curName == null || curName.isEmpty()) curName = self.name();
                        if (owner == null || owner.isEmpty()) owner = self.owner();
                    }
                }
                if (curName == null || curName.isEmpty()) {
                    curName = net.minecraft.client.resource.language.I18n.translate("screen.ordertocook.order_machine.rename.unnamed");
                }
                if (owner == null || owner.isEmpty()) {
                    owner = (this.client != null && this.client.player != null) ? this.client.player.getGameProfile().getName() : "";
                }
                tips.add(Text.translatable("screen.ordertocook.order_machine.rename.current", curName));
                tips.add(Text.translatable("screen.ordertocook.order_machine.rename.owner", owner));
                tips.add(Text.translatable("screen.ordertocook.order_machine.rename.tooltip"));
                String pn2 = (this.client != null && this.client.player != null) ? this.client.player.getGameProfile().getName() : "";
                if (owner != null && !owner.isEmpty() && !owner.equals(pn2)) {
                    tips.add(Text.translatable("screen.ordertocook.order_machine.rename.only_owner").copy().formatted(Formatting.RED));
                }
                context.drawTooltip(this.textRenderer, tips, mouseX, mouseY);
            }
        }
        if (renaming && renameField != null) {
            String confirmText = net.minecraft.client.resource.language.I18n.translate("screen.ordertocook.order_machine.rename.confirm");
            String cancelText = net.minecraft.client.resource.language.I18n.translate("screen.ordertocook.order_machine.rename.cancel");
            confirmX = renameField.getX();
            confirmY = renameField.getY() + 20;
            confirmW = this.textRenderer.getWidth(confirmText) + 6;
            confirmH = 14;
            cancelX = confirmX + confirmW + 12;
            cancelY = confirmY;
            cancelW = this.textRenderer.getWidth(cancelText) + 6;
            cancelH = 14;
            context.drawText(this.textRenderer, Text.literal(confirmText), confirmX + 3, confirmY, 0x00FF00, false);
            context.drawText(this.textRenderer, Text.literal(cancelText), cancelX + 3, cancelY, 0xFF0000, false);
        }
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;
        int level = handler.getLevel();
        Text levelText = Text.translatable("screen.ordertocook.order_machine.level", level);
        int levelWidth = this.textRenderer.getWidth(levelText);
        int levelX = guiLeft + backgroundWidth - levelWidth - 8;
        int levelY = guiTop + 6;
        if (mouseX >= levelX && mouseX <= levelX + levelWidth && mouseY >= levelY && mouseY <= levelY + this.textRenderer.fontHeight) {
            Text[] tips = CurrentLevelTooltipBuilder.build(level);
            context.drawTooltip(this.textRenderer, java.util.Arrays.asList(tips), mouseX, mouseY);
        }
        int eff = handler.getBenefitLevel();
        if (eff < level) {
            Text effText = Text.translatable("screen.ordertocook.order_machine.effective_level", eff).copy().formatted(Formatting.RED);
            int effWidth = this.textRenderer.getWidth(effText);
            int effX = guiLeft + backgroundWidth - effWidth - 8;
            int effY = guiTop + 18;
            if (mouseX >= effX && mouseX <= effX + effWidth && mouseY >= effY && mouseY <= effY + this.textRenderer.fontHeight) {
                Text tip = Text.translatable("screen.ordertocook.order_machine.effective_level_warn", level).copy().formatted(Formatting.RED);
                context.drawTooltip(this.textRenderer, java.util.List.of(tip), mouseX, mouseY);
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY);
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

    private int getSidePanelX(int guiLeft) {
        return guiLeft + backgroundWidth + 8;
    }

    public boolean oc$isRenaming() {
        return renaming;
    }

    public Rect2i oc$getRenameFieldRect() {
        if (renameField == null) return null;
        return new Rect2i(renameField.getX(), renameField.getY(), renameField.getWidth(), renameField.getHeight());
    }

    public Rect2i oc$getConfirmRect() {
        if (!renaming || renameField == null) return null;
        String confirmText = net.minecraft.client.resource.language.I18n.translate("screen.ordertocook.order_machine.rename.confirm");
        int w = this.textRenderer.getWidth(confirmText) + 6;
        int h = 14;
        int x = renameField.getX();
        int y = renameField.getY() + 20;
        return new Rect2i(x, y, w, h);
    }

    public Rect2i oc$getCancelRect() {
        if (!renaming || renameField == null) return null;
        String confirmText = net.minecraft.client.resource.language.I18n.translate("screen.ordertocook.order_machine.rename.confirm");
        int cw = this.textRenderer.getWidth(confirmText) + 6;
        String cancelText = net.minecraft.client.resource.language.I18n.translate("screen.ordertocook.order_machine.rename.cancel");
        int w = this.textRenderer.getWidth(cancelText) + 6;
        int h = 14;
        int x = renameField.getX() + cw + 12;
        int y = renameField.getY() + 20;
        return new Rect2i(x, y, w, h);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        int progress = handler.getProgress();
        int cdMinutes = handler.getCooldownMinutes();
        int totalTicks = cdMinutes * 60 * 20;
        int remainingTicks = Math.max(0, totalTicks - progress);
        
        int totalSeconds = remainingTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        String mm = String.format("%02d", minutes);
        String ss = String.format("%02d", seconds);
        boolean active = handler.isActive();
        Text timeText = active
                ? Text.translatable("screen.ordertocook.order_machine.time_left", mm, ss)
                : Text.translatable("screen.ordertocook.order_machine.paused");
        Text promptText = active
                ? Text.translatable("screen.ordertocook.order_machine.prompt")
                : Text.translatable("screen.ordertocook.order_machine.choose");
        int level = handler.getLevel();
        Text levelText = Text.translatable("screen.ordertocook.order_machine.level", level);

        context.drawText(this.textRenderer, timeText, 8, 6, 0x404040, false);
        context.drawText(this.textRenderer, promptText, 8, 18, 0x404040, false);
        int rightX = this.backgroundWidth - this.textRenderer.getWidth(levelText) - 8;
        context.drawText(this.textRenderer, levelText, rightX, 6, 0x404040, false);
        int eff = handler.getBenefitLevel();
        if (eff < level) {
            Text effText = Text.translatable("screen.ordertocook.order_machine.effective_level", eff).copy().formatted(Formatting.RED);
            int effX = this.backgroundWidth - this.textRenderer.getWidth(effText) - 8;
            context.drawText(this.textRenderer, effText, effX, 18, 0xFF4040, false);
        }
    }

    private static class UpgradeTooltipBuilder {
        static Text[] build(int currentLevel, int nextLevel) {
            if (nextLevel > 8) {
                return new Text[]{Text.translatable("screen.ordertocook.order_machine.max_level")};
            }
            int cost = upgradeCost(nextLevel);
            int requiredHunger = requiredBoardHunger(nextLevel);
            java.util.List<Text> lines = new java.util.ArrayList<>();
            lines.add(Text.translatable("screen.ordertocook.order_machine.upgrade.cost", cost));
            if (requiredHunger > 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.upgrade.board_hunger", requiredHunger));
            }

            int slotDelta = unlockedSlots(nextLevel) - unlockedSlots(currentLevel);
            if (slotDelta != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.current.slots", "+" + slotDelta).copy().formatted(Formatting.GREEN));
            }

            int rewardDeltaPct = baseHungerBonusPercent(nextLevel) - baseHungerBonusPercent(currentLevel);
            if (rewardDeltaPct != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.current.reward_plus", rewardDeltaPct + "%").copy().formatted(Formatting.GREEN));
            }

            int[] wCur = weightBonus(currentLevel);
            int[] wNext = weightBonus(nextLevel);
            int purpleDelta = wNext[0] - wCur[0];
            int redDelta = wNext[1] - wCur[1];
            if (purpleDelta != 0 || redDelta != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.current.weight_bonus", purpleDelta, redDelta).copy().formatted(Formatting.GREEN));
            }

            int urgentDelta = urgentBonusPct(nextLevel) - urgentBonusPct(currentLevel);
            if (urgentDelta != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.current.urgent_bonus", urgentDelta).copy().formatted(Formatting.GREEN));
            }

            int walkInDelta = walkInChancePct(nextLevel) - walkInChancePct(currentLevel);
            if (walkInDelta != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.walkin_chance", "+" + walkInDelta).copy().formatted(Formatting.GREEN));
            }

            int longDisDelta = longDistanceBonusPct(nextLevel) - longDistanceBonusPct(currentLevel);
            if (longDisDelta != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.current.long_distance_bonus", longDisDelta).copy().formatted(Formatting.GREEN));
            }
            
            int deliveryDelta = longDistanceBonusPct(nextLevel) - longDistanceBonusPct(currentLevel);
            if (deliveryDelta != 0) {
                lines.add(Text.translatable("screen.ordertocook.order_machine.current.delivery_bonus", deliveryDelta).copy().formatted(Formatting.GREEN));
            }
            return lines.toArray(new Text[0]);
        }
    }

    private static class CurrentLevelTooltipBuilder {
        static Text[] build(int level) {
            java.util.List<Text> lines = new java.util.ArrayList<>();
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.effects").copy().append(Text.literal(" (" + level + "/8)")));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.slots", unlockedSlots(level)));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.reward_value", baseHungerBonusPercent(level) + "%"));

            int[] probs = orderProbabilitiesPct(level);
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.order_prob_white", probs[0]));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.order_prob_green", probs[1]));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.order_prob_blue", probs[2]));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.order_prob_purple", probs[3]));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.order_prob_red", probs[4]));

            int deliveryRatePct = (int) Math.round(ConfigManager.get().deliveryRate * 100.0);
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.delivery_rate", deliveryRatePct));

            double urgentRate = Math.min(1.0, ConfigManager.get().urgentRate + (urgentBonusPct(level) / 100.0));
            int urgentRatePct = (int) Math.round(urgentRate * 100.0);
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.urgent_rate", urgentRatePct));

            lines.add(Text.translatable("screen.ordertocook.order_machine.walkin_chance", walkInChancePct(level)));

            double deliveryMultiplier = ConfigManager.get().deliveryMultiplier * (1.0 + (longDistanceBonusPct(level) / 100.0));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.delivery_multiplier", formatNumber(deliveryMultiplier)));

            double longDistanceMultiplier = ConfigManager.get().longDistanceDeliveryMultiplier * (1.0 + (longDistanceBonusPct(level) / 100.0));
            lines.add(Text.translatable("screen.ordertocook.order_machine.current.long_distance_multiplier", formatNumber(longDistanceMultiplier)));
            return lines.toArray(new Text[0]);
        }
    }

    private static int upgradeCost(int nextLevel) {
        return switch (nextLevel) {
            case 1 -> 2;
            case 2 -> 20;
            case 3 -> 40;
            case 4 -> 100;
            case 5 -> 300;
            case 6 -> 500;
            case 7 -> 500;
            case 8 -> 800;
            default -> 0;
        };
    }

    private static int requiredBoardHunger(int nextLevel) {
        var list = ConfigManager.get().orderMachineUpgradeBoardHunger;
        if (list == null || list.isEmpty()) return 0;
        if (nextLevel < 0) return 0;
        if (nextLevel >= list.size()) return list.get(list.size() - 1);
        return list.get(nextLevel);
    }

    private static int unlockedSlots(int level) {
        return switch (level) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    private static int baseHungerBonusPercent(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 15;
            case 3 -> 20;
            case 4 -> 25;
            case 5 -> 30;
            case 6 -> 40;
            case 7 -> 50;
            case 8 -> 75;
            default -> 0;
        };
    }

    private static int[] weightBonus(int level) {
        return switch (level) {
            case 3 -> new int[]{5, 2};
            case 4 -> new int[]{5, 3};
            case 5 -> new int[]{8, 3};
            case 6 -> new int[]{10, 5};
            case 7 -> new int[]{20, 10};
            case 8 -> new int[]{20, 20};
            default -> new int[]{0, 0};
        };
    }

    private static int[] orderProbabilitiesPct(int level) {
        int w0 = ConfigManager.get().weightWhite;
        int w1 = ConfigManager.get().weightGreen;
        int w2 = ConfigManager.get().weightBlue;
        int w3 = ConfigManager.get().weightPurple;
        int w4 = ConfigManager.get().weightRed;

        int baseTotal = w0 + w1 + w2 + w3 + w4;
        if (baseTotal <= 0) baseTotal = 1;

        int[] bonus = weightBonus(level);
        if (bonus[0] > 0) {
            w3 += Math.max(1, (int) Math.round(baseTotal * (bonus[0] / 100.0)));
        }
        if (bonus[1] > 0) {
            w4 += Math.max(1, (int) Math.round(baseTotal * (bonus[1] / 100.0)));
        }

        int total = w0 + w1 + w2 + w3 + w4;
        if (total <= 0) total = 1;

        int p0 = (int) Math.round(w0 * 100.0 / total);
        int p1 = (int) Math.round(w1 * 100.0 / total);
        int p2 = (int) Math.round(w2 * 100.0 / total);
        int p3 = (int) Math.round(w3 * 100.0 / total);
        int p4 = (int) Math.round(w4 * 100.0 / total);
        return new int[]{p0, p1, p2, p3, p4};
    }

    private static int urgentBonusPct(int level) {
        return switch (level) {
            case 3 -> 5;
            case 4 -> 5;
            case 5 -> 10;
            case 6 -> 15;
            case 7 -> 25;
            case 8 -> 30;
            default -> 0;
        };
    }

    private static int walkInChancePct(int level) {
        return switch (level) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            case 4 -> 20;
            case 5 -> 30;
            case 6 -> 40;
            case 7 -> 50;
            case 8 -> 70;
            default -> 0;
        };
    }

    private static int longDistanceBonusPct(int level) {
        return switch (level) {
            case 5 -> 30;
            case 6 -> 40;
            case 7 -> 50;
            case 8 -> 100;
            default -> 0;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && renaming && renameField != null) {
            if (mouseX >= confirmX && mouseX <= confirmX + confirmW && mouseY >= confirmY && mouseY <= confirmY + confirmH) {
                String name = renameField.getText();
                if (name == null) name = "";
                cn.breezeth.ordertocook.network.ModClientNetworking.sendRestaurantRename(name);
                clearRename();
                return true;
            }
            if (mouseX >= cancelX && mouseX <= cancelX + cancelW && mouseY >= cancelY && mouseY <= cancelY + cancelH) {
                clearRename();
                return true;
            }
            if (renameField.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(renameField);
                renameField.setFocused(true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renaming && renameField != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                clearRename();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String name = renameField.getText();
                if (name == null) name = "";
                cn.breezeth.ordertocook.network.ModClientNetworking.sendRestaurantRename(name);
                clearRename();
                return true;
            }
            if (renameField.isFocused()) {
                if (renameField.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (renaming && renameField != null && renameField.isFocused()) {
            return renameField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    private void clearRename() {
        renaming = false;
        if (renameField != null) {
            remove(renameField);
            renameField = null;
        }
    }

    private static String formatNumber(double value) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        int dot = s.indexOf('.');
        if (dot < 0) return s;
        int end = s.length();
        while (end > dot + 1 && s.charAt(end - 1) == '0') end--;
        if (end > dot && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }
}
