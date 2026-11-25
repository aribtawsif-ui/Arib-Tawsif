package com.example.domain.model

import com.example.R
import com.example.data.local.entity.CartItemEntity
import com.example.data.local.entity.FulfilmentOrderEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.MerchantListingEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.ProductEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class UserRole(val labelEn: String, val labelBn: String, val badgeCode: String) {
    GUEST("Guest Browser", "অতিথি", "GUEST"),
    CUSTOMER("Customer", "ক্রেতা", "CUSTOMER"),
    PHYSICAL_MERCHANT("Physical Merchant (POS)", "দোকানদার (পিওএস)", "POS SHOP"),
    NO_POS_MERCHANT("Physical Shop (No POS)", "দোকান (ম্যানুয়াল)", "NO-POS"),
    ONLINE_MERCHANT("Online Brand", "অনলাইন ব্র্যান্ড", "ONLINE"),
    STORE_EMPLOYEE("Store Employee", "স্টোর কর্মী", "STAFF"),
    RIDER("Delivery Rider", "রাইডার", "RIDER"),
    ADMIN("Platform Admin", "অ্যাডমিন", "ADMIN"),
    SUPER_ADMIN("Super Admin", "সুপার অ্যাডমিন", "SUPER")
}

enum class ThemeMode(val label: String) {
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class StoreType(val label: String) {
    PHYSICAL_POS("Physical Store (POS)"),
    PHYSICAL_MANUAL("Physical Store (Manual)"),
    ONLINE_ONLY("Online-Only Brand")
}

enum class PosConnectionStatus(val label: String) {
    CONNECTED_SANDBOX("POS Webhook Ready (Sandbox)"),
    SYNC_DELAYED("Sync Delayed"),
    MANUAL_ONLY("Manual Stock Only"),
    DISCONNECTED("Connection Alert")
}

enum class InventorySource(val label: String) {
    POS_SYNC_SANDBOX("POS Webhook (Sandbox)"),
    MANUAL_UPDATE("Manually Updated")
}

enum class StockConfidence(val label: String) {
    LIVE_VERIFIED("Live POS Synced"),
    MANUAL_RECENT("Manually Updated · Recent"),
    NEEDS_RECONCILIATION("Verify Availability")
}

enum class StockMovementReason(val label: String) {
    POS_SALE("POS Physical Sale"),
    MANUAL_SALE("Manual Sale Deduction"),
    RESTOCK("Stock Restock"),
    DAMAGED("Damaged / Defective"),
    RETURN_RESTOCK("Return Restocked"),
    ORDER_RESERVE("Online Order Reserved"),
    ORDER_RELEASE("Reservation Released"),
    EOD_RECONCILE("End-of-Day Reconcile"),
    ORDER_FULFILLED("Order Fulfilled")
}

enum class PaymentMethod(val label: String, val subtitle: String, val isSandboxGateway: Boolean) {
    CASH_ON_DELIVERY("Cash on Delivery", "Pay cash to rider upon inspection", false),
    BKASH_SANDBOX("bKash (Sandbox Adapter)", "Tokenized gateway adapter — sandbox mode", true),
    NAGAD_SANDBOX("Nagad (Sandbox Adapter)", "MFS checkout adapter — sandbox mode", true),
    SSLCOMMERZ_SANDBOX("Card / SSLCommerz (Sandbox)", "Visa / Mastercard / Amex gateway adapter", true)
}

enum class PaymentStatus(val label: String) {
    COD_PENDING("COD Pending Collection"),
    SANDBOX_AUTHORIZED("Sandbox Authorized (Verify Webhook)"),
    FAILED("Payment Failed"),
    REFUNDED("Refunded")
}

enum class FulfilmentStatus(val label: String, val stepIndex: Int) {
    RESERVED_PENDING_MERCHANT("Reserved · Awaiting Merchant", 0),
    CONFIRMED_PREPARING("Merchant Confirmed · Preparing", 1),
    READY_FOR_PICKUP("Ready for Rider Pickup", 2),
    RIDER_ASSIGNED("Rider Assigned", 2),
    PICKED_UP("Picked Up from Store", 3),
    OUT_FOR_DELIVERY("Out for Delivery", 4),
    DELIVERED("Delivered & Verified", 5),
    CANCELLED("Cancelled", -1),
    FAILED_DELIVERY("Delivery Failed", -1)
}

enum class ReturnRequestType(val label: String) {
    RETURN("Return & Refund"),
    EXCHANGE("Size / Color Exchange")
}

enum class ReturnStatus(val label: String) {
    REQUESTED("Requested"),
    MERCHANT_APPROVED("Merchant Approved"),
    MERCHANT_REJECTED("Merchant Rejected"),
    DISPUTE_ADMIN_REVIEW("Admin Dispute Review"),
    REFUNDED_CLOSED("Resolved & Closed")
}

enum class SortOption(val label: String) {
    POPULAR("Most Popular"),
    PRICE_LOW("Price: Low to High"),
    PRICE_HIGH("Price: High to Low"),
    RATING("Top Rated"),
    NEWEST("Newest Arrivals")
}

data class MerchantOfferView(
    val listing: MerchantListingEntity,
    val merchant: MerchantEntity,
    val allocationScore: Double,
    val isRecommended: Boolean
) {
    val availableQty: Int
        get() = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)

    val effectivePriceBdt: Int
        get() = listing.discountPriceBdt ?: listing.priceBdt
}

data class CatalogProductView(
    val product: ProductEntity,
    val offers: List<MerchantOfferView>,
    val lowestPriceBdt: Int,
    val originalMaxPriceBdt: Int,
    val totalAvailableQty: Int,
    val nearestMerchantArea: String,
    val nearestDistanceKm: Double,
    val hasExpressOption: Boolean,
    val bestOffer: MerchantOfferView?
)

data class CartItemDetail(
    val cartItem: CartItemEntity,
    val listing: MerchantListingEntity,
    val product: ProductEntity,
    val merchant: MerchantEntity
) {
    val unitPriceBdt: Int get() = listing.discountPriceBdt ?: listing.priceBdt
    val lineTotalBdt: Int get() = unitPriceBdt * cartItem.quantity
    val availableQty: Int
        get() = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
}

data class CartMerchantGroup(
    val merchant: MerchantEntity,
    val items: List<CartItemDetail>,
    val subtotalBdt: Int,
    val deliveryChargeBdt: Int,
    val estimatedWindow: String
)

data class FulfilmentGroupDetail(
    val fulfilment: FulfilmentOrderEntity,
    val items: List<OrderItemEntity>,
    val merchant: MerchantEntity?
)

data class OrderDetailView(
    val order: OrderEntity,
    val fulfilmentGroups: List<FulfilmentGroupDetail>
)

data class CheckoutSummary(
    val groups: List<CartMerchantGroup>,
    val subtotalBdt: Int,
    val deliveryTotalBdt: Int,
    val discountBdt: Int,
    val grandTotalBdt: Int
)

fun formatBdt(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "BD"))
    return "৳${formatter.format(amount)}"
}

fun formatDhakaTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Not synced yet"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Dhaka")
    return "${sdf.format(Date(epochMillis))} BDT"
}

fun productDrawableRes(imageKey: String): Int = when (imageKey) {
    "panjabi" -> R.drawable.img_panjabi_artisan
    "saree" -> R.drawable.img_saree_atelier
    "streetwear" -> R.drawable.img_streetwear_dhaka
    else -> R.drawable.img_hero_dhaka
}
