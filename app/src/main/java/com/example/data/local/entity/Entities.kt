package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val assignedMerchantId: String? = null,
    val defaultAddress: String,
    val defaultZone: String,
    val passwordHash: String = "porey123",
    val isActive: Boolean = true
)

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val storeType: String,
    val area: String,
    val address: String,
    val distanceKm: Double,
    val rating: Double,
    val fulfilmentReliability: Double,
    val avgPrepMinutes: Int,
    val deliveryChargeBdt: Int,
    val posProvider: String?,
    val posStatus: String,
    val commissionRatePct: Int,
    val isVerified: Boolean,
    val isSuspended: Boolean = false,
    val isOpenNow: Boolean = true,
    val ownerUserId: String? = null,
    val tradeLicenseRef: String? = null,
    val lastSyncedAt: Long
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val subCategory: String,
    val fabric: String,
    val description: String,
    val careInstructions: String,
    val returnPolicy: String,
    val imageKey: String,
    val isNewArrival: Boolean,
    val isTrending: Boolean,
    val isBestSeller: Boolean,
    val rating: Double,
    val reviewCount: Int
)

@Entity(
    tableName = "merchant_listings",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId"),
        Index("merchantId"),
        Index(value = ["merchantId", "sku"], unique = true)
    ]
)
data class MerchantListingEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val merchantId: String,
    val sku: String,
    val size: String,
    val color: String,
    val colorHex: Long,
    val priceBdt: Int,
    val discountPriceBdt: Int? = null,
    val physicalQty: Int,
    val reservedQty: Int = 0,
    val damagedQty: Int = 0,
    val inventorySource: String,
    val stockConfidence: String,
    val isPublished: Boolean = true,
    val lastUpdatedAt: Long
)

@Entity(
    tableName = "stock_movements",
    indices = [Index("listingId"), Index("merchantId"), Index("createdAt")]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val listingId: String,
    val merchantId: String,
    val sku: String,
    val deltaPhysical: Int,
    val deltaReserved: Int,
    val deltaDamaged: Int,
    val reason: String,
    val actorRole: String,
    val note: String,
    val idempotencyKey: String? = null,
    val createdAt: Long
)

@Entity(
    tableName = "pos_webhook_events",
    indices = [Index("merchantId"), Index(value = ["idempotencyKey"], unique = true)]
)
data class PosWebhookEventEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val providerName: String,
    val idempotencyKey: String,
    val sku: String,
    val qtySold: Int,
    val signatureVerified: Boolean,
    val status: String,
    val errorMessage: String? = null,
    val eventTimestamp: Long,
    val processedAt: Long
)

@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = MerchantListingEntity::class,
            parentColumns = ["id"],
            childColumns = ["listingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId"), Index("listingId")]
)
data class CartItemEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val listingId: String,
    val quantity: Int,
    val addedAt: Long
)

@Entity(
    tableName = "orders",
    indices = [Index("customerId"), Index("createdAt")]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderCode: String,
    val customerId: String,
    val customerName: String,
    val deliveryAddress: String,
    val deliveryZone: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val subtotalBdt: Int,
    val deliveryTotalBdt: Int,
    val discountBdt: Int,
    val grandTotalBdt: Int,
    val promoCode: String? = null,
    val createdAt: Long
)

@Entity(
    tableName = "fulfilment_orders",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId"), Index("merchantId"), Index("riderId")]
)
data class FulfilmentOrderEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val merchantId: String,
    val merchantName: String,
    val riderId: String? = null,
    val riderName: String? = null,
    val status: String,
    val subtotalBdt: Int,
    val deliveryFeeBdt: Int,
    val commissionBdt: Int,
    val merchantPayableBdt: Int,
    val estimatedMinutes: Int,
    val isLocationSharingActive: Boolean = false,
    val riderLat: Double? = null,
    val riderLng: Double? = null,
    val proofOfPickupNote: String? = null,
    val proofOfDeliveryNote: String? = null,
    val updatedAt: Long
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = FulfilmentOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["fulfilmentOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fulfilmentOrderId"), Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val fulfilmentOrderId: String,
    val orderId: String,
    val listingId: String,
    val productName: String,
    val brand: String,
    val size: String,
    val color: String,
    val unitPriceBdt: Int,
    val quantity: Int
)

@Entity(
    tableName = "financial_ledger",
    indices = [Index("merchantId"), Index("orderId"), Index("createdAt")]
)
data class FinancialLedgerEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val fulfilmentOrderId: String,
    val merchantId: String,
    val entryType: String,
    val amountBdt: Int,
    val status: String,
    val note: String,
    val createdAt: Long
)

@Entity(
    tableName = "return_requests",
    indices = [Index("customerId"), Index("merchantId"), Index("fulfilmentOrderId")]
)
data class ReturnRequestEntity(
    @PrimaryKey val id: String,
    val fulfilmentOrderId: String,
    val orderId: String,
    val customerId: String,
    val merchantId: String,
    val requestType: String,
    val reason: String,
    val status: String,
    val resolutionNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "reviews",
    indices = [Index("productId"), Index("merchantId")]
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val merchantId: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val verifiedPurchase: Boolean,
    val createdAt: Long
)

@Entity(tableName = "delivery_zones")
data class DeliveryZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val areas: String,
    val baseChargeBdt: Int,
    val expressAvailable: Boolean,
    val maxDistanceKm: Double,
    val estimatedWindow: String,
    val isActive: Boolean
)

@Entity(
    tableName = "notifications",
    indices = [Index("targetRole"), Index("createdAt")]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val targetRole: String,
    val targetUserId: String? = null,
    val title: String,
    val message: String,
    val category: String,
    val isRead: Boolean = false,
    val createdAt: Long
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("createdAt")]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorRole: String,
    val actorName: String,
    val action: String,
    val targetEntity: String,
    val details: String,
    val createdAt: Long
)

@Entity(tableName = "wishlist_items")
data class WishlistItemEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val productId: String,
    val addedAt: Long
)

@Entity(tableName = "saved_addresses")
data class SavedAddressEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val label: String,
    val addressLine: String,
    val zoneName: String,
    val phone: String,
    val isDefault: Boolean
)
