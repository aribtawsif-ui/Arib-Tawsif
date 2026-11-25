package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.UserRole
import com.example.ui.components.PoreyBrandHeader
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.MerchantDashboardScreen
import com.example.ui.screens.OrdersTrackingScreen
import com.example.ui.screens.PoreyWebStoreScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.RiderDashboardScreen
import com.example.ui.screens.WorkspaceHubScreen
import com.example.ui.state.PoreyViewModel

private enum class PoreyTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Shop", Icons.Filled.Home, Icons.Outlined.Home),
    DISCOVER("discover", "Discover", Icons.Filled.Search, Icons.Outlined.Search),
    WEB("web", "Website", Icons.Filled.Language, Icons.Outlined.Language),
    CART("cart", "Bag", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    ORDERS("orders", "Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    OPS("ops", "Console", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    RIDER("rider", "Deliveries", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    ADMIN("admin", "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
    ACCOUNT("account", "Account", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun PoreyApp(viewModel: PoreyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(PoreyTab.HOME) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var launchWebsitePending by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Automatically route to role's primary console when switching roles or signing in (unless user is inside Website)
    LaunchedEffect(uiState.activeRole, uiState.hasChosenSession) {
        selectedProductId = null
        if (launchWebsitePending && uiState.hasChosenSession) {
            currentTab = PoreyTab.WEB
            launchWebsitePending = false
        } else if (currentTab != PoreyTab.WEB || !uiState.hasChosenSession) {
            currentTab = when (uiState.activeRole) {
                UserRole.GUEST,
                UserRole.CUSTOMER -> PoreyTab.HOME
                UserRole.PHYSICAL_MERCHANT,
                UserRole.NO_POS_MERCHANT,
                UserRole.ONLINE_MERCHANT,
                UserRole.STORE_EMPLOYEE -> PoreyTab.OPS
                UserRole.RIDER -> PoreyTab.RIDER
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN -> PoreyTab.ADMIN
            }
        }
    }

    if (uiState.hasChosenSession) {
        if (selectedProductId != null) {
            BackHandler {
                selectedProductId = null
            }
        } else if (currentTab != PoreyTab.HOME && (uiState.activeRole == UserRole.CUSTOMER || uiState.activeRole == UserRole.GUEST)) {
            BackHandler {
                currentTab = PoreyTab.HOME
            }
        }
    }

    val navTabs = when (uiState.activeRole) {
        UserRole.GUEST -> listOf(
            PoreyTab.HOME,
            PoreyTab.DISCOVER,
            PoreyTab.WEB,
            PoreyTab.CART,
            PoreyTab.ACCOUNT
        )
        UserRole.CUSTOMER -> listOf(
            PoreyTab.HOME,
            PoreyTab.DISCOVER,
            PoreyTab.CART,
            PoreyTab.ORDERS,
            PoreyTab.ACCOUNT
        )
        UserRole.PHYSICAL_MERCHANT,
        UserRole.NO_POS_MERCHANT,
        UserRole.ONLINE_MERCHANT,
        UserRole.STORE_EMPLOYEE -> listOf(
            PoreyTab.OPS,
            PoreyTab.HOME,
            PoreyTab.WEB,
            PoreyTab.ACCOUNT
        )
        UserRole.RIDER -> listOf(
            PoreyTab.RIDER,
            PoreyTab.HOME,
            PoreyTab.WEB,
            PoreyTab.ACCOUNT
        )
        UserRole.ADMIN,
        UserRole.SUPER_ADMIN -> listOf(
            PoreyTab.ADMIN,
            PoreyTab.HOME,
            PoreyTab.WEB,
            PoreyTab.ACCOUNT
        )
    }

    val cartBadgeCount = if (uiState.isGuest) 0 else uiState.cartDetails.sumOf { it.cartItem.quantity }
    val unreadNotifCount = uiState.notifications.count { !it.isRead && it.targetRole == uiState.activeRole.name }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (uiState.hasChosenSession && !useRail && selectedProductId == null) {
                    NavigationBar {
                        navTabs.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab == PoreyTab.CART && cartBadgeCount > 0) {
                                                Badge { Text("$cartBadgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.hasChosenSession && useRail && selectedProductId == null) {
                    NavigationRail {
                        navTabs.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab == PoreyTab.CART && cartBadgeCount > 0) {
                                                Badge { Text("$cartBadgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        if (!uiState.isReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (!uiState.hasChosenSession) {
                            AuthScreen(
                                uiState = uiState,
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onLoginWithCredentials = { id, pw -> viewModel.loginWithCredentials(id, pw) },
                                onLoginDemoUser = { user -> viewModel.loginAsDemoUser(user) },
                                onContinueAsGuest = { viewModel.continueAsGuest() },
                                onOpenWebsite = {
                                    launchWebsitePending = true
                                    viewModel.continueAsGuest()
                                },
                                onRegisterCustomer = { n, ph, em, pw, addr ->
                                    viewModel.registerCustomer(n, ph, em, pw, addr)
                                },
                                onRegisterShopOwner = { owner, ph, em, pw, shop, area, addr, type, pos, lic ->
                                    viewModel.registerShopOwner(owner, ph, em, pw, shop, area, addr, type, pos, lic)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (selectedProductId != null) {
                            val productView = uiState.catalogProducts.firstOrNull { it.product.id == selectedProductId }
                            if (productView != null) {
                                ProductDetailScreen(
                                    productView = productView,
                                    uiState = uiState,
                                    onBack = { selectedProductId = null },
                                    onAddToCart = { listingId, qty -> viewModel.addToCart(listingId, qty) },
                                    onToggleWishlist = { viewModel.toggleWishlist(it) },
                                    onSubmitReview = { pid, mid, r, c -> viewModel.submitReview(pid, mid, r, c) }
                                )
                            } else {
                                selectedProductId = null
                            }
                        } else {
                            PoreyBrandHeader(
                                activeRole = uiState.activeRole,
                                showBangla = uiState.showBanglaSubtitles,
                                isDarkMode = uiState.isDarkMode,
                                isWebViewActive = currentTab == PoreyTab.WEB,
                                unreadNotifications = unreadNotifCount,
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onToggleWebView = {
                                    currentTab = if (currentTab == PoreyTab.WEB) PoreyTab.HOME else PoreyTab.WEB
                                },
                                onRoleBadgeClick = { currentTab = PoreyTab.ACCOUNT },
                                onNotificationsClick = {
                                    viewModel.markNotificationsRead()
                                    currentTab = PoreyTab.ACCOUNT
                                }
                            )

                            when (currentTab) {
                                PoreyTab.HOME -> CustomerHomeScreen(
                                    uiState = uiState,
                                    onCategoryClick = { cat ->
                                        viewModel.selectCategoryQuick(cat)
                                        currentTab = PoreyTab.DISCOVER
                                    },
                                    onOpenDiscover = { currentTab = PoreyTab.DISCOVER },
                                    onOpenProduct = { pid -> selectedProductId = pid },
                                    onToggleWishlist = { pid -> viewModel.toggleWishlist(pid) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.DISCOVER -> DiscoverScreen(
                                    uiState = uiState,
                                    onUpdateFilter = { viewModel.updateFilter(it) },
                                    onResetFilters = { viewModel.resetFilters() },
                                    onOpenProduct = { pid -> selectedProductId = pid },
                                    onToggleWishlist = { pid -> viewModel.toggleWishlist(pid) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.WEB -> PoreyWebStoreScreen(
                                    uiState = uiState,
                                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                                    onLoginWithCredentials = { id, pw -> viewModel.loginWithCredentials(id, pw) },
                                    onLoginDemoRole = { role -> viewModel.switchRole(role) },
                                    onContinueAsGuest = { viewModel.continueAsGuest() },
                                    onRegisterCustomer = { n, ph, em, pw, addr ->
                                        viewModel.registerCustomer(n, ph, em, pw, addr)
                                    },
                                    onRegisterShopOwner = { ow, ph, em, pw, sn, ar, ad, st, lic ->
                                        viewModel.registerShopOwner(ow, ph, em, pw, sn, ar, ad, st, null, lic)
                                    },
                                    onAddToCart = { lid, qty -> viewModel.addToCart(lid, qty) },
                                    onUpdateCartQty = { cid, d -> viewModel.updateCartQty(cid, d) },
                                    onRemoveCartItem = { cid -> viewModel.removeCartItem(cid) },
                                    onSelectZone = { zid -> viewModel.selectDeliveryZone(zid) },
                                    onUpdatePromoCode = { code -> viewModel.updatePromoCode(code) },
                                    onPlaceOrder = { addr, pm -> viewModel.placeOrder(addr, pm) {} },
                                    onToggleMerchantVerified = { mid -> viewModel.toggleMerchantVerified(mid) },
                                    onPublishProduct = { mid, n, br, cat, fab, sz, col, sku, pr, st, img ->
                                        viewModel.publishNewProduct(mid, n, br, cat, fab, sz, col, sku, pr, null, st, img)
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.CART -> CartCheckoutScreen(
                                    uiState = uiState,
                                    onUpdateQty = { id, delta -> viewModel.updateCartQty(id, delta) },
                                    onRemoveItem = { id -> viewModel.removeCartItem(id) },
                                    onSelectZone = { viewModel.selectDeliveryZone(it) },
                                    onPromoChange = { viewModel.updatePromoCode(it) },
                                    onPlaceOrder = { addr, method ->
                                        viewModel.placeOrder(addr, method) {
                                            currentTab = PoreyTab.ORDERS
                                        }
                                    },
                                    onBrowseShop = { currentTab = PoreyTab.DISCOVER },
                                    onLoginAsCustomer = { viewModel.openCustomerLoginFromGate() },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.ORDERS -> OrdersTrackingScreen(
                                    uiState = uiState,
                                    onCancelFulfilment = { id ->
                                        viewModel.advanceFulfilment(id, FulfilmentStatus.CANCELLED)
                                    },
                                    onSubmitReturn = { id, type, reason ->
                                        viewModel.submitReturnRequest(id, type, reason)
                                    },
                                    onBrowseShop = { currentTab = PoreyTab.DISCOVER },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.OPS -> MerchantDashboardScreen(
                                    uiState = uiState,
                                    onSendPosWebhook = { mid, sku, qty, key, sig ->
                                        viewModel.sendPosWebhook(mid, sku, qty, key, sig)
                                    },
                                    onAdjustStock = { lid, dPhys, dDmg, reason, note ->
                                        viewModel.adjustManualStock(lid, dPhys, dDmg, reason, note)
                                    },
                                    onReconcileEod = { lid, count ->
                                        viewModel.reconcileEndOfDay(lid, count)
                                    },
                                    onPublishProduct = { mid, name, brand, cat, fab, sz, col, sku, pr, disc, st, img ->
                                        viewModel.publishNewProduct(mid, name, brand, cat, fab, sz, col, sku, pr, disc, st, img)
                                    },
                                    onTogglePublished = { lid -> viewModel.toggleListingPublished(lid) },
                                    onAdvanceFulfilment = { fid, st -> viewModel.advanceFulfilment(fid, st) },
                                    onReviewReturn = { rid, st, note -> viewModel.reviewReturn(rid, st, note) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.RIDER -> RiderDashboardScreen(
                                    uiState = uiState,
                                    onToggleOnline = { viewModel.toggleRiderOnline() },
                                    onAdvanceFulfilment = { fid, st, note -> viewModel.advanceFulfilment(fid, st, note) },
                                    onToggleLocationSharing = { fid, en -> viewModel.toggleRiderLocation(fid, en) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.ADMIN -> AdminDashboardScreen(
                                    uiState = uiState,
                                    onToggleMerchantVerified = { viewModel.toggleMerchantVerified(it) },
                                    onToggleMerchantSuspended = { viewModel.toggleMerchantSuspended(it) },
                                    onUpdateCommission = { mid, pct -> viewModel.updateCommission(mid, pct) },
                                    onSettleMerchant = { mid, amt -> viewModel.settleMerchant(mid, amt) },
                                    onReviewReturn = { rid, st, note -> viewModel.reviewReturn(rid, st, note) },
                                    onToggleZone = { viewModel.toggleZone(it) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )

                                PoreyTab.ACCOUNT -> WorkspaceHubScreen(
                                    uiState = uiState,
                                    onSwitchRole = { role -> viewModel.switchRole(role) },
                                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                                    onToggleBangla = { viewModel.toggleBanglaSubtitles() },
                                    onSignOut = { viewModel.signOutToLogin() },
                                    onAddAddress = { l, a, z, p -> viewModel.addAddress(l, a, z, p) },
                                    onMarkNotificationsRead = { viewModel.markNotificationsRead() },
                                    onOpenProduct = { pid -> selectedProductId = pid },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Login Gate Dialog for Guest buyers
    if (uiState.showLoginGateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLoginGateDialog() },
            title = { Text("Customer Login Required") },
            text = {
                Text(
                    "Guests can browse products, compare verified shops, and explore Web View freely. Please log in or register as a Customer to buy products and reserve stock."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.openCustomerLoginFromGate() },
                    modifier = Modifier.testTag("gate_login_customer_btn")
                ) {
                    Text("Login as Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLoginGateDialog() }) {
                    Text("Keep Browsing")
                }
            }
        )
    }
}
