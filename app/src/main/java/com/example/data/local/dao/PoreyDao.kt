package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CartItemEntity
import com.example.data.local.entity.DeliveryZoneEntity
import com.example.data.local.entity.FinancialLedgerEntity
import com.example.data.local.entity.FulfilmentOrderEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.MerchantListingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.PosWebhookEventEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ReturnRequestEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.SavedAddressEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoreyDao {
    // --- OBSERVE STREAMS ---
    @Query("SELECT * FROM user_accounts")
    fun observeUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM merchants ORDER BY distanceKm ASC")
    fun observeMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM products")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM merchant_listings")
    fun observeListings(): Flow<List<MerchantListingEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT 100")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM pos_webhook_events ORDER BY processedAt DESC LIMIT 50")
    fun observePosWebhookEvents(): Flow<List<PosWebhookEventEntity>>

    @Query("SELECT * FROM cart_items WHERE customerId = :customerId ORDER BY addedAt DESC")
    fun observeCartItems(customerId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM fulfilment_orders ORDER BY updatedAt DESC")
    fun observeFulfilments(): Flow<List<FulfilmentOrderEntity>>

    @Query("SELECT * FROM order_items")
    fun observeOrderItems(): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM financial_ledger ORDER BY createdAt DESC LIMIT 120")
    fun observeFinancialLedger(): Flow<List<FinancialLedgerEntity>>

    @Query("SELECT * FROM return_requests ORDER BY updatedAt DESC")
    fun observeReturnRequests(): Flow<List<ReturnRequestEntity>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun observeReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM delivery_zones")
    fun observeDeliveryZones(): Flow<List<DeliveryZoneEntity>>

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 80")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 80")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM wishlist_items WHERE customerId = :customerId")
    fun observeWishlist(customerId: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM saved_addresses WHERE customerId = :customerId ORDER BY isDefault DESC")
    fun observeSavedAddresses(customerId: String): Flow<List<SavedAddressEntity>>

    // --- SYNCHRONOUS / SUSPEND LOOKUPS FOR TRANSACTIONS ---
    @Query("SELECT COUNT(*) FROM merchants")
    suspend fun getMerchantCount(): Int

    @Query("SELECT * FROM user_accounts WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:identifier) OR REPLACE(phone, ' ', '') = REPLACE(:identifier, ' ', '') LIMIT 1")
    suspend fun getUserByPhoneOrEmail(identifier: String): UserAccountEntity?

    @Query("SELECT * FROM merchant_listings WHERE id = :listingId")
    suspend fun getListingById(listingId: String): MerchantListingEntity?

    @Query("SELECT * FROM merchant_listings WHERE merchantId = :merchantId AND sku = :sku LIMIT 1")
    suspend fun getListingByMerchantSku(merchantId: String, sku: String): MerchantListingEntity?

    @Query("SELECT * FROM merchants WHERE id = :merchantId")
    suspend fun getMerchantById(merchantId: String): MerchantEntity?

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?

    @Query("SELECT * FROM cart_items WHERE customerId = :customerId")
    suspend fun getCartItemsForCustomer(customerId: String): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE customerId = :customerId AND listingId = :listingId LIMIT 1")
    suspend fun getCartItemByListing(customerId: String, listingId: String): CartItemEntity?

    @Query("SELECT * FROM pos_webhook_events WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getWebhookByIdempotencyKey(key: String): PosWebhookEventEntity?

    @Query("SELECT * FROM fulfilment_orders WHERE id = :fulfilmentId")
    suspend fun getFulfilmentById(fulfilmentId: String): FulfilmentOrderEntity?

    @Query("SELECT * FROM order_items WHERE fulfilmentOrderId = :fulfilmentId")
    suspend fun getOrderItemsByFulfilment(fulfilmentId: String): List<OrderItemEntity>

    @Query("SELECT * FROM return_requests WHERE id = :returnId")
    suspend fun getReturnRequestById(returnId: String): ReturnRequestEntity?

    @Query("SELECT * FROM delivery_zones WHERE id = :zoneId")
    suspend fun getDeliveryZoneById(zoneId: String): DeliveryZoneEntity?

    @Query("SELECT * FROM wishlist_items WHERE customerId = :customerId AND productId = :productId LIMIT 1")
    suspend fun getWishlistItem(customerId: String, productId: String): WishlistItemEntity?

    // --- INSERTS & UPDATES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(items: List<UserAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(item: UserAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchants(items: List<MerchantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(item: MerchantEntity)

    @Update
    suspend fun updateMerchant(item: MerchantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(item: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(items: List<MerchantListingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(item: MerchantListingEntity)

    @Update
    suspend fun updateListing(item: MerchantListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(item: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosWebhookEvent(item: PosWebhookEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :cartItemId")
    suspend fun deleteCartItem(cartItemId: String)

    @Query("DELETE FROM cart_items WHERE customerId = :customerId")
    suspend fun clearCartForCustomer(customerId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(item: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFulfilmentOrder(item: FulfilmentOrderEntity)

    @Update
    suspend fun updateFulfilmentOrder(item: FulfilmentOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialLedger(item: FinancialLedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnRequest(item: ReturnRequestEntity)

    @Update
    suspend fun updateReturnRequest(item: ReturnRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(item: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryZones(items: List<DeliveryZoneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryZone(item: DeliveryZoneEntity)

    @Update
    suspend fun updateDeliveryZone(item: DeliveryZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(item: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE targetRole = :role")
    suspend fun markNotificationsRead(role: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(item: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE customerId = :customerId AND productId = :productId")
    suspend fun removeWishlistItem(customerId: String, productId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAddress(item: SavedAddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAddresses(items: List<SavedAddressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(items: List<ReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(items: List<StockMovementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(items: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(items: List<AuditLogEntity>)
}
