package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.PaymentMethod
import com.example.domain.model.StoreType
import com.example.domain.model.UserRole
import com.example.domain.model.formatBdt
import com.example.ui.state.PoreyUiState
import org.json.JSONArray
import org.json.JSONObject

private const val LOCAL_WEBSITE_URL = "file:///android_asset/website/index.html"

class PoreyWebBridge(
    private val onToggleTheme: () -> Unit,
    private val onLoginWithCredentials: (String, String) -> Unit,
    private val onLoginDemoRole: (UserRole) -> Unit,
    private val onContinueAsGuest: () -> Unit,
    private val onRegisterCustomer: (String, String, String, String, String) -> Unit,
    private val onRegisterShopOwner: (String, String, String, String, String, String, String, StoreType, String) -> Unit,
    private val onAddToCart: (String, Int) -> Unit,
    private val onUpdateCartQty: (String, Int) -> Unit,
    private val onRemoveCartItem: (String) -> Unit,
    private val onSelectZone: (String) -> Unit,
    private val onUpdatePromoCode: (String) -> Unit,
    private val onPlaceOrder: (String, PaymentMethod) -> Unit,
    private val onToggleMerchantVerified: (String) -> Unit,
    private val onPublishProduct: (String, String, String, String, String, String, String, String, Int, Int, String) -> Unit
) {
    @JavascriptInterface
    fun toggleTheme() = onToggleTheme()

    @JavascriptInterface
    fun loginWithCredentials(identifier: String, password: String) =
        onLoginWithCredentials(identifier, password)

    @JavascriptInterface
    fun loginDemoRole(roleName: String) {
        if (roleName == UserRole.GUEST.name) {
            onContinueAsGuest()
        } else {
            val role = UserRole.entries.firstOrNull { it.name == roleName } ?: UserRole.CUSTOMER
            onLoginDemoRole(role)
        }
    }

    @JavascriptInterface
    fun continueAsGuest() = onContinueAsGuest()

    @JavascriptInterface
    fun registerCustomer(name: String, phone: String, email: String, password: String, address: String) =
        onRegisterCustomer(name, phone, email, password, address)

    @JavascriptInterface
    fun registerShopOwner(
        ownerName: String,
        phone: String,
        email: String,
        password: String,
        shopName: String,
        area: String,
        address: String,
        storeTypeName: String,
        tradeLicenseRef: String
    ) {
        val storeType = StoreType.entries.firstOrNull { it.name == storeTypeName } ?: StoreType.PHYSICAL_POS
        onRegisterShopOwner(ownerName, phone, email, password, shopName, area, address, storeType, tradeLicenseRef)
    }

    @JavascriptInterface
    fun addToCart(listingId: String, qty: Int) = onAddToCart(listingId, qty)

    @JavascriptInterface
    fun updateCartQty(cartItemId: String, delta: Int) = onUpdateCartQty(cartItemId, delta)

    @JavascriptInterface
    fun removeCartItem(cartItemId: String) = onRemoveCartItem(cartItemId)

    @JavascriptInterface
    fun selectDeliveryZone(zoneId: String) = onSelectZone(zoneId)

    @JavascriptInterface
    fun updatePromoCode(code: String) = onUpdatePromoCode(code)

    @JavascriptInterface
    fun placeOrder(address: String, paymentMethodName: String) {
        val method = PaymentMethod.entries.firstOrNull { it.name == paymentMethodName }
            ?: PaymentMethod.CASH_ON_DELIVERY
        onPlaceOrder(address, method)
    }

    @JavascriptInterface
    fun toggleMerchantVerified(merchantId: String) = onToggleMerchantVerified(merchantId)

    @JavascriptInterface
    fun publishProduct(
        merchantId: String,
        name: String,
        brand: String,
        category: String,
        fabric: String,
        size: String,
        color: String,
        sku: String,
        priceBdt: Int,
        stock: Int,
        imageKey: String
    ) = onPublishProduct(merchantId, name, brand, category, fabric, size, color, sku, priceBdt, stock, imageKey)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PoreyWebStoreScreen(
    uiState: PoreyUiState,
    onToggleDarkMode: () -> Unit,
    onLoginWithCredentials: (String, String) -> Unit,
    onLoginDemoRole: (UserRole) -> Unit,
    onContinueAsGuest: () -> Unit,
    onRegisterCustomer: (String, String, String, String, String) -> Unit,
    onRegisterShopOwner: (String, String, String, String, String, String, String, StoreType, String) -> Unit,
    onAddToCart: (String, Int) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onRemoveCartItem: (String) -> Unit,
    onSelectZone: (String) -> Unit,
    onUpdatePromoCode: (String) -> Unit,
    onPlaceOrder: (String, PaymentMethod) -> Unit,
    onToggleMerchantVerified: (String) -> Unit,
    onPublishProduct: (String, String, String, String, String, String, String, String, Int, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useLiveUrl by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("https://example.com") }
    var activeExternalUrl by remember { mutableStateOf("https://example.com") }
    var loadProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLocalWebsiteLoaded by remember { mutableStateOf(false) }

    val stateJson = remember(
        uiState.isDarkMode,
        uiState.activeRole,
        uiState.isGuest,
        uiState.activeUser,
        uiState.merchants,
        uiState.catalogProducts,
        uiState.cartDetails,
        uiState.deliveryZones,
        uiState.selectedZoneId,
        uiState.promoCode
    ) {
        buildPoreyWebsiteStateJson(uiState)
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    loadProgress = newProgress
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    canGoBack = view?.canGoBack() == true
                    if (url != LOCAL_WEBSITE_URL) {
                        isLocalWebsiteLoaded = false
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    canGoBack = view?.canGoBack() == true
                    if (url == LOCAL_WEBSITE_URL) {
                        isLocalWebsiteLoaded = true
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }
        }
    }

    DisposableEffect(webView) {
        val bridge = PoreyWebBridge(
            onToggleTheme = { webView.post { onToggleDarkMode() } },
            onLoginWithCredentials = { id, pw -> webView.post { onLoginWithCredentials(id, pw) } },
            onLoginDemoRole = { role -> webView.post { onLoginDemoRole(role) } },
            onContinueAsGuest = { webView.post { onContinueAsGuest() } },
            onRegisterCustomer = { n, ph, em, pw, ad -> webView.post { onRegisterCustomer(n, ph, em, pw, ad) } },
            onRegisterShopOwner = { ow, ph, em, pw, sn, ar, ad, st, lic ->
                webView.post { onRegisterShopOwner(ow, ph, em, pw, sn, ar, ad, st, lic) }
            },
            onAddToCart = { lid, q -> webView.post { onAddToCart(lid, q) } },
            onUpdateCartQty = { cid, d -> webView.post { onUpdateCartQty(cid, d) } },
            onRemoveCartItem = { cid -> webView.post { onRemoveCartItem(cid) } },
            onSelectZone = { zid -> webView.post { onSelectZone(zid) } },
            onUpdatePromoCode = { code -> webView.post { onUpdatePromoCode(code) } },
            onPlaceOrder = { addr, pm -> webView.post { onPlaceOrder(addr, pm) } },
            onToggleMerchantVerified = { mid -> webView.post { onToggleMerchantVerified(mid) } },
            onPublishProduct = { mid, n, br, cat, fab, sz, col, sku, pr, st, img ->
                webView.post { onPublishProduct(mid, n, br, cat, fab, sz, col, sku, pr, st, img) }
            }
        )
        webView.addJavascriptInterface(bridge, "PoreyBridge")
        onDispose {
            webView.removeJavascriptInterface("PoreyBridge")
            webView.destroy()
        }
    }

    LaunchedEffect(useLiveUrl, activeExternalUrl) {
        if (useLiveUrl) {
            isLocalWebsiteLoaded = false
            webView.loadUrl(activeExternalUrl)
        } else {
            webView.loadUrl(LOCAL_WEBSITE_URL)
        }
    }

    LaunchedEffect(isLocalWebsiteLoaded, stateJson, useLiveUrl) {
        if (!useLiveUrl && isLocalWebsiteLoaded) {
            webView.evaluateJavascript(
                "window.PoreyWebsite && window.PoreyWebsite.syncState($stateJson);",
                null
            )
        }
    }

    BackHandler(enabled = canGoBack) {
        webView.goBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = !useLiveUrl,
                            onClick = { useLiveUrl = false },
                            leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Porey Website") },
                            modifier = Modifier.testTag("webview_local_mode_chip")
                        )
                        FilterChip(
                            selected = useLiveUrl,
                            onClick = { useLiveUrl = true },
                            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Custom URL") },
                            modifier = Modifier.testTag("webview_url_mode_chip")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = "Toggle Website Light/Dark theme"
                            )
                        }
                        if (canGoBack) {
                            IconButton(
                                onClick = { webView.goBack() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Website back")
                            }
                        }
                        IconButton(
                            onClick = { webView.reload() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reload Website")
                        }
                    }
                }

                if (useLiveUrl) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("External URL (https://...)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                val normalized = if (customUrlInput.startsWith("http")) customUrlInput else "https://$customUrlInput"
                                activeExternalUrl = normalized
                                webView.loadUrl(normalized)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Go",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("porey_android_webview")
            )
        }
    }
}

fun buildPoreyWebsiteStateJson(uiState: PoreyUiState): String {
    val root = JSONObject()
    root.put("isDarkMode", uiState.isDarkMode)
    root.put("activeRole", uiState.activeRole.name)
    root.put("isGuest", uiState.isGuest || uiState.activeRole == UserRole.GUEST)
    root.put("promoCode", uiState.promoCode)
    root.put("selectedZoneId", uiState.selectedZoneId)

    val user = uiState.activeUser
    if (user != null && !uiState.isGuest && uiState.activeRole != UserRole.GUEST) {
        root.put(
            "activeUser",
            JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("email", user.email)
                put("phone", user.phone)
                put("role", user.role)
                put("merchantId", user.assignedMerchantId ?: JSONObject.NULL)
                put("address", user.defaultAddress)
            }
        )
    } else {
        root.put("activeUser", JSONObject.NULL)
    }

    val zonesArray = JSONArray()
    uiState.deliveryZones.forEach { z ->
        zonesArray.put(
            JSONObject().apply {
                put("id", z.id)
                put("name", z.name)
                put("feeBdt", z.baseChargeBdt)
                put("window", z.estimatedWindow)
            }
        )
    }
    root.put("zones", zonesArray)

    val merchantsArray = JSONArray()
    uiState.merchants.forEach { m ->
        merchantsArray.put(
            JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("area", m.area)
                put("storeType", m.storeType)
                put("isVerified", m.isVerified)
                put("isSuspended", m.isSuspended)
                put("rating", m.rating)
                put("tradeLicenseRef", m.tradeLicenseRef ?: "N/A")
                put("ownerUserId", m.ownerUserId ?: JSONObject.NULL)
            }
        )
    }
    root.put("merchants", merchantsArray)

    val productsArray = JSONArray()
    uiState.catalogProducts.forEach { item ->
        val offersArray = JSONArray()
        item.offers.forEach { o ->
            offersArray.put(
                JSONObject().apply {
                    put("listingId", o.listing.id)
                    put("merchantId", o.merchant.id)
                    put("size", o.listing.size)
                    put("color", o.listing.color)
                    put("priceBdt", o.listing.priceBdt)
                    put("discountPriceBdt", o.listing.discountPriceBdt ?: JSONObject.NULL)
                    put("availableQty", o.availableQty)
                }
            )
        }
        productsArray.put(
            JSONObject().apply {
                put("id", item.product.id)
                put("name", item.product.name)
                put("brand", item.product.brand)
                put("category", item.product.category)
                put("fabric", item.product.fabric)
                put("rating", item.product.rating)
                put("imageKey", item.product.imageKey)
                put("isNewArrival", item.product.isNewArrival)
                put("offers", offersArray)
            }
        )
    }
    root.put("products", productsArray)

    val cartArray = JSONArray()
    if (!uiState.isGuest && uiState.activeRole != UserRole.GUEST) {
        uiState.cartDetails.forEach { c ->
            cartArray.put(
                JSONObject().apply {
                    put("id", c.cartItem.id)
                    put("listingId", c.listing.id)
                    put("productName", c.product.name)
                    put("merchantName", c.merchant.name)
                    put("unitPriceBdt", c.unitPriceBdt)
                    put("qty", c.cartItem.quantity)
                }
            )
        }
    }
    root.put("cart", cartArray)

    return root.toString()
}

fun buildPoreyWebHtml(uiState: PoreyUiState): String {
    val dark = uiState.isDarkMode
    val bg = if (dark) "#141A16" else "#F3EFE4"
    val surface = if (dark) "#1D2520" else "#FAF8F2"
    val surfaceAlt = if (dark) "#27312B" else "#EAE3D2"
    val text = if (dark) "#F3EFE4" else "#1B241E"
    val muted = if (dark) "#B3BEB6" else "#556259"
    val border = if (dark) "#3E4A42" else "#D1CAB9"
    val primary = if (dark) "#A7B69B" else "#2E3A32"
    val primaryText = if (dark) "#1B241E" else "#FAF8F2"
    val coral = if (dark) "#FF6E57" else "#E94E35"
    val verifiedBg = if (dark) "#1B3B2B" else "#D8F3DC"
    val verifiedFg = if (dark) "#7EE2A8" else "#2D6A4F"
    val pendingBg = if (dark) "#3B2D16" else "#FAF0DC"
    val pendingFg = if (dark) "#F4C26B" else "#C87D18"

    val roleLabel = when (uiState.activeRole) {
        UserRole.GUEST -> "Guest Mode (Login required to buy)"
        else -> "${uiState.activeUser?.name ?: uiState.activeRole.labelEn} (${uiState.activeRole.badgeCode})"
    }

    val productCardsHtml = uiState.catalogProducts.joinToString("\n") { item ->
        val safeName = escapeHtml(item.product.name)
        val safeBrand = escapeHtml(item.product.brand)
        val safeCategory = escapeHtml(item.product.category)
        val safeFabric = escapeHtml(item.product.fabric)
        val bestMerchant = item.bestOffer?.merchant
        val shopName = escapeHtml(bestMerchant?.name ?: item.nearestMerchantArea)
        val isVerifiedShop = bestMerchant?.isVerified == true
        val shopBadge = if (isVerifiedShop) {
            "<span class='badge verified'>&#10004; Verified Shop</span>"
        } else {
            "<span class='badge pending'>Pending Verification</span>"
        }
        val priceStr = escapeHtml(formatBdt(item.lowestPriceBdt))
        val stockStr = if (item.totalAvailableQty > 0) "${item.totalAvailableQty} in stock" else "Out of stock"
        val ctaLabel = if (uiState.activeRole == UserRole.GUEST) "View &middot; Login to Buy" else "View &amp; Buy"

        """
        <article class="card">
          <div class="card-top">
            <span class="brand">$safeBrand &middot; $safeCategory</span>
            <span class="rating">&#9733; ${item.product.rating}</span>
          </div>
          <h3>$safeName</h3>
          <p class="fabric">$safeFabric</p>
          <div class="meta-row">
            <span class="shop">$shopName</span>
            $shopBadge
          </div>
          <div class="price-row">
            <div>
              <span class="price">$priceStr</span>
              <span class="stock">$stockStr</span>
            </div>
            <button class="btn-buy" type="button">$ctaLabel</button>
          </div>
        </article>
        """.trimIndent()
    }

    val shopCardsHtml = uiState.merchants.filter { !it.isSuspended }.joinToString("\n") { m ->
        val sName = escapeHtml(m.name)
        val sArea = escapeHtml(m.area)
        val vBadge = if (m.isVerified) {
            "<span class='badge verified'>&#10004; Verified</span>"
        } else {
            "<span class='badge pending'>Pending Admin KYC</span>"
        }
        """
        <div class="shop-pill">
          <div class="shop-pill-head"><strong>$sName</strong>$vBadge</div>
          <div class="shop-pill-sub">$sArea &middot; ${escapeHtml(m.storeType)} &middot; &#9733; ${m.rating}</div>
        </div>
        """.trimIndent()
    }

    val themeBtnLabel = if (dark) "Switch to Light Mode" else "Switch to Dark Mode"

    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <title>Porey - পরে Website</title>
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: sans-serif; background: $bg; color: $text; padding: 16px; }
        .topbar { display: flex; justify-content: space-between; align-items: center; padding-bottom: 14px; border-bottom: 1px solid $border; margin-bottom: 16px; }
        .logo { font-size: 24px; font-weight: 800; }
        .logo span { color: $coral; margin-left: 4px; }
        .sub { font-size: 11px; color: $muted; }
        .hero { background: $surfaceAlt; border: 1px solid $border; border-radius: 18px; padding: 18px; margin-bottom: 18px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 12px; }
        .card { background: $surface; border: 1px solid $border; border-radius: 16px; padding: 14px; }
        .badge { font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 999px; }
        .verified { background: $verifiedBg; color: $verifiedFg; }
        .pending { background: $pendingBg; color: $pendingFg; }
        .price { font-size: 17px; font-weight: 800; color: $primary; }
        .btn-buy { background: $primary; color: $primaryText; border: none; border-radius: 999px; padding: 8px 14px; font-size: 12px; font-weight: 700; }
      </style>
    </head>
    <body>
      <header class="topbar">
        <div>
          <div class="logo">Porey<span>পরে</span></div>
          <div class="sub">Website &middot; ${escapeHtml(roleLabel)}</div>
        </div>
        <button type="button">$themeBtnLabel</button>
      </header>
      <section class="hero"><h1>Dhaka Heritage &amp; Contemporary Fashion</h1></section>
      <section class="grid">$productCardsHtml</section>
      <section class="shops-list">$shopCardsHtml</section>
    </body>
    </html>
    """.trimIndent()
}

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
