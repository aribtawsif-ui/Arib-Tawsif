package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DeliveryZoneEntity
import com.example.data.local.entity.FinancialLedgerEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.MerchantListingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PosWebhookEventEntity
import com.example.data.local.entity.ReturnRequestEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.SavedAddressEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.repository.PoreyRepository
import com.example.domain.model.CartItemDetail
import com.example.domain.model.CartMerchantGroup
import com.example.domain.model.CatalogProductView
import com.example.domain.model.CheckoutSummary
import com.example.domain.model.FulfilmentGroupDetail
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.OrderDetailView
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReturnRequestType
import com.example.domain.model.ReturnStatus
import com.example.domain.model.SortOption
import com.example.domain.model.StockMovementReason
import com.example.domain.model.StoreType
import com.example.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CatalogFilterState(
    val query: String = "",
    val selectedCategory: String = "All",
    val selectedSize: String = "All",
    val selectedColor: String = "All",
    val inStockOnly: Boolean = false,
    val expressOnly: Boolean = false,
    val maxPriceBdt: Int = 10000,
    val sortOption: SortOption = SortOption.POPULAR
)

data class PoreyUiState(
    val isReady: Boolean = false,
    val isDarkMode: Boolean = false,
    val hasChosenSession: Boolean = false,
    val isGuest: Boolean = false,
    val showLoginGateDialog: Boolean = false,
    val activeRole: UserRole = UserRole.GUEST,
    val activeUser: UserAccountEntity? = null,
    val showBanglaSubtitles: Boolean = true,
    val users: List<UserAccountEntity> = emptyList(),
    val merchants: List<MerchantEntity> = emptyList(),
    val allListings: List<MerchantListingEntity> = emptyList(),
    val catalogProducts: List<CatalogProductView> = emptyList(),
    val filteredProducts: List<CatalogProductView> = emptyList(),
    val filterState: CatalogFilterState = CatalogFilterState(),
    val cartDetails: List<CartItemDetail> = emptyList(),
    val checkoutSummary: CheckoutSummary = CheckoutSummary(emptyList(), 0, 0, 0, 0),
    val selectedZoneId: String = "zone_dhaka_express",
    val promoCode: String = "",
    val orders: List<OrderDetailView> = emptyList(),
    val deliveryZones: List<DeliveryZoneEntity> = emptyList(),
    val savedAddresses: List<SavedAddressEntity> = emptyList(),
    val wishlistProductIds: Set<String> = emptySet(),
    val reviews: List<ReviewEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val posWebhookEvents: List<PosWebhookEventEntity> = emptyList(),
    val financialLedger: List<FinancialLedgerEntity> = emptyList(),
    val returnRequests: List<ReturnRequestEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val isRiderOnline: Boolean = true
)

private data class SessionControlState(
    val isDarkMode: Boolean = false,
    val hasChosenSession: Boolean = false,
    val isGuest: Boolean = false,
    val showLoginGateDialog: Boolean = false,
    val activeRole: UserRole = UserRole.GUEST,
    val activeUserId: String? = null,
    val showBangla: Boolean = true,
    val filter: CatalogFilterState = CatalogFilterState(),
    val selectedZoneId: String = "zone_dhaka_express",
    val promoCode: String = "",
    val isRiderOnline: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class PoreyViewModel(
    private val repository: PoreyRepository,
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    private val sessionState = MutableStateFlow(SessionControlState())
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        if (appPreferences != null) {
            viewModelScope.launch {
                appPreferences.prefsFlow.collect { prefs ->
                    val users = uiState.value.users
                    val matchedUser = prefs.loggedInUserId?.let { uid -> users.firstOrNull { it.id == uid } }
                    val resolvedRole = when {
                        prefs.isGuest -> UserRole.GUEST
                        matchedUser != null -> UserRole.entries.firstOrNull { it.name == matchedUser.role } ?: UserRole.CUSTOMER
                        prefs.hasChosenSession -> sessionState.value.activeRole
                        else -> UserRole.GUEST
                    }
                    sessionState.value = sessionState.value.copy(
                        isDarkMode = prefs.isDarkMode,
                        hasChosenSession = prefs.hasChosenSession,
                        isGuest = prefs.isGuest,
                        activeUserId = prefs.loggedInUserId,
                        activeRole = resolvedRole
                    )
                }
            }
        }
    }

    private val effectiveCustomerIdFlow = sessionState
        .map { state ->
            if (!state.isGuest && state.activeUserId != null) state.activeUserId else "guest_readonly"
        }
        .distinctUntilChanged()

    private val catalogFlow = combine(
        repository.observeProducts(),
        repository.observeListings(),
        repository.observeMerchants(),
        repository.observeDeliveryZones()
    ) { products, listings, merchants, zones ->
        Triple(products, listings, Pair(merchants, zones))
    }

    private val commerceFlow = combine(
        effectiveCustomerIdFlow.flatMapLatest { cid -> repository.observeCartItems(cid) },
        repository.observeOrders(),
        repository.observeFulfilments(),
        repository.observeOrderItems(),
        repository.observeReturnRequests()
    ) { cart, orders, fulfilments, orderItems, returns ->
        CommerceSnapshot(cart, orders, fulfilments, orderItems, returns)
    }

    private val opsFlow = combine(
        repository.observeUsers(),
        repository.observeStockMovements(),
        repository.observePosWebhookEvents(),
        repository.observeFinancialLedger(),
        repository.observeNotifications()
    ) { users, movements, webhooks, ledger, notifications ->
        OpsSnapshot(users, movements, webhooks, ledger, notifications)
    }

    private val metaFlow = combine(
        repository.observeReviews(),
        repository.observeAuditLogs(),
        effectiveCustomerIdFlow.flatMapLatest { cid -> repository.observeWishlist(cid) },
        effectiveCustomerIdFlow.flatMapLatest { cid -> repository.observeSavedAddresses(cid) }
    ) { reviews, audits, wishlist, addresses ->
        MetaSnapshot(reviews, audits, wishlist, addresses)
    }

    val uiState: StateFlow<PoreyUiState> = combine(
        sessionState,
        catalogFlow,
        commerceFlow,
        opsFlow,
        metaFlow
    ) { session, catalogTuple, commerce, ops, meta ->
        val products = catalogTuple.first
        val listings = catalogTuple.second
        val merchants = catalogTuple.third.first
        val zones = catalogTuple.third.second

        val merchantsById = merchants.associateBy { it.id }
        val productsById = products.associateBy { it.id }
        val listingsById = listings.associateBy { it.id }
        val zonesById = zones.associateBy { it.id }

        val activeUser = if (session.isGuest) {
            null
        } else {
            session.activeUserId?.let { uid -> ops.users.firstOrNull { it.id == uid } }
                ?: ops.users.firstOrNull { it.role == session.activeRole.name }
        }
        val resolvedRole = if (session.isGuest) {
            UserRole.GUEST
        } else if (activeUser != null) {
            UserRole.entries.firstOrNull { it.name == activeUser.role } ?: session.activeRole
        } else {
            session.activeRole
        }

        // Build catalog product views with ranked merchant offers
        val catalogViews = products.map { product ->
            val productListings = listings.filter { it.productId == product.id && it.isPublished }
            val offers = repository.buildRankedOffers(productListings, merchantsById)
            val availableOffers = offers.filter { it.availableQty > 0 }
            val lowestPrice = offers.minOfOrNull { it.effectivePriceBdt } ?: 0
            val maxOriginal = offers.maxOfOrNull { it.listing.priceBdt } ?: lowestPrice
            val nearest = availableOffers.minByOrNull { it.merchant.distanceKm }
                ?: offers.minByOrNull { it.merchant.distanceKm }
            CatalogProductView(
                product = product,
                offers = offers,
                lowestPriceBdt = lowestPrice,
                originalMaxPriceBdt = maxOriginal,
                totalAvailableQty = offers.sumOf { it.availableQty },
                nearestMerchantArea = nearest?.merchant?.area ?: "Dhaka",
                nearestDistanceKm = nearest?.merchant?.distanceKm ?: 2.0,
                hasExpressOption = offers.any { it.merchant.distanceKm <= 10.0 && it.availableQty > 0 },
                bestOffer = offers.firstOrNull { it.isRecommended } ?: availableOffers.firstOrNull()
            )
        }

        val filter = session.filter
        val queryLower = filter.query.trim().lowercase()
        val filtered = catalogViews.filter { view ->
            val matchesQuery = queryLower.isEmpty() ||
                view.product.name.lowercase().contains(queryLower) ||
                view.product.brand.lowercase().contains(queryLower) ||
                view.product.category.lowercase().contains(queryLower) ||
                view.product.fabric.lowercase().contains(queryLower) ||
                view.offers.any { it.merchant.name.lowercase().contains(queryLower) || it.merchant.area.lowercase().contains(queryLower) }
            val matchesCategory = filter.selectedCategory == "All" ||
                (filter.selectedCategory == "New Arrivals" && view.product.isNewArrival) ||
                (filter.selectedCategory == "Offers" && view.lowestPriceBdt < view.originalMaxPriceBdt) ||
                view.product.category.equals(filter.selectedCategory, ignoreCase = true)
            val matchesSize = filter.selectedSize == "All" ||
                view.offers.any { it.listing.size.equals(filter.selectedSize, ignoreCase = true) }
            val matchesColor = filter.selectedColor == "All" ||
                view.offers.any { it.listing.color.contains(filter.selectedColor, ignoreCase = true) }
            val matchesStock = !filter.inStockOnly || view.totalAvailableQty > 0
            val matchesExpress = !filter.expressOnly || view.hasExpressOption
            val matchesPrice = view.lowestPriceBdt <= filter.maxPriceBdt

            matchesQuery && matchesCategory && matchesSize && matchesColor && matchesStock && matchesExpress && matchesPrice
        }.let { list ->
            when (filter.sortOption) {
                SortOption.POPULAR -> list.sortedByDescending { (if (it.product.isBestSeller) 100 else 0) + it.product.reviewCount }
                SortOption.PRICE_LOW -> list.sortedBy { it.lowestPriceBdt }
                SortOption.PRICE_HIGH -> list.sortedByDescending { it.lowestPriceBdt }
                SortOption.RATING -> list.sortedByDescending { it.product.rating }
                SortOption.NEWEST -> list.sortedByDescending { it.product.isNewArrival }
            }
        }

        val cartDetails = commerce.cart.mapNotNull { c ->
            val listing = listingsById[c.listingId] ?: return@mapNotNull null
            val product = productsById[listing.productId] ?: return@mapNotNull null
            val merchant = merchantsById[listing.merchantId] ?: return@mapNotNull null
            CartItemDetail(c, listing, product, merchant)
        }

        val activeZone = zonesById[session.selectedZoneId] ?: zones.firstOrNull()
        val cartGroups = cartDetails.groupBy { it.merchant.id }.mapNotNull { (_, groupItems) ->
            val merchant = groupItems.firstOrNull()?.merchant ?: return@mapNotNull null
            val sub = groupItems.sumOf { it.lineTotalBdt }
            val fee = maxOf(activeZone?.baseChargeBdt ?: 60, merchant.deliveryChargeBdt)
            CartMerchantGroup(
                merchant = merchant,
                items = groupItems,
                subtotalBdt = sub,
                deliveryChargeBdt = fee,
                estimatedWindow = activeZone?.estimatedWindow ?: "Same-Day (2–4 hrs)"
            )
        }
        val subtotal = cartGroups.sumOf { it.subtotalBdt }
        val deliveryTotal = cartGroups.sumOf { it.deliveryChargeBdt }
        val promo = session.promoCode.trim().uppercase()
        val discount = when (promo) {
            "POREY10" -> (subtotal * 0.10).roundToInt().coerceAtMost(600)
            "DHAKA50" -> 50.coerceAtMost(subtotal)
            else -> 0
        }
        val summary = CheckoutSummary(
            groups = cartGroups,
            subtotalBdt = subtotal,
            deliveryTotalBdt = deliveryTotal,
            discountBdt = discount,
            grandTotalBdt = (subtotal - discount + deliveryTotal).coerceAtLeast(0)
        )

        val itemsByFulfilment = commerce.orderItems.groupBy { it.fulfilmentOrderId }
        val fulfilmentsByOrder = commerce.fulfilments.groupBy { it.orderId }
        val orderDetails = commerce.orders.map { order ->
            val fulGroups = (fulfilmentsByOrder[order.id] ?: emptyList()).map { f ->
                FulfilmentGroupDetail(
                    fulfilment = f,
                    items = itemsByFulfilment[f.id] ?: emptyList(),
                    merchant = merchantsById[f.merchantId]
                )
            }
            OrderDetailView(order, fulGroups)
        }

        PoreyUiState(
            isReady = merchants.isNotEmpty(),
            isDarkMode = session.isDarkMode,
            hasChosenSession = session.hasChosenSession,
            isGuest = session.isGuest,
            showLoginGateDialog = session.showLoginGateDialog,
            activeRole = resolvedRole,
            activeUser = activeUser,
            showBanglaSubtitles = session.showBangla,
            users = ops.users,
            merchants = merchants,
            allListings = listings,
            catalogProducts = catalogViews,
            filteredProducts = filtered,
            filterState = filter,
            cartDetails = cartDetails,
            checkoutSummary = summary,
            selectedZoneId = activeZone?.id ?: session.selectedZoneId,
            promoCode = session.promoCode,
            orders = orderDetails,
            deliveryZones = zones,
            savedAddresses = meta.addresses,
            wishlistProductIds = meta.wishlist.map { it.productId }.toSet(),
            reviews = meta.reviews,
            stockMovements = ops.movements,
            posWebhookEvents = ops.webhooks,
            financialLedger = ops.ledger,
            returnRequests = commerce.returns,
            notifications = ops.notifications,
            auditLogs = meta.audits,
            isRiderOnline = session.isRiderOnline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PoreyUiState()
    )

    // --- AUTHENTICATION, THEME & SESSION ---
    fun toggleDarkMode() {
        val next = !sessionState.value.isDarkMode
        sessionState.value = sessionState.value.copy(isDarkMode = next)
        viewModelScope.launch {
            appPreferences?.setDarkMode(next)
        }
        emitMessage(if (next) "Dark Mode enabled" else "Light Mode enabled")
    }

    fun setDarkMode(enabled: Boolean) {
        if (sessionState.value.isDarkMode == enabled) return
        sessionState.value = sessionState.value.copy(isDarkMode = enabled)
        viewModelScope.launch {
            appPreferences?.setDarkMode(enabled)
        }
    }

    fun continueAsGuest() {
        sessionState.value = sessionState.value.copy(
            hasChosenSession = true,
            isGuest = true,
            showLoginGateDialog = false,
            activeRole = UserRole.GUEST,
            activeUserId = null
        )
        viewModelScope.launch {
            appPreferences?.setGuestSession()
        }
        emitMessage("Browsing as Guest · Log in as Customer anytime to buy")
    }

    fun signOutToLogin() {
        sessionState.value = sessionState.value.copy(
            hasChosenSession = false,
            isGuest = false,
            showLoginGateDialog = false,
            activeRole = UserRole.GUEST,
            activeUserId = null
        )
        viewModelScope.launch {
            appPreferences?.clearSessionToAuthGate()
        }
    }

    fun loginAsDemoUser(user: UserAccountEntity) {
        val role = UserRole.entries.firstOrNull { it.name == user.role } ?: UserRole.CUSTOMER
        sessionState.value = sessionState.value.copy(
            hasChosenSession = true,
            isGuest = false,
            showLoginGateDialog = false,
            activeRole = role,
            activeUserId = user.id
        )
        viewModelScope.launch {
            appPreferences?.setLoggedInUser(user.id)
        }
        emitMessage("Signed in as ${user.name}")
    }

    fun loginWithCredentials(identifier: String, password: String) {
        viewModelScope.launch {
            repository.loginUser(identifier, password)
                .onSuccess { user -> loginAsDemoUser(user) }
                .onFailure { emitMessage(it.message ?: "Login failed") }
        }
    }

    fun registerCustomer(
        name: String,
        phone: String,
        email: String,
        password: String,
        address: String
    ) {
        viewModelScope.launch {
            repository.registerCustomer(name, phone, email, password, address)
                .onSuccess { user ->
                    loginAsDemoUser(user)
                    emitMessage("Welcome to Porey, ${user.name}!")
                }
                .onFailure { emitMessage(it.message ?: "Registration failed") }
        }
    }

    fun registerShopOwner(
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
    ) {
        viewModelScope.launch {
            repository.registerShopOwner(
                ownerName = ownerName,
                phone = phone,
                email = email,
                password = password,
                shopName = shopName,
                area = area,
                address = address,
                storeType = storeType,
                posProvider = posProvider,
                tradeLicenseRef = tradeLicenseRef
            ).onSuccess { (user, merchant) ->
                loginAsDemoUser(user)
                emitMessage("${merchant.name} registered! Awaiting Admin verification for Verified badge.")
            }.onFailure {
                emitMessage(it.message ?: "Shop registration failed")
            }
        }
    }

    fun dismissLoginGateDialog() {
        sessionState.value = sessionState.value.copy(showLoginGateDialog = false)
    }

    fun openCustomerLoginFromGate() {
        sessionState.value = sessionState.value.copy(
            showLoginGateDialog = false,
            hasChosenSession = false
        )
    }

    fun switchRole(role: UserRole) {
        if (role == UserRole.GUEST) {
            continueAsGuest()
            return
        }
        val matchedUser = uiState.value.users.firstOrNull { it.role == role.name }
        sessionState.value = sessionState.value.copy(
            hasChosenSession = true,
            isGuest = false,
            showLoginGateDialog = false,
            activeRole = role,
            activeUserId = matchedUser?.id
        )
        viewModelScope.launch {
            if (matchedUser != null) {
                appPreferences?.setLoggedInUser(matchedUser.id)
            }
        }
        emitMessage("Switched workspace to ${role.labelEn}")
    }

    fun toggleBanglaSubtitles() {
        val current = sessionState.value.showBangla
        sessionState.value = sessionState.value.copy(showBangla = !current)
    }

    fun toggleRiderOnline() {
        val current = sessionState.value.isRiderOnline
        sessionState.value = sessionState.value.copy(isRiderOnline = !current)
    }

    // --- SEARCH & FILTER ---
    fun updateFilter(transform: (CatalogFilterState) -> CatalogFilterState) {
        sessionState.value = sessionState.value.copy(filter = transform(sessionState.value.filter))
    }

    fun selectCategoryQuick(category: String) {
        updateFilter { it.copy(selectedCategory = category) }
    }

    fun resetFilters() {
        updateFilter { CatalogFilterState() }
    }

    fun selectDeliveryZone(zoneId: String) {
        sessionState.value = sessionState.value.copy(selectedZoneId = zoneId)
    }

    fun updatePromoCode(code: String) {
        sessionState.value = sessionState.value.copy(promoCode = code)
    }

    private fun currentCustomerId(): String {
        val st = uiState.value
        return if (!st.isGuest && st.activeUser != null) st.activeUser.id else "usr_customer_1"
    }

    // --- CUSTOMER ACTIONS ---
    fun addToCart(listingId: String, qty: Int = 1) {
        val st = uiState.value
        if (st.isGuest || st.activeRole == UserRole.GUEST) {
            sessionState.value = sessionState.value.copy(showLoginGateDialog = true)
            emitMessage("Please log in as a Customer to buy products.")
            return
        }
        viewModelScope.launch {
            repository.addToCart(currentCustomerId(), listingId, qty, isGuest = false)
                .onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "Unable to add to cart") }
        }
    }

    fun updateCartQty(cartItemId: String, delta: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, currentCustomerId(), delta)
                .onFailure { emitMessage(it.message ?: "Stock limit reached") }
        }
    }

    fun removeCartItem(cartItemId: String) {
        viewModelScope.launch {
            repository.removeCartItem(cartItemId)
            emitMessage("Removed item from cart")
        }
    }

    fun toggleWishlist(productId: String) {
        val st = uiState.value
        if (st.isGuest || st.activeRole == UserRole.GUEST) {
            sessionState.value = sessionState.value.copy(showLoginGateDialog = true)
            emitMessage("Log in as a Customer to save wishlist items.")
            return
        }
        viewModelScope.launch {
            val saved = repository.toggleWishlist(currentCustomerId(), productId)
            emitMessage(if (saved) "Saved to wishlist" else "Removed from wishlist")
        }
    }

    fun addAddress(label: String, addressLine: String, zoneName: String, phone: String) {
        viewModelScope.launch {
            repository.addSavedAddress(currentCustomerId(), label, addressLine, zoneName, phone)
                .onSuccess { emitMessage("Address saved") }
                .onFailure { emitMessage(it.message ?: "Invalid address") }
        }
    }

    fun submitReview(productId: String, merchantId: String, rating: Int, comment: String) {
        val st = uiState.value
        if (st.isGuest || st.activeRole == UserRole.GUEST) {
            sessionState.value = sessionState.value.copy(showLoginGateDialog = true)
            emitMessage("Please log in as a Customer to leave a review.")
            return
        }
        viewModelScope.launch {
            val name = st.activeUser?.name ?: "Dhaka Customer"
            repository.submitReview(productId, merchantId, name, rating, comment)
                .onSuccess { emitMessage("Review published") }
                .onFailure { emitMessage(it.message ?: "Review error") }
        }
    }

    fun placeOrder(
        addressLine: String,
        paymentMethod: PaymentMethod,
        onOrderCreated: (String) -> Unit
    ) {
        val state = uiState.value
        if (state.isGuest || state.activeRole == UserRole.GUEST) {
            sessionState.value = sessionState.value.copy(showLoginGateDialog = true)
            emitMessage("Customer login required to complete purchase.")
            return
        }
        viewModelScope.launch {
            val customerName = state.activeUser?.name ?: "Nafisa Rahman"
            repository.placeOrder(
                customerId = currentCustomerId(),
                customerName = customerName,
                deliveryAddress = addressLine,
                zoneId = state.selectedZoneId,
                paymentMethod = paymentMethod,
                promoCodeInput = state.promoCode,
                isGuest = false
            ).onSuccess { orderCode ->
                sessionState.value = sessionState.value.copy(promoCode = "")
                emitMessage("Order $orderCode placed successfully!")
                onOrderCreated(orderCode)
            }.onFailure { err ->
                emitMessage(err.message ?: "Checkout failed")
            }
        }
    }

    fun submitReturnRequest(fulfilmentId: String, type: ReturnRequestType, reason: String) {
        viewModelScope.launch {
            repository.submitReturnOrExchange(currentCustomerId(), fulfilmentId, type, reason)
                .onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "Failed to submit request") }
        }
    }

    // --- MERCHANT & POS OPERATIONS ---
    fun sendPosWebhook(
        merchantId: String,
        sku: String,
        qtySold: Int,
        idempotencyKey: String,
        signatureValid: Boolean
    ) {
        viewModelScope.launch {
            repository.processPosSaleWebhook(
                actorRole = uiState.value.activeRole,
                merchantId = merchantId,
                sku = sku,
                qtySold = qtySold,
                idempotencyKey = idempotencyKey,
                signatureValid = signatureValid
            ).onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "POS webhook error") }
        }
    }

    fun adjustManualStock(
        listingId: String,
        deltaPhysical: Int,
        deltaDamaged: Int,
        reason: StockMovementReason,
        note: String
    ) {
        viewModelScope.launch {
            repository.adjustManualStock(
                actorRole = uiState.value.activeRole,
                listingId = listingId,
                deltaPhysical = deltaPhysical,
                deltaDamaged = deltaDamaged,
                reason = reason,
                note = note
            ).onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "Permission or stock error") }
        }
    }

    fun reconcileEndOfDay(listingId: String, verifiedCount: Int) {
        viewModelScope.launch {
            repository.reconcileEndOfDayStock(
                actorRole = uiState.value.activeRole,
                listingId = listingId,
                verifiedPhysicalQty = verifiedCount
            ).onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "Reconciliation failed") }
        }
    }

    fun publishNewProduct(
        merchantId: String,
        name: String,
        brand: String,
        category: String,
        fabric: String,
        size: String,
        color: String,
        sku: String,
        priceBdt: Int,
        discountBdt: Int?,
        initialStock: Int,
        imageKey: String
    ) {
        viewModelScope.launch {
            repository.createOnlineProductAndListing(
                actorRole = uiState.value.activeRole,
                merchantId = merchantId,
                name = name,
                brand = brand,
                category = category,
                fabric = fabric,
                size = size,
                color = color,
                sku = sku,
                priceBdt = priceBdt,
                discountPriceBdt = discountBdt,
                initialStock = initialStock,
                imageKey = imageKey
            ).onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "Publish failed") }
        }
    }

    fun toggleListingPublished(listingId: String) {
        viewModelScope.launch {
            repository.toggleListingPublished(uiState.value.activeRole, listingId)
                .onFailure { emitMessage(it.message ?: "Unable to change visibility") }
        }
    }

    // --- FULFILMENT & RIDER ---
    fun advanceFulfilment(
        fulfilmentId: String,
        targetStatus: FulfilmentStatus,
        proofNote: String? = null
    ) {
        viewModelScope.launch {
            val riderUser = uiState.value.users.firstOrNull { it.role == UserRole.RIDER.name }
            val assignRider = targetStatus in setOf(
                FulfilmentStatus.RIDER_ASSIGNED,
                FulfilmentStatus.PICKED_UP,
                FulfilmentStatus.OUT_FOR_DELIVERY,
                FulfilmentStatus.DELIVERED
            )
            repository.advanceFulfilmentStatus(
                actorRole = uiState.value.activeRole,
                fulfilmentId = fulfilmentId,
                targetStatus = targetStatus,
                riderId = if (assignRider) riderUser?.id ?: "usr_rider_1" else null,
                riderName = if (assignRider) riderUser?.name ?: "Sajib Ahmed" else null,
                proofNote = proofNote
            ).onSuccess { emitMessage(it) }
                .onFailure { emitMessage(it.message ?: "Transition denied") }
        }
    }

    fun toggleRiderLocation(fulfilmentId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRiderLocationSharing(uiState.value.activeRole, fulfilmentId, enabled)
                .onSuccess { emitMessage(if (enabled) "Live rider location enabled for this job" else "Location sharing paused") }
                .onFailure { emitMessage(it.message ?: "Unable to toggle location") }
        }
    }

    // --- RETURNS & ADMIN ---
    fun reviewReturn(returnId: String, newStatus: ReturnStatus, note: String) {
        viewModelScope.launch {
            repository.reviewReturnRequest(uiState.value.activeRole, returnId, newStatus, note)
                .onSuccess { emitMessage("Return updated to ${newStatus.label}") }
                .onFailure { emitMessage(it.message ?: "Action denied") }
        }
    }

    fun toggleMerchantVerified(merchantId: String) {
        viewModelScope.launch {
            repository.toggleMerchantVerification(uiState.value.activeRole, merchantId)
                .onSuccess { emitMessage("Merchant verification updated") }
                .onFailure { emitMessage(it.message ?: "Admin role required") }
        }
    }

    fun toggleMerchantSuspended(merchantId: String) {
        viewModelScope.launch {
            repository.toggleMerchantSuspension(uiState.value.activeRole, merchantId)
                .onSuccess { emitMessage("Merchant status updated") }
                .onFailure { emitMessage(it.message ?: "Admin role required") }
        }
    }

    fun updateCommission(merchantId: String, commissionPct: Int) {
        viewModelScope.launch {
            repository.updateMerchantCommission(uiState.value.activeRole, merchantId, commissionPct)
                .onSuccess { emitMessage("Commission updated to $commissionPct%") }
                .onFailure { emitMessage(it.message ?: "Failed to update commission") }
        }
    }

    fun toggleZone(zoneId: String) {
        viewModelScope.launch {
            repository.toggleDeliveryZone(uiState.value.activeRole, zoneId)
                .onFailure { emitMessage(it.message ?: "Admin role required") }
        }
    }

    fun settleMerchant(merchantId: String, amountBdt: Int) {
        viewModelScope.launch {
            repository.settleMerchantPayables(uiState.value.activeRole, merchantId, amountBdt)
                .onSuccess { emitMessage("Settlement recorded") }
                .onFailure { emitMessage(it.message ?: "Settlement failed") }
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead(uiState.value.activeRole)
        }
    }

    private fun emitMessage(msg: String) {
        _events.tryEmit(msg)
    }

    class Factory(
        private val repository: PoreyRepository,
        private val appPreferences: AppPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PoreyViewModel(repository, appPreferences) as T
        }
    }
}

private data class CommerceSnapshot(
    val cart: List<com.example.data.local.entity.CartItemEntity>,
    val orders: List<com.example.data.local.entity.OrderEntity>,
    val fulfilments: List<com.example.data.local.entity.FulfilmentOrderEntity>,
    val orderItems: List<com.example.data.local.entity.OrderItemEntity>,
    val returns: List<ReturnRequestEntity>
)

private data class OpsSnapshot(
    val users: List<UserAccountEntity>,
    val movements: List<StockMovementEntity>,
    val webhooks: List<PosWebhookEventEntity>,
    val ledger: List<FinancialLedgerEntity>,
    val notifications: List<NotificationEntity>
)

private data class MetaSnapshot(
    val reviews: List<ReviewEntity>,
    val audits: List<AuditLogEntity>,
    val wishlist: List<com.example.data.local.entity.WishlistItemEntity>,
    val addresses: List<SavedAddressEntity>
)
