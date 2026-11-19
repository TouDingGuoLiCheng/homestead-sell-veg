# 装备目录耐久与附魔展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** WEAPON/ARMOR 商品数量锁 1；格子显示 ✦ 与耐久%；悬停原版 tooltip；详情页左侧星级下展示装备信息；挂单/库存同步保留 NBT。

**Architecture:** `PriceCatalog.isGearItem(Item)` 统一判定。`ShopSyncPacket` 挂单与库存改传完整 `ItemStack`。`NetShopScreen` 负责格子装饰、悬停、详情布局与数量 UI。服务端 list/buy/cart 对装备 `count` 夹紧为 1。

**Tech Stack:** MC 1.20.1 Forge 47；现有 NetShop / StockIndex / ListingService。

## Global Constraints

- 分支 `exp/sellveg`；不改 `mod_version`
- 规格见根目录 `下一步的计划-装备目录耐久附魔.md`
- 收尾写 `版本改动.md`；无 `docs/规划/`

---

### Task 1: Gear 判定 + 数量夹紧纯函数

**Files:**
- Modify: `src/main/java/com/homestead/sellveg/market/PriceCatalog.java` — 增加 `isGearItem(Item)`
- Create: `src/main/java/com/homestead/sellveg/shop/GearTradeRules.java` — `clampCount(Item, int)` / `isGear(ItemStack)`
- Test: `src/unitTest/java/com/homestead/sellveg/shop/GearTradeRulesTest.java`

**Interfaces:**
- Produces: `PriceCatalog.isGearItem(Item item): boolean`；`GearTradeRules.clampBuyOrListCount(Item item, int count): int`（装备→1，其它→现有 sanitize）

- [ ] **Step 1:** 写单测：铁剑 clamp→1；小麦保持原 count（≤64）
- [ ] **Step 2:** 实现 `isGearItem`（shelfKind WEAPON/ARMOR）与 `GearTradeRules`
- [ ] **Step 3:** 跑单测通过后 commit

---

### Task 2: 服务端夹紧 + 同步 NBT

**Files:**
- Modify: `ShopSyncPacket.java` — ListingRow/StockRow 编解码 `writeItem`/`readItem`；StockRow 改为 `ItemStack template`
- Modify: `ListingService.java` / `CheckoutService.java` / `CartData.java`（或 `ShopActionPacket` 处理处）— 装备 count 夹 1
- Modify: `NetShopScreen.java` — 适配 `StockRow.template()`，上架用 template

**Interfaces:**
- Consumes: `GearTradeRules.clampBuyOrListCount`
- Produces: 客户端 `StockRow`/`ListingRow` 带完整 NBT

- [ ] **Step 1:** 改 `StockRow(ItemStack template, int count, …)`，build/encode/decode 对齐
- [ ] **Step 2:** Listing 同步改 `buf.writeItem(stack)` / `buf.readItem()`
- [ ] **Step 3:** list/buyNow/cartAdd/cartSet 入口夹紧
- [ ] **Step 4:** 客户端上架 `listPick` 存 ItemStack；commit

---

### Task 3: UI — 格子、悬停、详情、数量控件

**Files:**
- Modify: `NetShopScreen.java`
- Modify: `zh_cn.json` / `en_us.json`

**Interfaces:**
- Consumes: gear 判定、完整 stack

- [ ] **Step 1:** 商城/库存/挂单格：✦ + `xx%`；`renderTooltip(stack)` 悬停
- [ ] **Step 2:** 详情左侧星级下画耐久+附魔；右侧去掉附魔块
- [ ] **Step 3:** 装备详情/上架隐藏 +1/+16，数量框只读或固定 1
- [ ] **Step 4:** lang 键；commit

---

### Task 4: 版本改动 + 勾选计划验收

**Files:**
- Modify: `版本改动.md`
- Modify: `下一步的计划-装备目录耐久附魔.md` 勾选阶段

- [ ] **Step 1:** 写入新增内容中英结果句
- [ ] **Step 2:** 勾选计划 A–D；commit
