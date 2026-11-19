# Handcrafted Soft-Compat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 温馨家具店软兼容 Handcrafted（`handcrafted`），装到才有货。

**Architecture:** 扩展 `FURNITURE_MOD_NAMESPACES` + `mods.toml` 可选依赖；不新开店、不硬依赖。

**模块契约（从已批准设计原样抄入，不得省略或改写）：**

- 第二次使用：已有——扫货、专营判定、货架规划都走同一集合；本次只扩名单，不抽新抽象。
- 公开面：仍是 `isFurnitureModNamespace` / `isFurnitureCatalogItem` / `isFurnitureShopExclusive`；调用方不感知具体 modid 列表。
- 依赖方向：`PriceCatalog` ← 商店规划/定价；`mods.toml` 只声明软依赖，Java 不引用 Handcrafted API。
- 故意不做：不做「按模组分子店」、不做 Handcrafted 专属排除表、不预留插件式注册器、不 compileOnly。

**Tech Stack:** MC 1.20.1 / Forge 47 / Java 17；现有 unitTest。

**规格来源：** `下一步的计划-家具店-Handcrafted.md`

## File Structure

| 文件 | 职责 |
|------|------|
| `PriceCatalogFurnitureNamespaceTest.java`（新建） | 断言 `handcrafted` 进家具命名空间 |
| `PriceCatalog.java` | 名单加 `handcrafted` |
| `mods.toml` | 可选依赖 |
| `版本改动.md` / 计划 md | 文档对齐 |

---

### Task 1: 命名空间软兼容

**Files:**
- Create: `src/unitTest/java/com/homestead/sellveg/market/PriceCatalogFurnitureNamespaceTest.java`
- Modify: `PriceCatalog.java`, `mods.toml`, docs

- [ ] **Step 1: 写失败测试** — `assertTrue(PriceCatalog.isFurnitureModNamespace(new ResourceLocation("handcrafted:cushion")))`
- [x] **Step 1b: 纯 JVM 改测源码/`mods.toml` 文本（PriceCatalog 无法在 unitTest 加载）**
- [x] **Step 2: 跑测确认红**
- [x] **Step 3: 名单加 `handcrafted` + mods.toml 可选依赖**
- [x] **Step 4: 跑测绿**
- [x] **Step 5: 更新版本改动与计划文档勾选**
- [x] **Step 6: 提交并打 jar 进测试实例**
