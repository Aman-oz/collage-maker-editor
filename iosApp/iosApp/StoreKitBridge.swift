import Foundation
import StoreKit
import Shared

/// StoreKit 2 implementation of the shared Kotlin `StoreKitBridge`, which the iOS side of
/// `AppBillingWrapper` calls into. StoreKit 2 is Swift-only, so this half lives in the app target;
/// it is handed to `MainViewController(storeKitBridge:)` at launch.
final class StoreKitBridgeImpl: NSObject, StoreKitBridge {
    static let shared = StoreKitBridgeImpl()

    /// Products loaded by `fetchProducts`, reused by `purchase` so it needn't fetch again.
    private var products: [String: Product] = [:]
    private var updatesTask: Task<Void, Never>?

    func fetchProducts(productIds: [String], completion: @escaping ([StoreKitProduct]?, String?) -> Void) {
        Task { @MainActor in
            do {
                var result: [StoreKitProduct] = []
                for product in try await Product.products(for: productIds) {
                    products[product.id] = product
                    guard let subscription = product.subscription else { continue }
                    // The intro trial is offered once per subscription group, so check eligibility.
                    var trial: String?
                    if let intro = subscription.introductoryOffer, intro.paymentMode == .freeTrial,
                       await subscription.isEligibleForIntroOffer {
                        trial = Self.isoPeriod(intro.period)
                    }
                    result.append(
                        StoreKitProduct(
                            productId: product.id,
                            displayPrice: product.displayPrice,
                            priceMicros: NSDecimalNumber(decimal: product.price * 1_000_000).int64Value,
                            currencyCode: product.priceFormatStyle.currencyCode,
                            billingPeriod: Self.isoPeriod(subscription.subscriptionPeriod),
                            freeTrialPeriod: trial
                        )
                    )
                }
                completion(result, nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    func purchase(productId: String, completion: @escaping (StoreKitPurchaseOutcome, String?) -> Void) {
        Task { @MainActor in
            do {
                var product = products[productId]
                if product == nil {
                    product = try await Product.products(for: [productId]).first
                }
                guard let product else {
                    completion(.failed, "This plan isn't available right now")
                    return
                }
                switch try await product.purchase() {
                case .success(let verification):
                    switch verification {
                    case .verified(let transaction):
                        await transaction.finish()
                        completion(.purchased, nil)
                    case .unverified(_, let error):
                        completion(.failed, error.localizedDescription)
                    }
                case .pending:
                    completion(.pending, nil)
                case .userCancelled:
                    completion(.cancelled, nil)
                @unknown default:
                    completion(.failed, nil)
                }
            } catch {
                completion(.failed, error.localizedDescription)
            }
        }
    }

    func activeSubscriptions(completion: @escaping ([String]) -> Void) {
        Task { completion(await Self.activeProductIds()) }
    }

    func sync(completion: @escaping (String?) -> Void) {
        Task {
            do {
                try await AppStore.sync()
                completion(nil)
            } catch {
                completion(error.localizedDescription)
            }
        }
    }

    func observeEntitlements(onChange: @escaping ([String]) -> Void) {
        updatesTask?.cancel()
        // Renewals, expiries, refunds, Ask to Buy approvals and purchases from other devices all
        // arrive here; each must be finished or StoreKit keeps redelivering it.
        updatesTask = Task.detached {
            for await update in Transaction.updates {
                if case .verified(let transaction) = update {
                    await transaction.finish()
                }
                onChange(await Self.activeProductIds())
            }
        }
    }

    /// Verified, unrevoked auto-renewable subscriptions the user is entitled to right now.
    private static func activeProductIds() async -> [String] {
        var ids: [String] = []
        for await entitlement in Transaction.currentEntitlements {
            if case .verified(let transaction) = entitlement,
               transaction.productType == .autoRenewable,
               transaction.revocationDate == nil {
                ids.append(transaction.productID)
            }
        }
        return ids
    }

    /// StoreKit's unit + value as the ISO 8601 duration the shared code parses ("P1W", "P3D").
    private static func isoPeriod(_ period: Product.SubscriptionPeriod) -> String {
        let unit: String
        switch period.unit {
        case .day: unit = "D"
        case .week: unit = "W"
        case .month: unit = "M"
        case .year: unit = "Y"
        @unknown default: unit = "D"
        }
        return "P\(period.value)\(unit)"
    }
}
