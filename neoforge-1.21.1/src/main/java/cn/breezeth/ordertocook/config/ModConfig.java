package cn.breezeth.ordertocook.config;

import blue.endless.jankson.Comment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModConfig {
    @Comment("========== Order Machine / Levels ==========\nMax number of Order Machines allowed within 24 blocks (Default: 1)")
    public int maxOrderMachinesWithin24 = 1;

    @Comment("Order Machine refresh cooldown in seconds (Default: 600)")
    public int orderMachineRefreshSeconds = 600;

    @Comment("Walk-in customer spawn attempt interval in seconds (Default: 60)")
    public int walkInAttemptIntervalSeconds = 60;

    @Comment("Order Machine upgrade requirement: Sum of board menu hunger within 24 blocks.\nIndex = requirement for that level; Lv.0 is always 0.\nDefault: [0,40,80,120,160,200,300,400,600]")
    public List<Integer> orderMachineUpgradeBoardHunger = new ArrayList<>(Arrays.asList(0, 40, 80, 120, 160, 200, 300, 400, 600));

    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.1")
    public double walkInChanceLevel1 = 0.0;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.2")
    public double walkInChanceLevel2 = 0.0;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.3")
    public double walkInChanceLevel3 = 0.1;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.4")
    public double walkInChanceLevel4 = 0.15;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.5")
    public double walkInChanceLevel5 = 0.2;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.6")
    public double walkInChanceLevel6 = 0.25;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.7")
    public double walkInChanceLevel7 = 0.3;
    @Comment("Walk-in customer chance (0.0 - 1.0) - Lv.8")
    public double walkInChanceLevel8 = 0.35;

    @Comment("========== Order Generation ==========\nOrder weight (percent) - White")
    public int weightWhite = 50;
    @Comment("Order weight (percent) - Green")
    public int weightGreen = 25;
    @Comment("Order weight (percent) - Blue")
    public int weightBlue = 15;
    @Comment("Order weight (percent) - Purple")
    public int weightPurple = 8;
    @Comment("Order weight (percent) - Red (rarest)")
    public int weightRed = 2;

    @Comment("Chance to generate Delivery orders (0.0 - 1.0)")
    public double deliveryRate = 0.3;

    @Comment("Chance to generate Urgent orders (0.0 - 1.0)")
    public double urgentRate = 0.1;

    @Comment("Chance to replace Delivery with Long-distance Delivery (0.0 - 1.0)")
    public double longDistanceDeliveryChance = 0.1;

    @Comment("========== Rewards / Currency ==========\nCustom reward item ID (e.g. minecraft:emerald). If empty, oTc coins are used.")
    public String customCurrencyItem = "";

    @Comment("Base oTc coin rewards per order tier [White, Green, Blue, Purple, Red]")
    public List<Integer> defaultCoinArray = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 6));

    @Comment("Extra oTc coins for completing urgent orders (added on top of base)")
    public int rushBonus = 3;

    @Comment("Delivery reward multiplier (e.g. 1.8 means +80%)")
    public double deliveryMultiplier = 1.8;

    @Comment("Long-distance delivery reward multiplier (Default: 4.0)")
    public double longDistanceDeliveryMultiplier = 4.0;

    @Comment("========== NPC / Tips ==========\nNormal customer probability (0.0 - 1.0), Default: 0.9")
    public double normalCustomerRate = 0.9;

    @Comment("Textured NPC gender ratio: female customer proportion (0.0 - 1.0), Default: 0.5; others are male")
    public double customerFemaleRate = 0.5;

    @Comment("Enable multiple resource-pack customer skins. If true, normal customers use textures/entity/customs/custom_wide_x.png and custom_slim_x.png, with x starting at 1 and continuous. Easter egg player skins are unaffected.")
    public boolean customMultipleCustomerSkins = false;

    @Comment("Easter egg customer probability (0.0 - 1.0), Default: 0.1")
    public double easterEggCustomerRate = 0.1;

    @Comment("Tip chance for normal orders (0.0 - 1.0)")
    public double tipNormalChance = 0.1;
    @Comment("Tip chance for urgent orders (0.0 - 1.0)")
    public double tipUrgentChance = 0.5;
    @Comment("Tip chance for easter egg customers (0.0 - 1.0)")
    public double tipEasterEggCustomerChance = 1.0;

    @Comment("Tip random range: min value")
    public int tipMin = 1;
    @Comment("Tip random range: max value")
    public int tipMax = 3;
    @Comment("Rainy day tip random range: min value")
    public int rainTipMin = 3;
    @Comment("Rainy day tip random range: max value")
    public int rainTipMax = 5;

    @Comment("========== Debug ==========\nEnable dev mode debug chat messages (Default: false)")
    public boolean devMode = false;

    @Comment("========== Countertop / Packaging ==========\nWhether crafting takeout bags consumes leather from shelf (Default: true=consume)")
    public boolean countertopConsumeLeather = true;

    @Comment("========== Compatibility ==========\nEnable SDMShop / SDM Economy digital currency. Uses the configured SDM currency key instead of physical oTc coins.")
    public boolean sdmShopCurrencyCompat = false;
    @Comment("SDM Economy currency key used when SDMShop currency compatibility is enabled.")
    public String sdmShopCurrencyKey = "basic_money";

    /*
     * ========== Food Filtering (Removed) ==========
     * Previous versions provided blacklist/whitelist to filter foods from the "all item pool" for orders.
     * New version uses the Board (Menu) system instead.
     */
    @Comment("========== Names Language ==========\nCustomer name list language: zh_cn or en_us")
    public String namesLanguage = "zh_cn";
}
