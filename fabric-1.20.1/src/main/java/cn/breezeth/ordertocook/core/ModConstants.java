package cn.breezeth.ordertocook.core;

public final class ModConstants {
    private ModConstants() {}
    public static final String MOD_ID = "ordertocook";
    
    // NBT Keys
    public static final String NBT_LEVEL = "order_machine.level";
    public static final String NBT_TYPE = "Type";
    public static final String NBT_DELIVERY = "Delivery";
    public static final String NBT_URGENT = "Urgent";
    public static final String NBT_EXPIRY_TIME = "ExpiryTime";
    public static final String NBT_EXPIRY_TICK = "ExpiryTick";
    public static final String NBT_FOOD_LIST = "FoodList";
    public static final String NBT_PRESTIGE = "Prestige";
    public static final String NBT_DELIVERY_POS = "delivery_pos";
    public static final String NBT_IS_LONG_DISTANCE = "IsLongDistance";
    public static final String NBT_ORDER_ID = "OrderId";
    public static final String NBT_CUSTOMER_NAME = "CustomerName";
    public static final String NBT_X = "x";
    public static final String NBT_Z = "z";
    public static final String NBT_PROGRESS = "order_machine.progress";
    public static final String NBT_INITIALIZED = "order_machine.initialized";
    public static final String NBT_INIT_DELAY = "order_machine.initDelay";
    public static final String NBT_ACTIVE = "order_machine.active";
    public static final String NBT_BOUND_BOARD_POS = "order_machine.bound_board_pos";
    public static final String NBT_PRESTIGE_KEY = "ordertocook:prestige";
    public static final String NBT_MACHINE_POS = "order_machine.pos_long";
    public static final String NBT_MACHINE_DIM = "order_machine.dimension";
    public static final String NBT_MACHINE_ID = "order_machine.id";
    public static final String NBT_DELIVERY_DIST = "delivery_dist";
    public static final String NBT_ORDER_TYPE = "Order_Type";

    // Config
    public static final int REFRESH_TIME = 12000; // 10 minutes

    // Logic constants
    public static final int WALK_IN_INTERVAL_TICKS = 1200; // 60 seconds
    public static final int URGENT_DURATION_TICKS = 10 * 60 * 20; // 10 minutes
    public static final int NORMAL_DURATION_TICKS = 30 * 60 * 20; // 30 minutes

    public static final int DELIVERY_POSITION_ATTEMPTS = 32;
    public static final int SHORT_MIN_DISTANCE = 50;
    public static final int SHORT_MAX_DISTANCE = 450;
    public static final int LONG_MIN_DISTANCE = 1500;
    public static final int LONG_MAX_DISTANCE = 3000;

    // Scan/caching
    public static final int BOARD_SCAN_RADIUS = 24;
    public static final int CHAIR_SCAN_RADIUS = 24;
    public static final int SCAN_VERTICAL = 8;
    public static final int SCAN_CACHE_INTERVAL_TICKS = 200;

    // Restaurant stats keys
    public static final String NBT_RESTAURANT_NAME = "restaurant.name";
    public static final String NBT_RESTAURANT_OWNER = "restaurant.owner";
    public static final String NBT_RESTAURANT_ACCEPTED = "restaurant.accepted";
    public static final String NBT_RESTAURANT_DELIVERY = "restaurant.delivery";
    public static final String NBT_RESTAURANT_LONG_DISTANCE = "restaurant.long_distance";
    public static final String NBT_RESTAURANT_TOTAL_PROFIT = "restaurant.total_profit";
    public static final String NBT_RESTAURANT_DELIVERY_PROFIT = "restaurant.delivery_profit";
    public static final String NBT_RESTAURANT_MAX_DELIVERY_DIST = "restaurant.max_delivery_dist";
    public static final String NBT_RESTAURANT_WALKIN = "restaurant.walkin";

    // Customer NBT Keys
    public static final String NBT_CUSTOMER_TEXTURE_VARIANT = "customer.texture_variant";
    public static final String NBT_CUSTOMER_SKIN_ACCOUNT = "customer.skin_account";
    public static final String NBT_CUSTOMER_SKIN_UUID = "customer.skin_uuid";
    public static final String NBT_CUSTOMER_EASTER_EGG = "customer.easter_egg";
    public static final String NBT_CUSTOMER_ID = "customer.id";
}
