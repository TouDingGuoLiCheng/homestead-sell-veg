# 物流在途包裹列表 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 物流页用类似店铺上架行的状态条展示每一笔在途包裹，收货点改为下拉选择。

**Architecture:** `ShopSyncPacket` 同步每笔 `Shipment` 摘要行；客户端物流 Tab 顶部下拉选默认收货点，下方可滚动渲染在途行；ETA 按剩余 tick 换算分钟，打开 GUI 后用游戏时间做本地倒计时。

**Tech Stack:** Forge 1.20.1、现有 `NetShopScreen` / `TextButton` / `ShopSyncPacket`

## Global Constraints

- 不升 `mod_version`（当前 `1.0.0`）；改动写入 `版本改动.md` 的 `## 1.0.0`
- 一包裹一行；纯文字状态；收货点在上（下拉）、在途在下
- 中英语言键齐全

## 你问-我回答

- 粒度？→ 按包裹一行  
- 布局？→ 收货点在上、在途在下；收货点改下拉不摊开  
- 状态展示？→ 纯文字（如「配送中 · 约 3 分钟」）  
- 方向确认？→ 批准并直接做完

---

### Task 1: Shipment 摘要工具 + 单测

**Files:**
- Create: `src/main/java/com/homestead/sellveg/order/ShipmentSummary.java`
- Create: `src/unitTest/java/com/homestead/sellveg/order/ShipmentSummaryTest.java`

**Produces:** `primaryStack(contents)`, `kindCount(contents)`, `etaMinutes(remainingTicks)`, 供 packet/UI 使用

- [x] **Step 1:** 实现 `ShipmentSummary`（首个非空栈、种类数、分钟向上取整至少 1）
- [x] **Step 2:** 单测覆盖空箱、单品、多品、0 tick、1200 tick

---

### Task 2: ShopSyncPacket 同步 shipments

**Files:**
- Modify: `ShopSyncPacket.java`

**Produces:** `List<ShipmentRow> shipments`；`ShipmentRow(UUID id, ItemStack primary, int kindCount, int remainingTicks, boolean blocked)`

- [x] **Step 1:** encode/decode/build 填充 shipments（空 primary 用 air×0）
- [x] **Step 2:** 保留 `shipmentCount` / `shipmentBlocked` 兼容汇总行

---

### Task 3: 客户端状态本地 ETA

**Files:**
- Modify: `ShopClientState.java`

- [x] **Step 1:** 记录 `syncGameTime`；`displayRemaining(ShipmentRow)` = max(0, remaining - elapsed)

---

### Task 4: 物流 Tab UI

**Files:**
- Modify: `NetShopScreen.java`
- Modify: `zh_cn.json` / `en_us.json`

- [x] **Step 1:** 收货点下拉（合拢一行，展开浮层点选 `setDefault`）
- [x] **Step 2:** 在途列表（图标 + 名称数量摘要 + 状态文案），可滚轮滚动
- [x] **Step 3:** 无在途空态文案

---

### Task 5: 送达后刷新 GUI（若仍开着）

**Files:**
- Modify: `DeliveryService.java`

- [x] **Step 1:** 包裹移除或新进入 blocked 通知后 `ShopSyncPacket.send(player, false)`（勿每 tick 广播）

---

### Task 6: 收尾

- [x] **Step 1:** 追加 `版本改动.md`
- [x] **Step 2:** 跑相关 unitTest

## 验收

- [ ] 多包裹时物流页能看到每一单名称/数量/状态/预计送达
- [ ] 收货点为下拉，不再占满整页列表
- [ ] 打开界面后分钟数会随时间减少；送达后列表更新
