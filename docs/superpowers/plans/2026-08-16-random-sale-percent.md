# 随机折扣百分比 Implementation Plan

> **For agentic workers:** Use inline execution in this session.

**Goal:** 每店每物随机 1–99% 偏浅折扣；商城显示划线原价+现价，右上绿底 `-xx%`，保留 `H`。

**Architecture:** `saleShops` 改为 `item → (shopId → percentOff)`；`SaleDiscountMath` 负责加权抽点与计价；`MallRow` 同步 `salePercent` + `baseBuyPrice`。

## Tasks

- [x] SaleDiscountMath + 单测
- [x] SaleRollMath.Listing 带 percent；MarketData 存读改
- [x] ShopOfferPricing / getShopBuyPrice
- [x] ShopSyncPacket + NetShopScreen UI
- [x] 版本改动.md
