# 建材店树叶与蘑菇方块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建材店专营上架深色橡树树叶（固定 1）与棕/红蘑菇方块、蘑菇柄（价带 2–3）。

**Architecture:** 四项写入 `BUILD_SHOP_EXCLUSIVE`；`putBuildShopBands` 对树叶走固定 1，对三种蘑菇方块走 2–3；`mushroom_stem` 必须在 `endsWith("_stem")`（菌核原木 8–12）之前分支，避免误入原木价带。

**模块契约（从已批准设计原样抄入，不得省略或改写）：**

- 第二次使用：不做通用，只服务这四条 SKU
- 公开面：仍是现有 `isBuildShopExclusive` / `marketBand` / `getBasePrice`
- 依赖方向：店铺层继续只依赖 `PriceCatalog`，不反向依赖店铺
- 故意不做：不批量上架其它树叶；不改菌核原木定价；不新开抽象层

**Tech Stack:** Forge 1.20.1、`PriceCatalog`、JUnit unitTest。

## Global Constraints

- `dark_oak_leaves`：建材专营，底价 1，价带固定 1
- `brown_mushroom_block` / `red_mushroom_block` / `mushroom_stem`：建材专营，底价 2，价带 2–3
- 禁止改 `mod_version`
- 实验分支 `exp/sellveg` 自动 commit，默认不 push

---

## File map

| 文件 | 职责 |
|------|------|
| `PriceCatalog.java` | 白名单 + 价带分支 |
| `default.json` | 四项底价 |
| unitTest | 源码/目录断言 |
| `版本改动.md` | 1.1.0 新增内容中英 |

---

### Task 1: 上架 + 测试 + 文档

- [x] `buildShopExclusiveSet` 加四项
- [x] `putBuildShopBands`：树叶 `{1,1}`；蘑菇三件 `{2,3}`（先于 `_stem`）
- [x] `default.json` 底价
- [x] unitTest 断言
- [x] `版本改动.md`
- [x] commit + 打 jar 进测试实例
