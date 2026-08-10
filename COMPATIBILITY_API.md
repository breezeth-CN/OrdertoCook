# OrderToCook 兼容 API（v1）

本项目在四个发行端提供语义一致的公开 API：Fabric 1.20.1、Forge 1.20.1、Fabric 1.21.1、NeoForge 1.21.1。类均位于 `cn.breezeth.ordertocook.api`，当前版本号为 `OrderToCookApi.API_VERSION == 1`。

第三方模组应在自身公共初始化阶段完成注册。注册项使用唯一命名空间 ID；同一 API 中重复 ID 会直接报错。数字越大的优先级越先执行，优先级相同时按 ID 排序。第三方处理器抛出的异常会被记录并隔离，不会中断 OrderToCook 主流程。

> 1.20.1 Fabric 示例中的 `Identifier`，在 Forge 中对应 `ResourceLocation`；1.21.1 请使用该加载器和映射版本对应的资源标识类型。其余契约不变。

## `custom_menu_items.json5` 与菜单 API

`config/ordertocook/custom_menu_items.json5` 继续保留，并且是服主/整合包作者最简单、优先级最高的固定配置入口，不会被 API 替代，也没有新增一套重复的数据包菜单格式。

解析顺序为：

1. `custom_menu_items.json5` 中与当前 `ItemStack` 匹配的条目；
2. `MenuFoodCompatApi` 注册规则，按优先级取第一个非 `PASS` 结果；
3. 原版食物组件及其饱食度；
4. 无匹配时拒绝进入菜单。

不带 `nbt_data` 的配置作用于整个物品 ID；带 `nbt_data` 的配置只作用于匹配的物品栈，不再泄漏成整个物品 ID 的默认规则。游戏内菜单录入仍写入普通物品级配置；需要 NBT 精确匹配时应手工编辑 JSON5。

```java
MenuFoodCompatApi.register(
    new Identifier("example", "custom_food"),
    100,
    (stack, context) -> {
        if (!isMyFood(stack)) return MenuFoodCompatApi.Result.pass();
        if (isDisabledVariant(stack)) return MenuFoodCompatApi.Result.deny();
        return MenuFoodCompatApi.Result.allow(8);
    }
);
```

`Purpose` 会说明查询用途：`MENU`、`ORDER_GENERATION` 或 `REWARD`。规则应是无副作用的快速查询。

当前订单的 `FoodList` 仍以物品 ID 记录。因此 API 可以按完整 `ItemStack` 判断菜单和饱食度，但同一物品 ID 下的多个动态 NBT 变体还不能在订单材料清单中分别计数。

## 食材来源 API

`IngredientSourceCompatApi` 允许第三方库存参与操作台的食材查询和扣除。OrderToCook 冰箱已作为优先级 `0` 的内置适配器注册，操作台自身也通过该 API 工作。

处理器必须遵守以下事务约定：

- `probe` 只判断某个坐标是否属于该处理器，不能修改世界；返回 `SOURCE` 后，同坐标不会继续交给低优先级处理器。
- `count` 必须无副作用，并返回当前可提供的准确数量。
- `extract(..., true)` 必须无副作用，并返回最多可提取多少。
- `extract(..., false)` 只能在服务端修改库存，返回实际提取数量，且不得超过请求数量。
- 物品键使用完整注册 ID，例如 `minecraft:bread`。

```java
IngredientSourceCompatApi.register(
    new Identifier("example", "pantry"),
    100,
    new IngredientSourceCompatApi.Handler() {
        public Probe probe(World world, BlockPos pos) { /* ... */ }
        public int count(World world, BlockPos pos, String itemId) { /* ... */ }
        public int extract(World world, BlockPos pos, String itemId, int amount, boolean simulate) { /* ... */ }
    }
);
```

批量查询 `countAllNearby` 只扫描一次范围并同时统计所有订单物品，避免第三方适配后按菜品数量重复进行半径扫描。

## 餐厅耗材 API

`RestaurantSupplyCompatApi` 允许第三方方块提供：

- `PACKAGING`：包装材料；
- `CLEAN_PLATE`：干净餐盘；
- `DIRTY_PLATE`：脏餐盘（为后续洗碗/回收联动预留）。

内置包装架和餐盘架已通过优先级 `0` 的默认处理器接入。接口的 `probe`、`count`、`extract` 契约与食材来源一致。多个来源按与操作台的距离由近到远扣除；相同坐标只由第一个声明为 `SOURCE` 的处理器负责。

## 操作台自动化 API

`CountertopAutomationApi.execute(world, pos, action, player, simulate)` 提供 `PACK` 和 `PLATE` 两种操作。`player` 可以为 `null`，因此机器和物流模组无需伪造玩家。

当 `simulate == true` 时只检查，不消耗食材、包装或餐盘，也不改变输出。返回状态可区分无订单、订单过期、缺少食材、缺少耗材、外卖订单不能装盘以及输出被占用等原因。实际执行仅允许在服务端。

OrderToCook 自己的操作台界面按钮也调用此 API，因此第三方自动化与玩家点击使用同一套校验和执行逻辑。

## 订单生命周期 API

`OrderLifecycleApi` 提供只读、不可取消的订单事件：

- `GENERATED`：订单内容和 ID 已完整写入；
- `ACCEPTED`：玩家从打单机接取订单；
- `PACKED`：订单成功打包或装盘；
- `COMPLETED`：奖励结算完成、物品即将消耗；
- `EXPIRED`：订单或已打包成品首次确认过期；
- `CANCELLED`：为明确的取消流程预留，当前业务没有主动取消入口，因此暂不发送。

```java
OrderLifecycleApi.register(
    new Identifier("example", "order_statistics"),
    0,
    event -> saveStatistics(event.type(), event.player(), event.order())
);
```

`OrderView` 是事件发生时从订单物品复制出的只读快照，食物表也不可修改。监听器不能取消结算，也不应在回调中阻塞服务器线程。

## 本轮明确未提供的接口

本轮没有添加通用顾客替换 API，也没有添加配送载具/容器 API。现有女仆顾客与特定配送兼容仍属于内部兼容逻辑，避免在行为契约尚未稳定时过早固化公共接口。
