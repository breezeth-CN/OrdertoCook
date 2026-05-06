package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RestaurantRankingScreen extends Screen {
    private static final net.minecraft.resources.ResourceLocation MEDAL_1 = new net.minecraft.resources.ResourceLocation("ordertocook", "textures/gui/medal_1st.png");
    private static final net.minecraft.resources.ResourceLocation MEDAL_2 = new net.minecraft.resources.ResourceLocation("ordertocook", "textures/gui/medal_2nd.png");
    private static final net.minecraft.resources.ResourceLocation MEDAL_3 = new net.minecraft.resources.ResourceLocation("ordertocook", "textures/gui/medal_3rd.png");
    private List<OrderMachineBlockEntity.RestaurantStats> data = new ArrayList<>();
    private List<OrderMachineBlockEntity.RestaurantStats> top3Profit = new ArrayList<>();
    private SortKey sortKey = SortKey.PROFIT;
    private boolean ascending = false;
    private int scrollIndex = 0;
    private int rowsPerPage = 20;
    private int headerY = 60;
    private int lastTableStartX = 0;
 
    private int lastHeaderTop = 0;
    private int[] lastColWidths = null;
    private final Screen parent;

    public RestaurantRankingScreen() {
        this(null);
    }

    public RestaurantRankingScreen(Screen parent) {
        super(Component.translatable("screen.ordertocook.ranking.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cn.breezeth.ordertocook.network.ModClientNetworking.sendRestaurantRankingQuery();
    }

    public void updateData(List<OrderMachineBlockEntity.RestaurantStats> list) {
        this.data = new ArrayList<>(list);
        
        // Pre-calculate top 3 profit leaders for medals (only when data changes)
        List<OrderMachineBlockEntity.RestaurantStats> sortedByProfit = new ArrayList<>(this.data);
        sortedByProfit.sort(Comparator.comparingInt(OrderMachineBlockEntity.RestaurantStats::totalProfit).reversed());
        this.top3Profit = sortedByProfit.subList(0, Math.min(3, sortedByProfit.size()));
        
        applySort();
    }

    private void toggleSort(SortKey key) {
        if (this.sortKey == key) {
            ascending = !ascending;
        } else {
            this.sortKey = key;
            ascending = key == SortKey.LEVEL ? false : false;
        }
        applySort();
    }

    private void applySort() {
        Comparator<OrderMachineBlockEntity.RestaurantStats> cmp = switch (sortKey) {
            case NAME -> Comparator.comparing(OrderMachineBlockEntity.RestaurantStats::name, java.lang.String.CASE_INSENSITIVE_ORDER);
            case OWNER -> Comparator.comparing(OrderMachineBlockEntity.RestaurantStats::owner, java.lang.String.CASE_INSENSITIVE_ORDER);
            case LEVEL -> Comparator.comparingInt(OrderMachineBlockEntity.RestaurantStats::level);
            case ACCEPTED -> Comparator.comparingInt(OrderMachineBlockEntity.RestaurantStats::accepted);
            case PROFIT -> Comparator.comparingInt(OrderMachineBlockEntity.RestaurantStats::totalProfit);
            case MAX_DIST -> Comparator.comparingInt(OrderMachineBlockEntity.RestaurantStats::maxDeliveryDist);
            case WALKIN -> Comparator.comparingInt(OrderMachineBlockEntity.RestaurantStats::walkIn);
        };
        if (!ascending) cmp = cmp.reversed();
        data.sort(cmp);
        scrollIndex = 0;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        Component title = Component.translatable("screen.ordertocook.ranking.title");
        int titleX = this.width / 2 - font.width(title) / 2;
        int titleY = 28;
        context.drawString(font, title, titleX, titleY, 0xFFFFFF, false);
        int startY = headerY;
        int lineH = 14;
        int row = 0;
        String selfName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : "";
        int maxLevel = 0, maxAccepted = 0, maxProfit = 0, maxMaxDist = 0, maxWalkIn = 0;
        for (var s : data) {
            if (s.level() > maxLevel) maxLevel = s.level();
            if (s.accepted() > maxAccepted) maxAccepted = s.accepted();
            if (s.totalProfit() > maxProfit) maxProfit = s.totalProfit();
            if (s.maxDeliveryDist() > maxMaxDist) maxMaxDist = s.maxDeliveryDist();
            if (s.walkIn() > maxWalkIn) maxWalkIn = s.walkIn();
        }
        int availableW = Math.max(0, this.width - 40);
        int columns = 8;
        double[] weights = new double[]{0.3, 0.8, 0.8, 1.0, 1.0, 1.0, 1.0, 1.0};
        double sumW = 0;
        for (double w : weights) sumW += w;
        double unit = availableW / sumW;
        int[] colWidths = new int[columns];
        for (int i = 0; i < columns; i++) {
            colWidths[i] = Math.max( (i==0?36:(i<=2?70:60)), (int)Math.floor(unit * weights[i]) );
        }
        int tableW = 0;
        for (int w : colWidths) tableW += w;
        int startX = (this.width - tableW) / 2;
        lastTableStartX = startX;
        lastColWidths = colWidths;
        int headerH = 20;
        int headerTop = startY - headerH - 2;
        lastHeaderTop = headerTop;
        int headerColor = 0xFFFFFF;
        Component[] headers = new Component[]{
                Component.nullToEmpty("#"),
                Component.translatable("screen.ordertocook.ranking.header.name"),
                Component.translatable("screen.ordertocook.ranking.header.owner"),
                Component.translatable("screen.ordertocook.ranking.header.level"),
                Component.translatable("screen.ordertocook.ranking.header.accepted"),
                Component.translatable("screen.ordertocook.ranking.header.profit"),
                Component.translatable("screen.ordertocook.ranking.header.max_distance"),
                Component.translatable("screen.ordertocook.ranking.header.walkin")
        };
        int accX = startX;
        context.fill(startX, headerTop, startX + tableW, headerTop + headerH, 0xB0202020);
        for (int c = 0; c < columns; c++) {
            Component ht = headers[c];
            int color = headerColor;
            if (c >= 1) {
                SortKey k = switch (c) {
                    case 1 -> SortKey.NAME;
                    case 2 -> SortKey.OWNER;
                    case 3 -> SortKey.LEVEL;
                    case 4 -> SortKey.ACCEPTED;
                    case 5 -> SortKey.PROFIT;
                    case 6 -> SortKey.MAX_DIST;
                    case 7 -> SortKey.WALKIN;
                    default -> SortKey.LEVEL;
                };
                if (k == sortKey) {
                    String arrow = ascending ? "^" : "v";
                    ht = Component.nullToEmpty(ht.getString() + " " + arrow);
                    color = 0xFF8080;
                }
            }
            int cellX = accX;
            int cw = colWidths[c];
            boolean hover = mouseX >= cellX && mouseX <= cellX + cw && mouseY >= headerTop && mouseY <= headerTop + headerH;
            int hx = cellX + cw / 2 - font.width(ht) / 2;
            int hy = headerTop + (headerH - font.lineHeight) / 2;
            int hc = hover ? 0xFFFFA0 : color;
            context.drawString(font, ht, hx, hy, hc, false);
            if (hover) {
                int uw = font.width(ht);
                int uy = hy + font.lineHeight;
                context.fill(hx, uy, hx + uw, uy + 1, 0x60FFFFA0);
            }
            accX += cw;
        }
        int maxRows = Math.max(1, (this.height - startY - 40) / lineH);
        rowsPerPage = maxRows;
        int end = Math.min(data.size(), scrollIndex + rowsPerPage);
        int tableTop = headerTop;
        int tableHeight = headerH + rowsPerPage * lineH;
        int tableLeft = startX;
        int tableRight = startX + tableW;
        int tableBottom = tableTop + tableHeight;
        context.fill(tableLeft - 1, tableTop - 1, tableRight + 1, tableTop, 0x80FFFFFF);
        context.fill(tableLeft - 1, tableBottom, tableRight + 1, tableBottom + 1, 0x80FFFFFF);
        context.fill(tableLeft - 1, tableTop, tableLeft, tableBottom, 0x80FFFFFF);
        context.fill(tableRight, tableTop, tableRight + 1, tableBottom, 0x80FFFFFF);
        int sepX = startX;
        for (int c = 1; c < columns; c++) {
            sepX += colWidths[c-1];
            context.fill(sepX, tableTop, sepX + 1, tableBottom, 0x40333333);
        }
        context.fill(tableLeft - 4, tableTop - 4, tableLeft - 1, tableTop - 1, 0x80FFFFFF);
        context.fill(tableRight + 1, tableTop - 4, tableRight + 4, tableTop - 1, 0x80FFFFFF);
        context.fill(tableLeft - 4, tableBottom + 1, tableLeft - 1, tableBottom + 4, 0x80FFFFFF);
        context.fill(tableRight + 1, tableBottom + 1, tableRight + 4, tableBottom + 4, 0x80FFFFFF);

        for (int i = scrollIndex; i < end; i++) {
            OrderMachineBlockEntity.RestaurantStats s = data.get(i);
            int y = startY + row * lineH;
            int bgTop = y - 2;
            int bgBottom = y + lineH - 2;
            int stripe = row % 2 == 0 ? 0x60383838 : 0x90303030;
            context.fill(startX, bgTop, startX + tableW, bgBottom, stripe);
            int colX = startX;
            int cw0 = colWidths[0];
            String idxStr = String.valueOf(i + 1);
            int ix = colX + cw0 / 2 - font.width(idxStr) / 2;
            context.drawString(font, Component.literal(idxStr), ix, y, 0xFFFFFF, false);
            colX += cw0;
            boolean isSelf = s.owner().equals(selfName);
            int nameColor = isSelf ? 0x00FF00 : 0xFFFFFF;
            int cw1 = colWidths[1];
            String nameStr = s.name();
            if (nameStr == null || nameStr.isEmpty()) {
                nameStr = net.minecraft.client.resources.language.I18n.get("screen.ordertocook.order_machine.rename.unnamed");
            }
            nameStr = fitToWidth(nameStr, cw1 - 18); // Give space for medal
            int textW1 = font.width(nameStr);
            
            // Draw medal for TOP 3 BY PROFIT
            int rankByProfit = -1;
            for (int k = 0; k < top3Profit.size(); k++) {
                if (top3Profit.get(k).name().equals(s.name()) && top3Profit.get(k).owner().equals(s.owner())) {
                    rankByProfit = k;
                    break;
                }
            }
            
            boolean drawMedal = rankByProfit >= 0;
            int medalW = drawMedal ? 12 : 0;
            int gap = drawMedal ? 4 : 0;
            int totalW1 = medalW + gap + textW1;
            int start1 = colX + cw1 / 2 - totalW1 / 2;
            
            if (drawMedal) {
                net.minecraft.resources.ResourceLocation tex = (rankByProfit == 0) ? MEDAL_1 : (rankByProfit == 1 ? MEDAL_2 : MEDAL_3);
                context.blit(tex, start1, y - 1, 0, 0, medalW, medalW, medalW, medalW);
                
                // Add tooltip for 1st place
                if (rankByProfit == 0 && mouseX >= start1 && mouseX <= start1 + medalW && mouseY >= y - 1 && mouseY <= y - 1 + medalW) {
                    context.renderComponentTooltip(font, List.of(
                        Component.translatable("screen.ordertocook.ranking.medal.gold.title").withStyle(net.minecraft.ChatFormatting.GOLD),
                        Component.translatable("screen.ordertocook.ranking.medal.gold.desc").withStyle(net.minecraft.ChatFormatting.GRAY)
                    ), mouseX, mouseY);
                }
            }
            int nx = start1 + medalW + gap;
            context.drawString(font, Component.literal(nameStr), nx, y, nameColor, false);
            colX += cw1;
            int cw2 = colWidths[2];
            String ownerStr = fitToWidth(s.owner(), cw2 - 6);
            int ox = colX + cw2 / 2 - font.width(ownerStr) / 2;
            context.drawString(font, Component.literal(ownerStr), ox, y, nameColor, false);
            colX += cw2;
            int levelColor = s.level() == maxLevel ? 0xFFD700 : (isSelf ? 0x00FF00 : 0xFFFFFF);
            String levelStr = String.valueOf(s.level());
            int cw3 = colWidths[3];
            int lx = colX + cw3 / 2 - font.width(levelStr) / 2;
            context.drawString(font, Component.literal(levelStr), lx, y, levelColor, false);
            colX += cw3;
            int cw4 = colWidths[4];
            String acceptedStr = s.accepted() + Component.translatable("screen.ordertocook.ranking.format.parentheses", s.delivery()).getString();
            String accepted = fitToWidth(acceptedStr, cw4 - 6);
            int acceptedColor = s.accepted() == maxAccepted ? 0xFFD700 : (isSelf ? 0x00FF00 : 0xFFFFFF);
            int ax = colX + cw4 / 2 - font.width(accepted) / 2;
            context.drawString(font, Component.literal(accepted), ax, y, acceptedColor, false);
            colX += cw4;
            int cw5 = colWidths[5];
            String profitStr = s.totalProfit() + Component.translatable("screen.ordertocook.ranking.format.parentheses", s.deliveryProfit()).getString();
            String profit = fitToWidth(profitStr, cw5 - 6);
            int profitColor = s.totalProfit() == maxProfit ? 0xFFD700 : (isSelf ? 0x00FF00 : 0xFFFFFF);
            int px = colX + cw5 / 2 - font.width(profit) / 2;
            context.drawString(font, Component.literal(profit), px, y, profitColor, false);
            colX += cw5;
            int distColor = s.maxDeliveryDist() == maxMaxDist ? 0xFFD700 : (isSelf ? 0x00FF00 : 0xFFFFFF);
            String distStr = String.valueOf(s.maxDeliveryDist());
            int cw6 = colWidths[6];
            int dx = colX + cw6 / 2 - font.width(distStr) / 2;
            context.drawString(font, Component.literal(distStr), dx, y, distColor, false);
            colX += cw6;
            int walkColor = s.walkIn() == maxWalkIn ? 0xFFD700 : (isSelf ? 0x00FF00 : 0xFFFFFF);
            String walkStr = String.valueOf(s.walkIn());
            int cw7 = colWidths[7];
            int wx = colX + cw7 / 2 - font.width(walkStr) / 2;
            context.drawString(font, Component.literal(walkStr), wx, y, walkColor, false);
            row++;
        }
        
    }

 

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        int total = data.size();
        int maxIndex = Math.max(0, total - rowsPerPage);
        scrollIndex -= (int) Math.signum(verticalAmount) * Math.max(1, rowsPerPage / 3);
        if (scrollIndex < 0) scrollIndex = 0;
        if (scrollIndex > maxIndex) scrollIndex = maxIndex;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int columns = 8;
            int startX = lastTableStartX;
            int[] colWArr = lastColWidths;
            int headerTop = lastHeaderTop;
            int headerH = 16;
            if (colWArr != null) {
                for (int c = 0; c < columns; c++) {
                    int x = startX;
                    for (int i = 0; i < c; i++) x += colWArr[i];
                    int cw = colWArr[c];
                    if (mouseX >= x && mouseX <= x + cw && mouseY >= headerTop && mouseY <= headerTop + headerH) {
                        switch (c) {
                            case 1 -> toggleSort(SortKey.NAME);
                            case 2 -> toggleSort(SortKey.OWNER);
                            case 3 -> toggleSort(SortKey.LEVEL);
                            case 4 -> toggleSort(SortKey.ACCEPTED);
                            case 5 -> toggleSort(SortKey.PROFIT);
                            case 6 -> toggleSort(SortKey.MAX_DIST);
                            case 7 -> toggleSort(SortKey.WALKIN);
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.parent != null) {
            this.minecraft.setScreen(this.parent);
            return;
        }
        super.onClose();
    }

    private enum SortKey {
        NAME, OWNER, LEVEL, ACCEPTED, PROFIT, MAX_DIST, WALKIN
    }

    private String fitToWidth(String s, int maxW) {
        if (s == null) return "";
        if (font.width(s) <= maxW) return s;
        int len = s.length();
        String ell = "...";
        int ellW = font.width(ell);
        for (int i = len - 1; i >= 1; i--) {
            String sub = s.substring(0, i);
            if (font.width(sub) + ellW <= maxW) {
                return sub + ell;
            }
        }
        return s;
    }
}
