# Subscription products — Collage Maker: Pic Editor (KMP)

Every premium screen, the store product ID it sells, how its price text is built, and how to
create the products in Google Play Console and App Store Connect.

The product IDs live in `shared/src/commonMain/kotlin/org/example/project/data/billing/BillingProducts.kt`
(`BillingProductIds`). They must match the store **character for character**. If an ID is wrong,
its price never loads.

---

## 1. Product keys

All products are **auto-renewing subscriptions**.

| Screen | Product ID | Code name | Period | What to set up |
|---|---|---|---|---|
| Main Premium + Shop › Premium | `item_1week` | `SubscriptionPlan.Weekly` | 1 week | Weekly price. Optional free trial. |
| Main Premium + Shop › Premium | `item_1m` | `SubscriptionPlan.Monthly` | 1 month | Monthly price. **The offer screen shows this price struck through.** |
| Offer screen (startup `offer` step) | `item_1m_offer` | `SubscriptionPlan.MonthlyOffer` | 1 month | The discounted offer: a lower monthly price and/or an **intro price** (e.g. 3 days at a low price). |
| Pro offer screen (premium banner on a premium item) | `item_1week_pro` | `SubscriptionPlan.WeeklyPro` | 1 week | Weekly price. Optional **free trial** and/or **intro price**. |
| Premium pack sheet: Stickers | `stickers_pack_weekly` | `SubscriptionPlan.StickersPack` | 1 week | Unlocks only stickers. |
| Premium pack sheet: Fonts | `fonts_pack_weekly` | `SubscriptionPlan.FontsPack` | 1 week | Unlocks only fonts. |
| Premium pack sheet: Backgrounds | `backgrounds_pack_weekly` | `SubscriptionPlan.BackgroundsPack` | 1 week | Unlocks only backgrounds. |
| Premium pack sheet: Frames | `frames_pack_weekly` | `SubscriptionPlan.FramesPack` | 1 week | Unlocks only frames. |
| Premium pack sheet: Templates | `templates_pack_weekly` | `SubscriptionPlan.TemplatesPack` | 1 week | Unlocks only templates. |
| (no screen sells it at the moment) | `yearly_subscription` | `SubscriptionPlan.Yearly` | 1 year | Optional. Kept so existing yearly subscribers stay premium. |

**Full plans vs pack plans**
- **Full plans** unlock all premium content: Weekly, Monthly, MonthlyOffer, WeeklyPro and Yearly.
- **Pack plans** unlock only their own category.

**Fallback while a key isn't live yet**
- The offer screen sells `item_1m` instead of `item_1m_offer`.
- The pro offer screen sells `item_1week` instead of `item_1week_pro`.
- So neither screen ever shows an empty plan.

**What "CURRENT" shows**
- A user subscribed through `item_1m_offer` sees **Monthly** marked "CURRENT" on the main paywall.
- A user subscribed through `item_1week_pro` sees **Weekly** marked "CURRENT".

---

## 2. Screens and how their text is built

All prices, periods, trials and intro prices come from the store, already localized for the user.
Nothing is hard-coded. A trial or intro price only appears when the store says this user is
eligible.

### 2.1 Main Premium screen (startup flow, in-app, Shop › Premium)
- **Plans:** Weekly (`item_1week`) and Monthly (`item_1m`).
- **"SAVE x%" on Monthly:** compares Monthly with a month of Weekly (52 weeks ÷ 12).
- **Free trial:** if a plan has one, its button reads "Start Free Trial".

### 2.2 Offer screen (`LaunchOfferScreen`, the startup `offer` step)
- **Sells:** `item_1m_offer`.
- **Countdown:** a 2-hour limited-time window that starts the first time the screen is shown.
  - The screen closes itself once the window has expired.
  - It also closes itself for a premium user.

| Element | Source |
|---|---|
| Struck-through price | `item_1m`'s recurring price, e.g. ~~Rs 820.00/month~~ (shown only when there is a saving) |
| Big offer line, with an intro price charged once | "Only Rs 190.71 for 3 days" |
| Big offer line, with a repeating intro price | "Only Rs 50/week for 4 weeks" |
| Big offer line, with no intro price | "Only Rs 410 per month" |
| "95 % OFF" | `1 − (offer's first charge ÷ item_1m price)`, rounded **down** |
| Small print, free trial | "Subscription is billed after the trial ends and will auto-renew." |
| Small print, intro price | "Then Rs 820/month. Renews automatically until you cancel." |
| Small print, otherwise | "Billed monthly. Renews automatically until you cancel." |

The **offer's first charge** is the intro price if there is one, otherwise the offer's monthly price.

**Discount examples** (`item_1m` = Rs 820):

| `item_1m_offer` set up as | Shown |
|---|---|
| Intro Rs 41 for 3 days | 95 % OFF |
| Intro Rs 190.71 for 3 days | 76 % OFF |
| Rs 410/month, no intro | 50 % OFF |
| Same price as `item_1m`, or a different currency | "BEST VALUE" (no %) |

To show **95 % OFF**, the offer's first charge must be at most 5 % of the `item_1m` price.

### 2.3 Pro offer screen (`ProOfferScreen`, opened from the premium banner)
- **Sells:** `item_1week_pro`.

| Store setup | Headline | Line under it | Button |
|---|---|---|---|
| Free trial | "3-Day Free Trial" | "Then Rs 300/week will be charged." | "Try for Rs 0" |
| Free trial, then intro price | "3-Day Free Trial" | "Then Rs 50 for 1 week, then Rs 300/week." | "Try for Rs 0" |
| Intro price only | "Only Rs 50 for 1 week" | "Then Rs 300/week will be charged." | "Continue" |
| Neither | "Rs 300/week" | "Auto Renewable. Cancel Anytime" | "Continue" |
| Already subscribed | price/period | — | "Current Plan" (disabled) |

### 2.4 Premium pack sheet
- **When it shows:** after a non-subscriber leaves the paywall for a premium item.
- **What it sells:** that category's `*_pack_weekly` plan.

---

## 3. Creating the products in Google Play Console

Go to **Monetize with Play › Products › Subscriptions › Create subscription**.

For each product ID in the table above:

1. **Product ID:** exactly as listed (e.g. `item_1m_offer`). It can't be changed later.
2. **Name:** e.g. "Monthly Offer". This is what users see in Play.
3. **Base plan:**
   - Click **Add base plan**, type **Auto-renewing**.
   - Billing period: 1 week or 1 month, as in the table.
   - Set the price for all countries (Play converts it, or set prices per country).
   - **Activate** the base plan.
4. **Trial or intro price (optional):** add an **offer** on that base plan.
   - **Eligibility:** *New customer acquisition* ("Never had this subscription", or "Never had any subscription").
   - **Phases:**
     - Free trial → a *Free trial* phase, e.g. 3 days.
     - Intro price charged once → a *Single payment* phase, e.g. Rs 190.71 for 3 days.
     - Intro price that repeats → a *Discounted recurring payment* phase, e.g. Rs 50/week for 4 weeks.
     - You can combine a free trial followed by a discounted phase.
   - **Activate** the offer.
5. **Activate** the subscription.

**Which offer the app shows:**
1. An eligible offer with a free trial.
2. Otherwise an eligible offer with an intro price.
3. Otherwise the plain base plan.

Play only lists offers the user is eligible for, so a used trial disappears on its own.

**Checklist for this release:**
- [ ] `item_1m_offer`: 1-month base plan, plus an optional intro-price offer.
- [ ] `item_1week_pro`: 1-week base plan, plus an optional free-trial and/or intro-price offer.
- [ ] Add license testers (**Settings › License testing**) to buy without being charged.

---

## 4. Creating the products in App Store Connect

Go to **App › Monetization › Subscriptions**.

1. **Subscription group:**
   - Put `item_1week`, `item_1m`, `item_1m_offer`, `item_1week_pro` (and `yearly_subscription`) in **the same group**, e.g. "Premium".
   - Then buying one replaces the other instead of charging the user twice.
   - Set the **levels** inside the group if you want upgrade/downgrade order. Longer or more expensive plans go at higher levels.
2. **Pack plans:** put each `*_pack_weekly` in **its own separate group**, away from the premium group.
   - Otherwise StoreKit treats buying a pack as switching away from the full plan.
3. **For each subscription:**
   - Click **Create**, then enter a **Reference name** and the **Product ID** exactly as listed.
   - Set the **Subscription duration** (1 week or 1 month).
   - Set **Subscription prices**.
   - Add a **localization** (display name and description).
   - Add a review screenshot.
4. **Introductory Offer (optional):** add it under **Subscription Prices › + › Create Introductory Offer**.
   - **Free** = free trial (e.g. 3 days).
   - **Pay as you go** = a discounted price per period for N periods (e.g. Rs 50/week for 4 weeks).
   - **Pay up front** = one price for the whole intro duration (e.g. Rs 190.71 for 3 days).
5. **Submit** the new subscriptions with the next app version. The first subscriptions in a group must be submitted together with an app build.

**Important (Apple rule):**
- A user can redeem only **one introductory offer per subscription group, ever**.
- Someone who already used the trial on `item_1week` won't get the intro price on `item_1m_offer` (same group).
- In that case the offer screen falls back to that product's plain monthly price, and the "% OFF" is calculated from that price instead.
- If you need the offer for already-trialed users, the alternative is **Promotional Offers**. These need a server-signed signature and are not implemented.

**Checklist for this release:**
- [ ] `item_1m_offer` in the Premium group, plus an optional Introductory Offer.
- [ ] `item_1week_pro` in the Premium group, plus an optional Introductory Offer (Free and/or Pay as you go / Pay up front).
- [ ] Test with a Sandbox account (**Users and Access › Sandbox**) or a StoreKit configuration file in Xcode.

---

## 5. Where this lives in the code

| What | File |
|---|---|
| Product IDs, plans, `variantOf` / `basePlan` | `shared/.../data/billing/BillingProducts.kt` |
| `SubscriptionProduct`, `IntroPrice`, `BillingState.productFor` (fallback) | `shared/.../data/billing/BillingModels.kt` |
| Play: offer choice, trial and intro phases | `shared/src/androidMain/.../data/billing/PlayBillingClientAdapter.kt` |
| StoreKit: trial and intro offer, eligibility | `iosApp/iosApp/StoreKitBridge.swift`, `shared/src/iosMain/.../data/billing/StoreKitBillingAdapter.kt` |
| Discount % and offer price line | `shared/.../ui/premium/PremiumMath.kt` (`offerDiscountPercent`, `offerPriceLine`) |
| Offer screen (`LaunchOfferPlan`) | `shared/.../ui/premium/LaunchOfferScreen.kt` |
| Pro offer screen (`ProOfferPlan`) | `shared/.../ui/premium/ProOfferScreen.kt` |
| Main paywall plans (`PremiumPlan`) | `shared/.../ui/premium/PremiumScreen.kt` |
| Tests | `shared/src/commonTest/.../ui/premium/PremiumMathTest.kt` |

**To change which product a screen sells:**
- Offer screen: change `LaunchOfferPlan`.
- Pro offer screen: change `ProOfferPlan`.
- New IDs go in `BillingProductIds` and `SubscriptionPlan`.
