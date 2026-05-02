# 🍳 OrderToCook（下单了）

> 在 Minecraft 里开一家属于自己的餐厅！接单、烹饪、装盘、配送——体验从后厨到餐桌的完整经营流程。

![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen)
![Platform](https://img.shields.io/badge/Fabric%20|%20NeoForge-1.20.1%20|%201.21.1%20|%201.21.4-orange)
![License](https://img.shields.io/badge/License-GPL%20v3-blue)
[![Modrinth](https://img.shields.io/badge/Modrinth-00AF5C?style=flat&logo=modrinth)](https://modrinth.com/mod/order-to-cook)
[![CurseForge](https://img.shields.io/badge/CurseForge-F16436?style=flat&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/order-to-cook)

## 概述

OrderToCook 是一个 Minecraft 餐厅经营模组。放置打单机开始营业，接收各种订单，在操作台上烹饪食物，用打包袋完成外卖配送——经营你的餐厅，赚取 oTc 币，升级设备，登上全服排行榜。

### 核心玩法

- **打单机** — 放置后接取订单，随等级解锁更多槽位、更高收益和特殊订单类型
- **操作台** — 根据订单内容装盘或打包，完成后交给顾客
- **顾客 NPC** — 到店堂食或等待配送，订单超时会流失顾客
- **小电驴** — 骑上它完成远距离配送，可自定义车身颜色
- **oTc 币** — 营收货币，用于升级餐厅和重命名

### 订单类型

| 类型 | 说明 |
|------|------|
| 拼好饭 | 基础订单 |
| 普通套餐 | 标准订单 |
| 奢华套餐 | 高级订单 |
| 多人团聚 | 大额订单 |
| 至尊土豪 | 顶级订单 |

订单附带 **配送**、**远距离配送**、**加急** 等额外属性，对应不同的收益倍率。

## 项目结构

```
├── common/                  # 跨版本通用代码
├── fabric-1.20.1/           # Fabric 1.20.1 平台
├── fabric-1.21.1/           # Fabric 1.21.1 平台
├── gradle/                  # Gradle wrapper
└── build.gradle             # 根项目构建脚本
```

## 开发

本项目使用 Gradle 多模块结构，`common` 模块存放共享逻辑，各平台子模块包含平台特定实现。使用 GeckoLib 进行实体动画渲染。


## 许可

本项目代码基于 GNU General Public License v3.0 开源。详见 [LICENSE.txt](LICENSE.txt)。
