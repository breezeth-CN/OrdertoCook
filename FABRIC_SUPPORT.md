# Fabric 版本冻结说明

Order To Cook 的 Fabric 1.20.1 与 Fabric 1.21.1 已在 **1.3.7a** 停止功能更新。

- 最终维护分支：`fabric-final-1.3.7a`
- 最终版本标签：`v1.3.7a-fabric-final`
- `main` 后续仅更新 Forge 与 NeoForge；Fabric 目录保留，但默认不参与构建。
- Fabric 最终分支只接受导致崩溃、存档损坏或严重兼容问题的必要修复。

重新构建最终 Fabric 版本时，优先检出冻结分支：

```text
git switch fabric-final-1.3.7a
```

如果需要在主线临时加载 Fabric 子工程，可将根目录 `gradle.properties` 中的配置改为：

```properties
enable_fabric_subprojects=true
```
