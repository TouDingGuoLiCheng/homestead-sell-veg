# 灰化土建材店 + 杜鹃花丛花店 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建材店专营上架灰化土（价带 2–3）；花店专营上架杜鹃花丛，价与盛开的杜鹃花丛同档。

**Architecture:** 灰化土走 `BUILD_SHOP_EXCLUSIVE` + `putBuildShopBands` 专用 2–3；杜鹃花丛走 `FLOWER_SHOP_EXTRA`，沿用装饰植物底价/价带（与 `#minecraft:flowers` 内非小花/非高花的盛开杜鹃一致）。

**模块契约（从已批准设计原样抄入，不得省略或改写）：**

- 第二次使用：不做通用，只服务这两条 SKU
- 公开面：仍是现有 `isBuildShopExclusive` / `isFlowerCatalogItem` / `marketBand` / `getBasePrice`
- 依赖方向：店铺层继续只依赖 `PriceCatalog`，不反向依赖店铺
- 故意不做：不把杜鹃花丛改挂建材店；不改盛开杜鹃货架/定价逻辑；不新开抽象层

**Tech Stack:** Forge 1.20.1、`PriceCatalog`、JUnit unitTest。

## Global Constraints

- 灰化土：建材专营，底价 2，价带 2–3
- 杜鹃花丛：花店专营（`FLOWER_SHOP_EXTRA`），底价/价带同盛开杜鹃（装饰植物档 8 / 约 6–16）
- 禁止改 `mod_version`
- 实验分支 `exp/sellveg` 自动 commit，默认不 push

---

## File map

| 文件 | 职责 |
|------|------|
| `PriceCatalog.java` | `podzol` 入建材白名单与价带；`azalea` 入 `FLOWER_SHOP_EXTRA` |
| `default.json` | `minecraft:podzol: 2`；可选 `minecraft:azalea: 8`（与花店 fallback 一致，便于目录断言） |
| unitTest | 源码/目录断言 |
| `版本改动.md` | 1.1.0 新增内容中英 |

---

### Task 1: 上架 + 测试 + 文档

- [x] `FLOWER_SHOP_EXTRA` 加 `minecraft:azalea`
- [x] `buildShopExclusiveSet` 加 `minecraft:podzol`；`putBuildShopBands` 对 `podzol` 写 `{2,3}`
- [x] `default.json` 补底价
- [x] unitTest 断言
- [x] `版本改动.md`
- [x] commit + 打 jar 进测试实例
