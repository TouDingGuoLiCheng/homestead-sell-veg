# 建材店加入深板岩 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建材店专营上架 `minecraft:deepslate`，价带与石头同档约 1–3。

**Architecture:** 沿用 `PriceCatalog.buildShopExclusiveSet()` + `putBuildShopBands()` 廉价档；目录 `default.json` 补底价 1，避免缺底价不上架。

**Tech Stack:** Forge 1.20.1、现有 `PriceCatalog` / 建材专营货架管线、JUnit unitTest。

## Global Constraints

- 只加 `minecraft:deepslate`；不加圆石/砖/瓦/阶/墙变体
- 价带 `CHEAP_BUILD_MIN`～`CHEAP_BUILD_MAX`（1–3）
- 禁止改 `mod_version`
- 实验分支 `exp/sellveg` 自动 commit，默认不 push

---

## File map

| 文件 | 职责 |
|------|------|
| `src/main/java/.../market/PriceCatalog.java` | `buildShopExclusiveSet()` 加入 deepslate |
| `src/main/resources/data/.../price_catalog/default.json` | 底价 `minecraft:deepslate: 1` |
| `src/unitTest/.../market/MarketBandRebaseTest.java` | 断言目录含 deepslate |
| `版本改动.md` | 1.1.0 新增内容中英一句 |
| `下一步的计划-建材店深板岩.md` | 验收勾选 |

---

### Task 1: 上架 deepslate + 测试 + 文档

**Files:**
- Modify: `src/main/java/com/homestead/sellveg/market/PriceCatalog.java`（`buildShopExclusiveSet`，在 `stone` 旁加 deepslate）
- Modify: `src/main/resources/data/homestead_sell_veg/price_catalog/default.json`（石类旁加 `"minecraft:deepslate": 1`）
- Modify: `src/unitTest/java/com/homestead/sellveg/market/MarketBandRebaseTest.java`
- Modify: `版本改动.md`、`下一步的计划-建材店深板岩.md`

**Interfaces:**
- Consumes: `BUILD_SHOP_EXCLUSIVE`、`putBuildShopBands` 廉价 else 分支、`CHEAP_BUILD_*`
- Produces: `deepslate` 建材专营 + listed band 1–3 + base price 1

- [x] **Step 1: Write the failing test**

在 `MarketBandRebaseTest` 增加：

```java
@Test
void deepslateHasCatalogBasePrice() throws Exception {
    String json = readDefaultCatalogJson();
    assertTrue(json.contains("\"minecraft:deepslate\""), "missing catalog price for deepslate");
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `gradlew.bat unitTest --tests com.homestead.sellveg.market.MarketBandRebaseTest.deepslateHasCatalogBasePrice`  
Expected: FAIL（missing catalog price）

- [x] **Step 3: Minimal implementation**

1. `buildShopExclusiveSet()`：`set.add(new ResourceLocation("minecraft:deepslate"));`（紧挨 stone）
2. `default.json` 石类段：`"minecraft:deepslate": 1,`
3. `版本改动.md` 1.1.0 新增内容追加一条中英
4. 勾选 `下一步的计划-建材店深板岩.md` 验收项

- [x] **Step 4: Run tests to verify pass**

Run: `gradlew.bat unitTest --tests com.homestead.sellveg.market.MarketBandRebaseTest`  
Expected: PASS

- [ ] **Step 5: Commit + 打 jar 到测试实例**

```bash
git add ... && git commit  # 说明：建材店专营上架深板岩（价同石类）
gradlew.bat build
# 覆盖 BMC4 mods/ 下 homestead_sell_veg jar
```
