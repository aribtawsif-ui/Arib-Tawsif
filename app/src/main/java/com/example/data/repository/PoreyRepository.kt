package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.PoreyDatabase
import com.example.data.local.SeedData
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
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.InventorySource
import com.example.domain.model.MerchantOfferView
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.PosConnectionStatus
import com.example.domain.model.ReturnRequestType
import com.example.domain.model.ReturnStatus
import com.example.domain.model.StockConfidence
import com.example.domain.model.StockMovementReason
import com.example.domain.model.StoreType
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.roundToInt

class PoreyRepository(private val db: PoreyDatabase) {
    private val dao = db.poreyDao()

    fun observeUsers(): Flow<List<UserAccountEntity>> = dao.observeUsers()
    fun observeMerchants(): Flow<List<MerchantEntity>> = dao.observeMerchants()
    fun observeProducts(): Flow<List<ProductEntity>> = dao.observeProducts()
    fun observeListings(): Flow<List<MerchantListingEntity>> = dao.observeListings()
    fun observeStockMovements(): Flow<List<StockMovementEntity>> = dao.observeStockMovements()
    fun observePosWebhookEvents(): Flow<List<PosWebhookEventEntity>> = dao.observePosWebhookEvents()
    fun observeCartItems(customerId: String): Flow<List<CartItemEntity>> = dao.observeCartItems(customerId)
    fun observeOrders(): Flow<List<OrderEntity>> = dao.observeOrders()
    fun observeFulfilments(): Flow<List<FulfilmentOrderEntity>> = dao.observeFulfilments()
    fun observeOrderItems(): Flow<List<OrderItemEntity>> = dao.observeOrderItems()
    fun observeFinancialLedger(): Flow<List<FinancialLedgerEntity>> = dao.observeFinancialLedger()
    fun observeReturnRequests(): Flow<List<ReturnRequestEntity>> = dao.observeReturnRequests()
    fun observeReviews(): Flow<List<ReviewEntity>> = dao.observeReviews()
    fun observeDeliveryZones(): Flow<List<DeliveryZoneEntity>> = dao.observeDeliveryZones()
    fun observeNotifications(): Flow<List<NotificationEntity>> = dao.observeNotifications()
    fun observeAuditLogs(): Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    fun observeWishlist(customerId: String): Flow<List<WishlistItemEntity>> = dao.observeWishlist(customerId)
    fun observeSavedAddresses(customerId: String): Flow<List<SavedAddressEntity>> = dao.observeSavedAddresses(customerId)

    suspend fun ensureSeeded() {
        if (dao.getMerchantCount() == 0) {
            db.withTransaction {
                if (dao.getMerchantCount() == 0) {
                    dao.insertUsers(SeedData.demoUsers)
                    dao.insertMerchants(SeedData.demoMerchants)
                    dao.insertProducts(SeedData.demoProducts)
                    dao.insertListings(SeedData.demoListings)
                    dao.insertDeliveryZones(SeedData.demoDeliveryZones)
                    dao.insertSavedAddresses(SeedData.demoAddresses)
                    dao.insertReviews(SeedData.demoReviews)
                    dao.insertStockMovements(SeedData.demoMovements)
                    dao.insertNotifications(SeedData.demoNotifications)
                    dao.insertAuditLogs(SeedData.demoAuditLogs)
                }
            }
        }
    }

    /**
     * Configurable merchant selection algorithm.
     * Weighs stock confidence, fulfilment reliability, verification, open status, distance, and fee.
     */
    fun scoreMerchantOffer(listing: MerchantListingEntity, merchant: MerchantEntity): Double {
        val available = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
        if (available <= 0 || !listing.isPublished || merchant.isSuspended) return -1.0

        val confidenceScore = when (listing.stockConfidence) {
            StockConfidence.LIVE_VERIFIED.name -> 35.0
            StockConfidence.MANUAL_RECENT.name -> 18.0
            else -> 5.0
        }
        val reliabilityScore = merchant.fulfilmentReliability * 30.0
        val trustBonus = (if (merchant.isVerified) 10.0 else 0.0) + (if (merchant.isOpenNow) 10.0 else 0.0)
        val distanceScore = (15.0 - merchant.distanceKm).coerceAtLeast(0.0) * 1.2
        val feeScore = (120 - merchant.deliveryChargeBdt).coerceAtLeast(0) * 0.1
        return confidenceScore + reliabilityScore + trustBonus + distanceScore + feeScore
    }

    fun buildRankedOffers(
        listings: List<MerchantListingEntity>,
        merchantsById: Map<String, MerchantEntity>
    ): List<MerchantOfferView> {
        val scored = listings.mapNotNull { listing ->
            val merchant = merchantsById[listing.merchantId] ?: return@mapNotNull null
            val score = scoreMerchantOffer(listing, merchant)
            listing to (merchant to score)
        }.sortedByDescending { it.second.second }

        val topScore = scored.firstOrNull { it.second.second > 0 }?.second?.second ?: -1.0
        return scored.map { (listing, pair) ->
            MerchantOfferView(
                listing = listing,
                merchant = pair.first,
                allocationScore = pair.second,
                isRecommended = pair.second > 0 && pair.second == topScore
            )
        }
    }

    // --- AUTHENTICATION & REGISTRATION ---
    private fun hashPassword(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun loginUser(identifier: String, password: String): Result<UserAccountEntity> {
        return runCatching {
            val cleanId = identifier.trim()
            require(cleanId.isNotBlank()) { "Please enter your email or phone number." }
            require(password.isNotBlank()) { "Please enter your password." }
            val user = dao.getUserByPhoneOrEmail(cleanId)
                ?: error("No Porey account found for '$cleanId'.")
            if (!user.isActive) error("Account is suspended. Contact Porey Admin.")
            val hashed = hashPassword(password)
            val passwordMatches = user.passwordHash == password || user.passwordHash == hashed
            require(passwordMatches) { "Incorrect password. (Demo accounts use 'porey123')" }
            user
        }
    }

    suspend fun registerCustomer(
        name: String,
        phone: String,
        email: String,
        password: String,
        address: String
    ): Result<UserAccountEntity> {
        return runCatching {
            require(name.trim().length >= 2) { "Full name is required." }
            require(phone.trim().length >= 6 || email.trim().contains("@")) {
                "Valid phone number or email is required."
            }
            require(password.length >= 4) { "Password must be at least 4 characters." }
            val cleanEmail = email.trim().ifBlank { "${phone.trim().filter { it.isDigit() }}@porey.bd" }
            if (dao.getUserByPhoneOrEmail(cleanEmail) != null || (phone.isNotBlank() && dao.getUserByPhoneOrEmail(phone.trim()) != null)) {
                error("An account with this phone or email already exists. Please sign in.")
            }
            val now = System.currentTimeMillis()
            val userId = "usr_cust_${UUID.randomUUID().toString().take(8)}"
            val newUser = UserAccountEntity(
                id = userId,
                name = name.trim(),
                phone = phone.trim().ifBlank { "+880 1700-000000" },
                email = cleanEmail,
                role = UserRole.CUSTOMER.name,
                assignedMerchantId = null,
                defaultAddress = address.trim().ifBlank { "Banani, Dhaka 1213" },
                defaultZone = "Dhaka Metro Express",
                passwordHash = hashPassword(password),
                isActive = true
            )
            db.withTransaction {
                dao.insertUser(newUser)
                dao.insertSavedAddress(
                    SavedAddressEntity(
                        id = "addr_${UUID.randomUUID().toString().take(8)}",
                        customerId = userId,
                        label = "Primary · Dhaka",
                        addressLine = newUser.defaultAddress,
                        zoneName = "Dhaka Metro Express",
                        phone = newUser.phone,
                        isDefault = true
                    )
                )
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = "aud_${UUID.randomUUID().toString().take(8)}",
                        actorRole = UserRole.CUSTOMER.name,
                        actorName = newUser.name,
                        action = "CUSTOMER_REGISTERED",
                        targetEntity = newUser.id,
                        details = "New customer registered (${newUser.email})",
                        createdAt = now
                    )
                )
            }
            newUser
        }
    }

    suspend fun registerShopOwner(
        ownerName: String,
        phone: String,
        email: String,
        password: String,
        shopName: String,
        area: String,
        address: String,
        storeType: StoreType,
        posProvider: String?,
        tradeLicenseRef: String
    ): Result<Pair<UserAccountEntity, MerchantEntity>> {
        return runCatching {
            require(ownerName.trim().length >= 2) { "Owner full name is required." }
            require(shopName.trim().length >= 2) { "Shop name is required." }
            require(area.isNotBlank() && address.isNotBlank()) { "Shop area and address are required." }
            require(tradeLicenseRef.trim().length >= 3) { "Trade License or NID/DBID reference is required for Admin verification." }
            require(password.length >= 4) { "Password must be at least 4 characters." }
            val cleanEmail = email.trim().ifBlank { "${phone.trim().filter { it.isDigit() }}@shop.porey.bd" }
            if (dao.getUserByPhoneOrEmail(cleanEmail) != null || (phone.isNotBlank() && dao.getUserByPhoneOrEmail(phone.trim()) != null)) {
                error("An account with this phone or email already exists.")
            }

            val now = System.currentTimeMillis()
            val userId = "usr_shop_${UUID.randomUUID().toString().take(8)}"
            val merchantId = "mer_${UUID.randomUUID().toString().take(8)}"
            val role = when (storeType) {
                StoreType.PHYSICAL_POS -> UserRole.PHYSICAL_MERCHANT
                StoreType.PHYSICAL_MANUAL -> UserRole.NO_POS_MERCHANT
                StoreType.ONLINE_ONLY -> UserRole.ONLINE_MERCHANT
            }

            val newUser = UserAccountEntity(
                id = userId,
                name = "${ownerName.trim()} (${shopName.trim()})",
                phone = phone.trim().ifBlank { "+880 1800-000000" },
                email = cleanEmail,
                role = role.name,
                assignedMerchantId = merchantId,
                defaultAddress = address.trim(),
                defaultZone = "Dhaka Metro Express",
                passwordHash = hashPassword(password),
                isActive = true
            )

            val newMerchant = MerchantEntity(
                id = merchantId,
                name = shopName.trim(),
                storeType = storeType.name,
                area = area.trim(),
                address = address.trim(),
                distanceKm = 3.2,
                rating = 5.0,
                fulfilmentReliability = 0.95,
                avgPrepMinutes = 25,
                deliveryChargeBdt = 65,
                posProvider = if (storeType == StoreType.PHYSICAL_POS) (posProvider?.ifBlank { "RetailBD Cloud POS" } ?: "RetailBD Cloud POS") else null,
                posStatus = if (storeType == StoreType.PHYSICAL_POS) PosConnectionStatus.CONNECTED_SANDBOX.name else PosConnectionStatus.MANUAL_ONLY.name,
                commissionRatePct = 10,
                isVerified = false, // Admin MUST verify before receiving Verified sign & publishing
                isSuspended = false,
                isOpenNow = true,
                ownerUserId = userId,
                tradeLicenseRef = tradeLicenseRef.trim().uppercase(),
                lastSyncedAt = now
            )

            db.withTransaction {
                dao.insertUser(newUser)
                dao.insertMerchant(newMerchant)
                dao.insertNotification(
                    NotificationEntity(
                        id = "not_${UUID.randomUUID().toString().take(8)}",
                        targetRole = UserRole.ADMIN.name,
                        title = "New Shop Pending Verification · ${newMerchant.name}",
                        message = "${ownerName.trim()} registered ${newMerchant.name} (${newMerchant.area}). Verify KYC (${newMerchant.tradeLicenseRef}) to grant Verified badge.",
                        category = "VERIFICATION",
                        createdAt = now
                    )
                )
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = "aud_${UUID.randomUUID().toString().take(8)}",
                        actorRole = role.name,
                        actorName = ownerName.trim(),
                        action = "SHOP_REGISTERED_PENDING_KYC",
                        targetEntity = newMerchant.name,
                        details = "Trade Ref: ${newMerchant.tradeLicenseRef} · Awaiting Admin verification",
                        createdAt = now
                    )
                )
            }
            newUser to newMerchant
        }
    }

    // --- CUSTOMER CART & WISHLIST ---
    suspend fun addToCart(
        customerId: String,
        listingId: String,
        qtyToAdd: Int = 1,
        isGuest: Boolean = false
    ): Result<String> {
        return runCatching {
            require(!isGuest) { "Please log in as a Customer to add items and buy." }
            val user = dao.getUserById(customerId)
            require(user == null || user.role != UserRole.GUEST.name) {
                "Guest accounts cannot purchase. Please log in as a Customer."
            }
            require(qtyToAdd > 0) { "Quantity must be positive." }
            db.withTransaction {
                val listing = dao.getListingById(listingId) ?: error("Listing not found.")
                val merchant = dao.getMerchantById(listing.merchantId) ?: error("Merchant not found.")
                if (merchant.isSuspended) error("Merchant is currently unavailable.")
                if (!listing.isPublished) error("This item is currently unpublished.")

                val available = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
                val existing = dao.getCartItemByListing(customerId, listingId)
                val newQty = (existing?.quantity ?: 0) + qtyToAdd
                if (newQty > available) {
                    error("Only $available unit(s) currently available from ${merchant.name}.")
                }
                dao.insertCartItem(
                    CartItemEntity(
                        id = existing?.id ?: "crt_${UUID.randomUUID().toString().take(8)}",
                        customerId = customerId,
                        listingId = listingId,
                        quantity = newQty,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
            "Added to cart"
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, customerId: String, delta: Int): Result<Unit> {
        return runCatching {
            db.withTransaction {
                val item = dao.getCartItemsForCustomer(customerId).firstOrNull { it.id == cartItemId }
                    ?: error("Cart item not found.")
                val targetQty = item.quantity + delta
                if (targetQty <= 0) {
                    dao.deleteCartItem(cartItemId)
                } else {
                    val listing = dao.getListingById(item.listingId) ?: error("Listing missing.")
                    val available = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
                    if (targetQty > available) {
                        error("Maximum $available unit(s) available right now.")
                    }
                    dao.updateCartItem(item.copy(quantity = targetQty))
                }
            }
        }
    }

    suspend fun removeCartItem(cartItemId: String) {
        dao.deleteCartItem(cartItemId)
    }

    suspend fun toggleWishlist(customerId: String, productId: String): Boolean {
        val existing = dao.getWishlistItem(customerId, productId)
        return if (existing != null) {
            dao.removeWishlistItem(customerId, productId)
            false
        } else {
            dao.insertWishlistItem(
                WishlistItemEntity(
                    id = "wsh_${UUID.randomUUID().toString().take(8)}",
                    customerId = customerId,
                    productId = productId,
                    addedAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun addSavedAddress(customerId: String, label: String, addressLine: String, zoneName: String, phone: String): Result<Unit> {
        return runCatching {
            require(label.isNotBlank() && addressLine.isNotBlank()) { "Label and address are required." }
            dao.insertSavedAddress(
                SavedAddressEntity(
                    id = "addr_${UUID.randomUUID().toString().take(8)}",
                    customerId = customerId,
                    label = label.trim(),
                    addressLine = addressLine.trim(),
                    zoneName = zoneName,
                    phone = phone.trim(),
                    isDefault = false
                )
            )
        }
    }

    suspend fun submitReview(
        productId: String,
        merchantId: String,
        customerName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        return runCatching {
            require(rating in 1..5) { "Rating must be 1 to 5 stars." }
            require(comment.trim().length >= 5) { "Please write at least 5 characters." }
            dao.insertReview(
                ReviewEntity(
                    id = "rev_${UUID.randomUUID().toString().take(8)}",
                    productId = productId,
                    merchantId = merchantId,
                    customerName = customerName,
                    rating = rating,
                    comment = comment.trim(),
                    verifiedPurchase = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // --- ATOMIC MULTI-MERCHANT CHECKOUT ---
    suspend fun placeOrder(
        customerId: String,
        customerName: String,
        deliveryAddress: String,
        zoneId: String,
        paymentMethod: PaymentMethod,
        promoCodeInput: String,
        isGuest: Boolean = false
    ): Result<String> {
        return runCatching {
            require(!isGuest) { "Customer login is required to buy products and reserve stock." }
            val user = dao.getUserById(customerId)
            require(user == null || user.role != UserRole.GUEST.name) {
                "Guest accounts cannot checkout. Please log in as a Customer."
            }
            require(deliveryAddress.isNotBlank()) { "Delivery address is required." }
            val now = System.currentTimeMillis()
            val promo = promoCodeInput.trim().uppercase()

            var createdOrderCode = ""
            db.withTransaction {
                val cartItems = dao.getCartItemsForCustomer(customerId)
                require(cartItems.isNotEmpty()) { "Your cart is empty." }

                val zone = dao.getDeliveryZoneById(zoneId)
                    ?: error("Selected delivery zone is unavailable.")
                if (!zone.isActive) error("Delivery zone ${zone.name} is paused by admin.")

                val resolvedRows = cartItems.map { cart ->
                    val listing = dao.getListingById(cart.listingId)
                        ?: error("Product variant no longer exists.")
                    val merchant = dao.getMerchantById(listing.merchantId)
                        ?: error("Merchant not found.")
                    val product = dao.getProductById(listing.productId)
                        ?: error("Product not found.")

                    if (merchant.isSuspended || !listing.isPublished) {
                        error("${product.name} from ${merchant.name} is currently unavailable.")
                    }
                    val available = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
                    if (available < cart.quantity) {
                        error("Stock conflict: Only $available unit(s) left for ${product.name} (${listing.size}) at ${merchant.name}.")
                    }
                    Triple(cart, listing, Pair(merchant, product))
                }

                // Atomically reserve inventory for every cart line
                resolvedRows.forEach { (cart, listing, _) ->
                    dao.updateListing(
                        listing.copy(
                            reservedQty = listing.reservedQty + cart.quantity,
                            lastUpdatedAt = now
                        )
                    )
                    dao.insertStockMovement(
                        StockMovementEntity(
                            id = "mov_${UUID.randomUUID().toString().take(8)}",
                            listingId = listing.id,
                            merchantId = listing.merchantId,
                            sku = listing.sku,
                            deltaPhysical = 0,
                            deltaReserved = cart.quantity,
                            deltaDamaged = 0,
                            reason = StockMovementReason.ORDER_RESERVE.name,
                            actorRole = UserRole.CUSTOMER.name,
                            note = "Reserved for online checkout",
                            createdAt = now
                        )
                    )
                }

                val orderId = "ord_${UUID.randomUUID().toString().take(8)}"
                createdOrderCode = "PRY-${(100000..999999).random()}"

                val groupedByMerchant = resolvedRows.groupBy { it.third.first.id }
                var orderSubtotal = 0
                var orderDeliveryTotal = 0

                val paymentStatus = when (paymentMethod) {
                    PaymentMethod.CASH_ON_DELIVERY -> PaymentStatus.COD_PENDING
                    else -> PaymentStatus.SANDBOX_AUTHORIZED
                }

                val fulfilmentPrepList = mutableListOf<Triple<FulfilmentOrderEntity, List<OrderItemEntity>, MerchantEntity>>()

                groupedByMerchant.forEach { (_, groupRows) ->
                    val merchant = groupRows.first().third.first
                    val groupSubtotal = groupRows.sumOf { (cart, listing, _) ->
                        (listing.discountPriceBdt ?: listing.priceBdt) * cart.quantity
                    }
                    val deliveryFee = maxOf(zone.baseChargeBdt, merchant.deliveryChargeBdt)
                    val commissionBdt = ((groupSubtotal * merchant.commissionRatePct) / 100.0).roundToInt()
                    val merchantPayable = (groupSubtotal - commissionBdt).coerceAtLeast(0)
                    val fulfilmentId = "ful_${UUID.randomUUID().toString().take(8)}"

                    val fulfilment = FulfilmentOrderEntity(
                        id = fulfilmentId,
                        orderId = orderId,
                        merchantId = merchant.id,
                        merchantName = merchant.name,
                        status = FulfilmentStatus.RESERVED_PENDING_MERCHANT.name,
                        subtotalBdt = groupSubtotal,
                        deliveryFeeBdt = deliveryFee,
                        commissionBdt = commissionBdt,
                        merchantPayableBdt = merchantPayable,
                        estimatedMinutes = merchant.avgPrepMinutes + if (zone.expressAvailable) 75 else 720,
                        updatedAt = now
                    )

                    val orderItems = groupRows.map { (cart, listing, pair) ->
                        val product = pair.second
                        OrderItemEntity(
                            id = "oi_${UUID.randomUUID().toString().take(8)}",
                            fulfilmentOrderId = fulfilmentId,
                            orderId = orderId,
                            listingId = listing.id,
                            productName = product.name,
                            brand = product.brand,
                            size = listing.size,
                            color = listing.color,
                            unitPriceBdt = listing.discountPriceBdt ?: listing.priceBdt,
                            quantity = cart.quantity
                        )
                    }

                    orderSubtotal += groupSubtotal
                    orderDeliveryTotal += deliveryFee
                    fulfilmentPrepList.add(Triple(fulfilment, orderItems, merchant))
                }

                val discountBdt = when (promo) {
                    "POREY10" -> (orderSubtotal * 0.10).roundToInt().coerceAtMost(600)
                    "DHAKA50" -> 50.coerceAtMost(orderSubtotal)
                    else -> 0
                }
                val grandTotal = (orderSubtotal - discountBdt + orderDeliveryTotal).coerceAtLeast(0)

                dao.insertOrder(
                    OrderEntity(
                        id = orderId,
                        orderCode = createdOrderCode,
                        customerId = customerId,
                        customerName = customerName,
                        deliveryAddress = deliveryAddress.trim(),
                        deliveryZone = zone.name,
                        paymentMethod = paymentMethod.name,
                        paymentStatus = paymentStatus.name,
                        subtotalBdt = orderSubtotal,
                        deliveryTotalBdt = orderDeliveryTotal,
                        discountBdt = discountBdt,
                        grandTotalBdt = grandTotal,
                        promoCode = promo.takeIf { discountBdt > 0 },
                        createdAt = now
                    )
                )

                fulfilmentPrepList.forEach { (fulfilment, items, merchant) ->
                    dao.insertFulfilmentOrder(fulfilment)
                    dao.insertOrderItems(items)

                    // Backend financial ledger entries
                    dao.insertFinancialLedger(
                        FinancialLedgerEntity(
                            id = "led_${UUID.randomUUID().toString().take(8)}",
                            orderId = orderId,
                            fulfilmentOrderId = fulfilment.id,
                            merchantId = merchant.id,
                            entryType = "COMMISSION_DEDUCTION",
                            amountBdt = fulfilment.commissionBdt,
                            status = "ACCRUED",
                            note = "${merchant.commissionRatePct}% platform commission on ${createdOrderCode}",
                            createdAt = now
                        )
                    )
                    dao.insertFinancialLedger(
                        FinancialLedgerEntity(
                            id = "led_${UUID.randomUUID().toString().take(8)}",
                            orderId = orderId,
                            fulfilmentOrderId = fulfilment.id,
                            merchantId = merchant.id,
                            entryType = "MERCHANT_PAYABLE",
                            amountBdt = fulfilment.merchantPayableBdt,
                            status = "PENDING_SETTLEMENT",
                            note = "Payable to ${merchant.name} after delivery",
                            createdAt = now
                        )
                    )
                    dao.insertNotification(
                        NotificationEntity(
                            id = "not_${UUID.randomUUID().toString().take(8)}",
                            targetRole = UserRole.PHYSICAL_MERCHANT.name,
                            title = "New Order $createdOrderCode",
                            message = "${items.sumOf { it.quantity }} item(s) reserved at ${merchant.name}. Confirm fulfilment.",
                            category = "ORDER",
                            createdAt = now
                        )
                    )
                }

                dao.insertNotification(
                    NotificationEntity(
                        id = "not_${UUID.randomUUID().toString().take(8)}",
                        targetRole = UserRole.CUSTOMER.name,
                        targetUserId = customerId,
                        title = "Order $createdOrderCode Placed",
                        message = "Stock reserved across ${fulfilmentPrepList.size} merchant group(s). Payment: ${paymentMethod.label}.",
                        category = "ORDER",
                        createdAt = now
                    )
                )

                dao.clearCartForCustomer(customerId)
            }
            createdOrderCode
        }
    }

    // --- POS INTEGRATION ADAPTER & WEBHOOK PROCESSOR ---
    suspend fun processPosSaleWebhook(
        actorRole: UserRole,
        merchantId: String,
        sku: String,
        qtySold: Int,
        idempotencyKey: String,
        signatureValid: Boolean
    ): Result<String> {
        return runCatching {
            requireInventoryWriteRole(actorRole)
            require(sku.isNotBlank()) { "SKU is required." }
            require(qtySold > 0) { "Quantity sold must be positive." }
            require(idempotencyKey.isNotBlank()) { "Idempotency key is required." }

            val now = System.currentTimeMillis()
            var resultMessage = ""

            db.withTransaction {
                val merchant = dao.getMerchantById(merchantId) ?: error("Merchant not found.")
                val provider = merchant.posProvider ?: "External POS"

                // 1. Verify webhook signature
                if (!signatureValid) {
                    dao.insertPosWebhookEvent(
                        PosWebhookEventEntity(
                            id = "wh_${UUID.randomUUID().toString().take(8)}",
                            merchantId = merchantId,
                            providerName = provider,
                            idempotencyKey = "${idempotencyKey}_sigfail_${now}",
                            sku = sku.trim(),
                            qtySold = qtySold,
                            signatureVerified = false,
                            status = "FAILED_SIGNATURE",
                            errorMessage = "Invalid HMAC-SHA256 webhook signature",
                            eventTimestamp = now,
                            processedAt = now
                        )
                    )
                    error("Webhook rejected: HMAC signature verification failed.")
                }

                // 2. Idempotency check
                val duplicate = dao.getWebhookByIdempotencyKey(idempotencyKey.trim())
                if (duplicate != null) {
                    resultMessage = "Duplicate webhook ignored safely (key=$idempotencyKey)."
                    return@withTransaction
                }

                // 3. SKU Mapping check
                val listing = dao.getListingByMerchantSku(merchantId, sku.trim())
                if (listing == null) {
                    dao.insertPosWebhookEvent(
                        PosWebhookEventEntity(
                            id = "wh_${UUID.randomUUID().toString().take(8)}",
                            merchantId = merchantId,
                            providerName = provider,
                            idempotencyKey = idempotencyKey.trim(),
                            sku = sku.trim(),
                            qtySold = qtySold,
                            signatureVerified = true,
                            status = "FAILED_UNMAPPED",
                            errorMessage = "Unmapped POS SKU '${sku.trim()}'",
                            eventTimestamp = now,
                            processedAt = now
                        )
                    )
                    dao.insertNotification(
                        NotificationEntity(
                            id = "not_${UUID.randomUUID().toString().take(8)}",
                            targetRole = UserRole.ADMIN.name,
                            title = "POS Mapping Failure · ${merchant.name}",
                            message = "Unmapped SKU ${sku.trim()} received from $provider.",
                            category = "INTEGRATION_ALERT",
                            createdAt = now
                        )
                    )
                    error("Unmapped SKU '${sku.trim()}'. Logged for merchant SKU mapping.")
                }

                val newPhysical = (listing.physicalQty - qtySold).coerceAtLeast(0)
                val actualDelta = newPhysical - listing.physicalQty
                dao.updateListing(
                    listing.copy(
                        physicalQty = newPhysical,
                        inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
                        stockConfidence = StockConfidence.LIVE_VERIFIED.name,
                        lastUpdatedAt = now
                    )
                )
                dao.updateMerchant(
                    merchant.copy(
                        posStatus = PosConnectionStatus.CONNECTED_SANDBOX.name,
                        lastSyncedAt = now
                    )
                )
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = "mov_${UUID.randomUUID().toString().take(8)}",
                        listingId = listing.id,
                        merchantId = merchantId,
                        sku = listing.sku,
                        deltaPhysical = actualDelta,
                        deltaReserved = 0,
                        deltaDamaged = 0,
                        reason = StockMovementReason.POS_SALE.name,
                        actorRole = actorRole.name,
                        note = "POS webhook sale ($provider)",
                        idempotencyKey = idempotencyKey.trim(),
                        createdAt = now
                    )
                )
                dao.insertPosWebhookEvent(
                    PosWebhookEventEntity(
                        id = "wh_${UUID.randomUUID().toString().take(8)}",
                        merchantId = merchantId,
                        providerName = provider,
                        idempotencyKey = idempotencyKey.trim(),
                        sku = listing.sku,
                        qtySold = qtySold,
                        signatureVerified = true,
                        status = "PROCESSED",
                        eventTimestamp = now,
                        processedAt = now
                    )
                )

                val remainingAvail = (newPhysical - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
                if (remainingAvail <= 2) {
                    dao.insertNotification(
                        NotificationEntity(
                            id = "not_${UUID.randomUUID().toString().take(8)}",
                            targetRole = UserRole.PHYSICAL_MERCHANT.name,
                            title = "Low Stock Alert · ${listing.sku}",
                            message = "Only $remainingAvail available after POS sale.",
                            category = "LOW_STOCK",
                            createdAt = now
                        )
                    )
                }
                resultMessage = "POS sale synced: ${listing.sku} now has $remainingAvail available."
            }
            resultMessage
        }
    }

    // --- MANUAL & NO-POS LIGHTWEIGHT STOCK MANAGEMENT ---
    suspend fun adjustManualStock(
        actorRole: UserRole,
        listingId: String,
        deltaPhysical: Int,
        deltaDamaged: Int,
        reason: StockMovementReason,
        note: String
    ): Result<String> {
        return runCatching {
            requireInventoryWriteRole(actorRole)
            val now = System.currentTimeMillis()
            var summary = ""
            db.withTransaction {
                val listing = dao.getListingById(listingId) ?: error("Listing not found.")
                val merchant = dao.getMerchantById(listing.merchantId) ?: error("Merchant not found.")

                val nextPhysical = (listing.physicalQty + deltaPhysical).coerceAtLeast(listing.reservedQty)
                val nextDamaged = (listing.damagedQty + deltaDamaged).coerceAtLeast(0)

                val confidence = if (merchant.storeType == StoreType.PHYSICAL_POS.name) {
                    StockConfidence.LIVE_VERIFIED.name
                } else {
                    StockConfidence.MANUAL_RECENT.name
                }

                dao.updateListing(
                    listing.copy(
                        physicalQty = nextPhysical,
                        damagedQty = nextDamaged,
                        inventorySource = InventorySource.MANUAL_UPDATE.name,
                        stockConfidence = confidence,
                        lastUpdatedAt = now
                    )
                )
                dao.updateMerchant(merchant.copy(lastSyncedAt = now))
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = "mov_${UUID.randomUUID().toString().take(8)}",
                        listingId = listing.id,
                        merchantId = listing.merchantId,
                        sku = listing.sku,
                        deltaPhysical = nextPhysical - listing.physicalQty,
                        deltaReserved = 0,
                        deltaDamaged = nextDamaged - listing.damagedQty,
                        reason = reason.name,
                        actorRole = actorRole.name,
                        note = note.ifBlank { reason.label },
                        createdAt = now
                    )
                )
                val avail = (nextPhysical - listing.reservedQty - nextDamaged).coerceAtLeast(0)
                summary = "${listing.sku}: $avail available (updated)"
            }
            summary
        }
    }

    suspend fun reconcileEndOfDayStock(
        actorRole: UserRole,
        listingId: String,
        verifiedPhysicalQty: Int
    ): Result<String> {
        return runCatching {
            requireInventoryWriteRole(actorRole)
            require(verifiedPhysicalQty >= 0) { "Count cannot be negative." }
            val now = System.currentTimeMillis()
            var message = ""
            db.withTransaction {
                val listing = dao.getListingById(listingId) ?: error("Listing not found.")
                val safePhysical = verifiedPhysicalQty.coerceAtLeast(listing.reservedQty)
                val diff = safePhysical - listing.physicalQty
                dao.updateListing(
                    listing.copy(
                        physicalQty = safePhysical,
                        stockConfidence = StockConfidence.MANUAL_RECENT.name,
                        lastUpdatedAt = now
                    )
                )
                val merchant = dao.getMerchantById(listing.merchantId)
                if (merchant != null) {
                    dao.updateMerchant(merchant.copy(lastSyncedAt = now))
                }
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = "mov_${UUID.randomUUID().toString().take(8)}",
                        listingId = listing.id,
                        merchantId = listing.merchantId,
                        sku = listing.sku,
                        deltaPhysical = diff,
                        deltaReserved = 0,
                        deltaDamaged = 0,
                        reason = StockMovementReason.EOD_RECONCILE.name,
                        actorRole = actorRole.name,
                        note = "End-of-day physical shelf count verified ($safePhysical)",
                        createdAt = now
                    )
                )
                message = "Reconciled ${listing.sku} to $safePhysical physical units."
            }
            message
        }
    }

    // --- ONLINE / MERCHANT PRODUCT STUDIO ---
    suspend fun createOnlineProductAndListing(
        actorRole: UserRole,
        merchantId: String,
        name: String,
        brand: String,
        category: String,
        fabric: String,
        size: String,
        color: String,
        sku: String,
        priceBdt: Int,
        discountPriceBdt: Int?,
        initialStock: Int,
        imageKey: String
    ): Result<String> {
        return runCatching {
            requireOwnerOrAdminForCatalog(actorRole)
            require(name.isNotBlank() && brand.isNotBlank() && sku.isNotBlank()) {
                "Product name, brand, and SKU are required."
            }
            require(priceBdt > 0 && initialStock >= 0) { "Price must be > 0 and stock >= 0." }
            val now = System.currentTimeMillis()

            db.withTransaction {
                val merchant = dao.getMerchantById(merchantId) ?: error("Merchant not found.")
                require(merchant.isVerified || actorRole == UserRole.ADMIN || actorRole == UserRole.SUPER_ADMIN) {
                    "Shop pending Admin verification. An Admin must verify '${merchant.name}' before publishing products."
                }
                val productId = "prd_${UUID.randomUUID().toString().take(8)}"
                val listingId = "lst_${UUID.randomUUID().toString().take(8)}"

                dao.insertProduct(
                    ProductEntity(
                        id = productId,
                        name = name.trim(),
                        brand = brand.trim(),
                        category = category,
                        subCategory = "Studio Release",
                        fabric = fabric.ifBlank { "100% Cotton" },
                        description = "Freshly listed by ${merchant.name} via Porey Merchant Studio.",
                        careInstructions = "Gentle wash cold.",
                        returnPolicy = "7-day return or size exchange.",
                        imageKey = imageKey,
                        isNewArrival = true,
                        isTrending = true,
                        isBestSeller = false,
                        rating = 5.0,
                        reviewCount = 0
                    )
                )
                dao.insertListing(
                    MerchantListingEntity(
                        id = listingId,
                        productId = productId,
                        merchantId = merchantId,
                        sku = sku.trim().uppercase(),
                        size = size,
                        color = color,
                        colorHex = 0xFF2E3A32,
                        priceBdt = priceBdt,
                        discountPriceBdt = discountPriceBdt?.takeIf { it in 1 until priceBdt },
                        physicalQty = initialStock,
                        reservedQty = 0,
                        damagedQty = 0,
                        inventorySource = InventorySource.MANUAL_UPDATE.name,
                        stockConfidence = if (merchant.storeType == StoreType.PHYSICAL_POS.name) {
                            StockConfidence.LIVE_VERIFIED.name
                        } else {
                            StockConfidence.MANUAL_RECENT.name
                        },
                        isPublished = true,
                        lastUpdatedAt = now
                    )
                )
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = "mov_${UUID.randomUUID().toString().take(8)}",
                        listingId = listingId,
                        merchantId = merchantId,
                        sku = sku.trim().uppercase(),
                        deltaPhysical = initialStock,
                        deltaReserved = 0,
                        deltaDamaged = 0,
                        reason = StockMovementReason.RESTOCK.name,
                        actorRole = actorRole.name,
                        note = "Initial listing creation",
                        createdAt = now
                    )
                )
            }
            "Published $name to Porey marketplace"
        }
    }

    suspend fun toggleListingPublished(actorRole: UserRole, listingId: String): Result<Unit> {
        return runCatching {
            requireOwnerOrAdminForCatalog(actorRole)
            val listing = dao.getListingById(listingId) ?: error("Listing not found.")
            val merchant = dao.getMerchantById(listing.merchantId) ?: error("Merchant not found.")
            if (!listing.isPublished) {
                require(merchant.isVerified || actorRole == UserRole.ADMIN || actorRole == UserRole.SUPER_ADMIN) {
                    "Shop must be verified by an Admin before listings go live."
                }
            }
            dao.updateListing(listing.copy(isPublished = !listing.isPublished, lastUpdatedAt = System.currentTimeMillis()))
        }
    }

    // --- FULFILMENT & RIDER OPERATIONS ---
    suspend fun advanceFulfilmentStatus(
        actorRole: UserRole,
        fulfilmentId: String,
        targetStatus: FulfilmentStatus,
        riderId: String? = null,
        riderName: String? = null,
        proofNote: String? = null
    ): Result<String> {
        return runCatching {
            val now = System.currentTimeMillis()
            db.withTransaction {
                val ful = dao.getFulfilmentById(fulfilmentId) ?: error("Fulfilment order not found.")
                when (targetStatus) {
                    FulfilmentStatus.CONFIRMED_PREPARING,
                    FulfilmentStatus.READY_FOR_PICKUP -> {
                        requireInventoryWriteRole(actorRole)
                    }
                    FulfilmentStatus.RIDER_ASSIGNED,
                    FulfilmentStatus.PICKED_UP,
                    FulfilmentStatus.OUT_FOR_DELIVERY,
                    FulfilmentStatus.DELIVERED,
                    FulfilmentStatus.FAILED_DELIVERY -> {
                        require(
                            actorRole in setOf(
                                UserRole.RIDER,
                                UserRole.ADMIN,
                                UserRole.SUPER_ADMIN,
                                UserRole.ONLINE_MERCHANT
                            )
                        ) { "Role ${actorRole.labelEn} cannot transition delivery state." }
                    }
                    FulfilmentStatus.CANCELLED -> {
                        require(actorRole != UserRole.RIDER && actorRole != UserRole.GUEST) {
                            "Only the customer, merchant, or admin can cancel orders."
                        }
                    }
                    else -> Unit
                }

                val items = dao.getOrderItemsByFulfilment(fulfilmentId)

                if (targetStatus == FulfilmentStatus.CANCELLED || targetStatus == FulfilmentStatus.FAILED_DELIVERY) {
                    // Release reserved stock back to available inventory
                    items.forEach { item ->
                        val listing = dao.getListingById(item.listingId)
                        if (listing != null) {
                            val releaseQty = item.quantity.coerceAtMost(listing.reservedQty)
                            dao.updateListing(
                                listing.copy(
                                    reservedQty = (listing.reservedQty - releaseQty).coerceAtLeast(0),
                                    lastUpdatedAt = now
                                )
                            )
                            dao.insertStockMovement(
                                StockMovementEntity(
                                    id = "mov_${UUID.randomUUID().toString().take(8)}",
                                    listingId = listing.id,
                                    merchantId = listing.merchantId,
                                    sku = listing.sku,
                                    deltaPhysical = 0,
                                    deltaReserved = -releaseQty,
                                    deltaDamaged = 0,
                                    reason = StockMovementReason.ORDER_RELEASE.name,
                                    actorRole = actorRole.name,
                                    note = "Released on ${targetStatus.label}",
                                    createdAt = now
                                )
                            )
                        }
                    }
                } else if (targetStatus == FulfilmentStatus.DELIVERED && ful.status != FulfilmentStatus.DELIVERED.name) {
                    // Commit physical deduction & clear reservation
                    items.forEach { item ->
                        val listing = dao.getListingById(item.listingId)
                        if (listing != null) {
                            val commitQty = item.quantity.coerceAtMost(listing.reservedQty)
                            dao.updateListing(
                                listing.copy(
                                    physicalQty = (listing.physicalQty - commitQty).coerceAtLeast(0),
                                    reservedQty = (listing.reservedQty - commitQty).coerceAtLeast(0),
                                    lastUpdatedAt = now
                                )
                            )
                            dao.insertStockMovement(
                                StockMovementEntity(
                                    id = "mov_${UUID.randomUUID().toString().take(8)}",
                                    listingId = listing.id,
                                    merchantId = listing.merchantId,
                                    sku = listing.sku,
                                    deltaPhysical = -commitQty,
                                    deltaReserved = -commitQty,
                                    deltaDamaged = 0,
                                    reason = StockMovementReason.ORDER_FULFILLED.name,
                                    actorRole = actorRole.name,
                                    note = "Delivered with proof: ${proofNote ?: "Verified"}",
                                    createdAt = now
                                )
                            )
                        }
                    }
                }

                val shareLocation = targetStatus == FulfilmentStatus.PICKED_UP ||
                    targetStatus == FulfilmentStatus.OUT_FOR_DELIVERY
                val lat = if (shareLocation) 23.7937 else null
                val lng = if (shareLocation) 90.4066 else null

                dao.updateFulfilmentOrder(
                    ful.copy(
                        status = targetStatus.name,
                        riderId = riderId ?: ful.riderId,
                        riderName = riderName ?: ful.riderName,
                        isLocationSharingActive = shareLocation,
                        riderLat = lat,
                        riderLng = lng,
                        proofOfPickupNote = if (targetStatus == FulfilmentStatus.PICKED_UP) (proofNote ?: "Parcel sealed & scanned") else ful.proofOfPickupNote,
                        proofOfDeliveryNote = if (targetStatus == FulfilmentStatus.DELIVERED) (proofNote ?: "Handed to recipient in Dhaka") else ful.proofOfDeliveryNote,
                        updatedAt = now
                    )
                )

                dao.insertNotification(
                    NotificationEntity(
                        id = "not_${UUID.randomUUID().toString().take(8)}",
                        targetRole = UserRole.CUSTOMER.name,
                        title = "Delivery Update · ${ful.merchantName}",
                        message = "Status changed to: ${targetStatus.label}",
                        category = "DELIVERY",
                        createdAt = now
                    )
                )
            }
            "Updated status to ${targetStatus.label}"
        }
    }

    suspend fun toggleRiderLocationSharing(
        actorRole: UserRole,
        fulfilmentId: String,
        enabled: Boolean
    ): Result<Unit> {
        return runCatching {
            require(actorRole == UserRole.RIDER) { "Only active riders can toggle telemetry." }
            val ful = dao.getFulfilmentById(fulfilmentId) ?: error("Job not found.")
            require(
                ful.status == FulfilmentStatus.PICKED_UP.name ||
                    ful.status == FulfilmentStatus.OUT_FOR_DELIVERY.name
            ) { "Location sharing is strictly limited to active in-transit deliveries." }
            dao.updateFulfilmentOrder(
                ful.copy(
                    isLocationSharingActive = enabled,
                    riderLat = if (enabled) 23.7949 else null,
                    riderLng = if (enabled) 90.4043 else null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // --- RETURNS & DISPUTES ---
    suspend fun submitReturnOrExchange(
        customerId: String,
        fulfilmentId: String,
        type: ReturnRequestType,
        reason: String
    ): Result<String> {
        return runCatching {
            require(reason.trim().length >= 5) { "Please provide a clear reason." }
            val now = System.currentTimeMillis()
            val ful = dao.getFulfilmentById(fulfilmentId) ?: error("Fulfilment order not found.")
            require(ful.status == FulfilmentStatus.DELIVERED.name) {
                "Returns or exchanges can be requested once delivered."
            }
            dao.insertReturnRequest(
                ReturnRequestEntity(
                    id = "ret_${UUID.randomUUID().toString().take(8)}",
                    fulfilmentOrderId = ful.id,
                    orderId = ful.orderId,
                    customerId = customerId,
                    merchantId = ful.merchantId,
                    requestType = type.name,
                    reason = reason.trim(),
                    status = ReturnStatus.REQUESTED.name,
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.insertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    targetRole = UserRole.PHYSICAL_MERCHANT.name,
                    title = "${type.label} Requested",
                    message = "Customer requested ${type.label} for ${ful.merchantName}: ${reason.trim()}",
                    category = "RETURN",
                    createdAt = now
                )
            )
            "${type.label} request submitted"
        }
    }

    suspend fun reviewReturnRequest(
        actorRole: UserRole,
        returnId: String,
        newStatus: ReturnStatus,
        resolutionNote: String
    ): Result<Unit> {
        return runCatching {
            if (newStatus == ReturnStatus.REFUNDED_CLOSED) {
                requireAdminRole(actorRole)
            } else {
                require(
                    actorRole !in setOf(
                        UserRole.GUEST,
                        UserRole.CUSTOMER,
                        UserRole.RIDER,
                        UserRole.STORE_EMPLOYEE
                    )
                ) {
                    "Insufficient role permissions to approve/reject returns."
                }
            }
            val now = System.currentTimeMillis()
            db.withTransaction {
                val req = dao.getReturnRequestById(returnId) ?: error("Return request not found.")
                dao.updateReturnRequest(
                    req.copy(
                        status = newStatus.name,
                        resolutionNote = resolutionNote.trim(),
                        updatedAt = now
                    )
                )
                if (newStatus == ReturnStatus.REFUNDED_CLOSED) {
                    val ful = dao.getFulfilmentById(req.fulfilmentOrderId)
                    if (ful != null) {
                        dao.insertFinancialLedger(
                            FinancialLedgerEntity(
                                id = "led_${UUID.randomUUID().toString().take(8)}",
                                orderId = req.orderId,
                                fulfilmentOrderId = req.fulfilmentOrderId,
                                merchantId = req.merchantId,
                                entryType = "REFUND_DEBIT",
                                amountBdt = ful.subtotalBdt,
                                status = "AUTHORIZED_BY_ADMIN",
                                note = "Refund processed: $resolutionNote",
                                createdAt = now
                            )
                        )
                    }
                }
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = "aud_${UUID.randomUUID().toString().take(8)}",
                        actorRole = actorRole.name,
                        actorName = actorRole.labelEn,
                        action = "RETURN_STATUS_${newStatus.name}",
                        targetEntity = returnId,
                        details = resolutionNote.trim(),
                        createdAt = now
                    )
                )
            }
        }
    }

    // --- ADMIN & SUPER ADMIN GOVERNANCE ---
    suspend fun toggleMerchantVerification(actorRole: UserRole, merchantId: String): Result<Unit> {
        return runCatching {
            requireAdminRole(actorRole)
            val m = dao.getMerchantById(merchantId) ?: error("Merchant not found.")
            val next = !m.isVerified
            val now = System.currentTimeMillis()
            dao.updateMerchant(m.copy(isVerified = next))
            val ownerRole = when (m.storeType) {
                StoreType.PHYSICAL_POS.name -> UserRole.PHYSICAL_MERCHANT.name
                StoreType.PHYSICAL_MANUAL.name -> UserRole.NO_POS_MERCHANT.name
                else -> UserRole.ONLINE_MERCHANT.name
            }
            dao.insertNotification(
                NotificationEntity(
                    id = "not_${UUID.randomUUID().toString().take(8)}",
                    targetRole = ownerRole,
                    targetUserId = m.ownerUserId,
                    title = if (next) "Verified Badge Granted · ${m.name}" else "Verification Paused · ${m.name}",
                    message = if (next) {
                        "${m.name} has been verified by Porey Admin. Your Verified badge is now visible and you can publish products."
                    } else {
                        "Verification for ${m.name} was paused by Admin."
                    },
                    category = "VERIFICATION",
                    createdAt = now
                )
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    actorRole = actorRole.name,
                    actorName = actorRole.labelEn,
                    action = if (next) "VERIFY_MERCHANT" else "UNVERIFY_MERCHANT",
                    targetEntity = m.name,
                    details = "Merchant verification set to $next",
                    createdAt = now
                )
            )
        }
    }

    suspend fun toggleMerchantSuspension(actorRole: UserRole, merchantId: String): Result<Unit> {
        return runCatching {
            requireAdminRole(actorRole)
            val m = dao.getMerchantById(merchantId) ?: error("Merchant not found.")
            val next = !m.isSuspended
            dao.updateMerchant(m.copy(isSuspended = next))
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    actorRole = actorRole.name,
                    actorName = actorRole.labelEn,
                    action = if (next) "SUSPEND_MERCHANT" else "REINSTATE_MERCHANT",
                    targetEntity = m.name,
                    details = "Suspended = $next",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateMerchantCommission(actorRole: UserRole, merchantId: String, commissionPct: Int): Result<Unit> {
        return runCatching {
            require(actorRole == UserRole.SUPER_ADMIN || actorRole == UserRole.ADMIN) {
                "Only Administrators can configure commission rates."
            }
            require(commissionPct in 1..30) { "Commission must be between 1% and 30%." }
            val m = dao.getMerchantById(merchantId) ?: error("Merchant not found.")
            dao.updateMerchant(m.copy(commissionRatePct = commissionPct))
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    actorRole = actorRole.name,
                    actorName = actorRole.labelEn,
                    action = "UPDATE_COMMISSION",
                    targetEntity = m.name,
                    details = "Commission updated to $commissionPct%",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun toggleDeliveryZone(actorRole: UserRole, zoneId: String): Result<Unit> {
        return runCatching {
            requireAdminRole(actorRole)
            val zone = dao.getDeliveryZoneById(zoneId) ?: error("Zone not found.")
            dao.updateDeliveryZone(zone.copy(isActive = !zone.isActive))
        }
    }

    suspend fun settleMerchantPayables(actorRole: UserRole, merchantId: String, amountBdt: Int): Result<Unit> {
        return runCatching {
            requireAdminRole(actorRole)
            require(amountBdt > 0) { "Settlement amount must be positive." }
            val m = dao.getMerchantById(merchantId) ?: error("Merchant not found.")
            val now = System.currentTimeMillis()
            dao.insertFinancialLedger(
                FinancialLedgerEntity(
                    id = "led_${UUID.randomUUID().toString().take(8)}",
                    orderId = "SETTLEMENT",
                    fulfilmentOrderId = "BATCH",
                    merchantId = merchantId,
                    entryType = "SETTLEMENT_PAYOUT",
                    amountBdt = amountBdt,
                    status = "SETTLED",
                    note = "Payout recorded for ${m.name}",
                    createdAt = now
                )
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    actorRole = actorRole.name,
                    actorName = actorRole.labelEn,
                    action = "SETTLE_MERCHANT",
                    targetEntity = m.name,
                    details = "Settled ৳$amountBdt",
                    createdAt = now
                )
            )
        }
    }

    suspend fun markNotificationsRead(role: UserRole) {
        dao.markNotificationsRead(role.name)
    }

    // --- BACKEND ROLE-BASED ACCESS CONTROL GUARDS ---
    private fun requireInventoryWriteRole(role: UserRole) {
        if (role !in setOf(
                UserRole.PHYSICAL_MERCHANT,
                UserRole.NO_POS_MERCHANT,
                UserRole.ONLINE_MERCHANT,
                UserRole.STORE_EMPLOYEE,
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
            )
        ) {
            throw SecurityException("RBAC denied: ${role.labelEn} cannot modify store inventory.")
        }
    }

    private fun requireOwnerOrAdminForCatalog(role: UserRole) {
        if (role !in setOf(
                UserRole.PHYSICAL_MERCHANT,
                UserRole.NO_POS_MERCHANT,
                UserRole.ONLINE_MERCHANT,
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
            )
        ) {
            throw SecurityException("RBAC denied: ${role.labelEn} cannot publish products or edit pricing.")
        }
    }

    private fun requireAdminRole(role: UserRole) {
        if (role !in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
            throw SecurityException("RBAC denied: Administrator privilege required.")
        }
    }
}
