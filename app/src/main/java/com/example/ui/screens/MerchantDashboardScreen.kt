package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.MerchantListingEntity
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.ReturnStatus
import com.example.domain.model.StockMovementReason
import com.example.domain.model.UserRole
import com.example.domain.model.formatBdt
import com.example.domain.model.formatDhakaTime
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StockConfidenceBadge
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyAmber
import com.example.ui.theme.PoreyAmberSoft
import com.example.ui.theme.PoreyCoral
import com.example.ui.theme.PoreyCoralSoft
import com.example.ui.theme.PoreyEmerald
import com.example.ui.theme.PoreyEmeraldSoft
import java.util.UUID

@Composable
fun MerchantDashboardScreen(
    uiState: PoreyUiState,
    onSendPosWebhook: (merchantId: String, sku: String, qty: Int, idempotencyKey: String, sigValid: Boolean) -> Unit,
    onAdjustStock: (listingId: String, deltaPhysical: Int, deltaDamaged: Int, reason: StockMovementReason, note: String) -> Unit,
    onReconcileEod: (listingId: String, verifiedCount: Int) -> Unit,
    onPublishProduct: (merchantId: String, name: String, brand: String, cat: String, fabric: String, size: String, color: String, sku: String, price: Int, disc: Int?, stock: Int, img: String) -> Unit,
    onTogglePublished: (listingId: String) -> Unit,
    onAdvanceFulfilment: (fulfilmentId: String, status: FulfilmentStatus) -> Unit,
    onReviewReturn: (returnId: String, status: ReturnStatus, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRole = uiState.activeRole
    val defaultMerchantId = when (activeRole) {
        UserRole.NO_POS_MERCHANT -> "mer_mirpur_manual"
        UserRole.ONLINE_MERCHANT -> "mer_dhaka_loom_online"
        else -> uiState.activeUser?.assignedMerchantId ?: "mer_banani_pos"
    }
    val merchant = uiState.merchants.firstOrNull { it.id == defaultMerchantId }
        ?: uiState.merchants.firstOrNull()
        ?: return

    val isEmployee = activeRole == UserRole.STORE_EMPLOYEE
    val isPosShop = activeRole == UserRole.PHYSICAL_MERCHANT || merchant.posProvider != null

    val merchantListings = uiState.allListings.filter { it.merchantId == merchant.id }
    val productsById = uiState.catalogProducts.associate { it.product.id to it.product }
    val merchantFulfilments = uiState.orders.flatMap { it.fulfilmentGroups }
        .filter { it.fulfilment.merchantId == merchant.id }
    val merchantMovements = uiState.stockMovements.filter { it.merchantId == merchant.id }
    val merchantWebhooks = uiState.posWebhookEvents.filter { it.merchantId == merchant.id }
    val merchantReturns = uiState.returnRequests.filter { it.merchantId == merchant.id }

    var selectedTab by remember(activeRole) { mutableIntStateOf(0) }
    var stockSearch by remember { mutableStateOf("") }
    var reconcileTarget by remember { mutableStateOf<MerchantListingEntity?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val tabs = buildList {
        add("Stock")
        add("Orders (${merchantFulfilments.count { it.fulfilment.status == FulfilmentStatus.RESERVED_PENDING_MERCHANT.name }})")
        if (isPosShop) add("POS Adapter")
        if (!isEmployee) add("Sales & Returns")
    }
    if (selectedTab >= tabs.size) selectedTab = 0

    Column(modifier = modifier.fillMaxSize()) {
        // Store Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(merchant.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            if (merchant.isVerified) {
                                Surface(color = PoreyEmeraldSoft, shape = RoundedCornerShape(50)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.Verified, contentDescription = "Verified Shop", tint = PoreyEmerald, modifier = Modifier.size(13.dp))
                                        Text("Verified", style = MaterialTheme.typography.labelSmall, color = PoreyEmerald)
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${activeRole.labelEn} · ${merchant.area}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isEmployee) {
                        Button(
                            onClick = { showAddProductDialog = true },
                            enabled = merchant.isVerified,
                            modifier = Modifier.testTag("add_product_studio_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Product")
                        }
                    }
                }
                if (!merchant.isVerified) {
                    Surface(color = PoreyAmberSoft, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = PoreyAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Pending Admin Verification: A Platform Admin must verify your shop (${merchant.tradeLicenseRef ?: "KYC"}) before your Verified badge appears and you can publish products.",
                                style = MaterialTheme.typography.labelMedium,
                                color = PoreyAmber
                            )
                        }
                    }
                }
                if (isEmployee) {
                    Surface(color = PoreyAmberSoft, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PoreyAmber, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Store Employee RBAC: Stock & Order prep active. Financial & commission tabs restricted.",
                                style = MaterialTheme.typography.labelSmall,
                                color = PoreyAmber
                            )
                        }
                    }
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        when (tabs[selectedTab]) {
            "Stock" -> {
                val filteredListings = merchantListings.filter { l ->
                    val prod = productsById[l.productId]
                    stockSearch.isBlank() ||
                        l.sku.contains(stockSearch, ignoreCase = true) ||
                        (prod?.name?.contains(stockSearch, ignoreCase = true) == true)
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = stockSearch,
                            onValueChange = { stockSearch = it },
                            label = { Text("Search SKU or garment name...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(filteredListings, key = { it.id }) { listing ->
                        val prod = productsById[listing.productId]
                        val available = (listing.physicalQty - listing.reservedQty - listing.damagedQty).coerceAtLeast(0)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stock_card_${listing.sku}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod?.name ?: listing.sku, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = "SKU: ${listing.sku} · Size ${listing.size} · ${listing.color} · ${formatBdt(listing.discountPriceBdt ?: listing.priceBdt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StockConfidenceBadge(listing.stockConfidence)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Physical: ${listing.physicalQty}", style = MaterialTheme.typography.labelMedium)
                                    Text("Reserved: ${listing.reservedQty}", style = MaterialTheme.typography.labelMedium, color = PoreyAmber)
                                    Text("Damaged: ${listing.damagedQty}", style = MaterialTheme.typography.labelMedium, color = PoreyCoral)
                                    Text(
                                        text = "Avail: $available",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (available <= 2) PoreyCoral else PoreyEmerald
                                    )
                                }

                                Text(
                                    text = "Last updated: ${formatDhakaTime(listing.lastUpdatedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onAdjustStock(listing.id, -1, 0, StockMovementReason.MANUAL_SALE, "Quick in-store sale")
                                        },
                                        enabled = available > 0,
                                        modifier = Modifier.weight(1f).testTag("sale_minus_btn_${listing.sku}")
                                    ) { Text("-1 Sale") }

                                    OutlinedButton(
                                        onClick = {
                                            onAdjustStock(listing.id, 1, 0, StockMovementReason.RESTOCK, "Shelf restock")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("+1 Restock") }

                                    OutlinedButton(
                                        onClick = { reconcileTarget = listing },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("EOD Count") }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        onAdjustStock(listing.id, 0, 1, StockMovementReason.DAMAGED, "Marked damaged item")
                                    }) {
                                        Text("Mark 1 Damaged", color = PoreyCoral)
                                    }
                                    if (!isEmployee) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Published", style = MaterialTheme.typography.labelMedium)
                                            Spacer(Modifier.width(6.dp))
                                            Switch(
                                                checked = listing.isPublished,
                                                onCheckedChange = { onTogglePublished(listing.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Recent Stock Movements", style = MaterialTheme.typography.titleMedium)
                    }
                    items(merchantMovements.take(10), key = { it.id }) { mov ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${mov.sku} · ${mov.reason}", style = MaterialTheme.typography.labelLarge)
                                    Text(mov.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "ΔPhys ${mov.deltaPhysical} / ΔRes ${mov.deltaReserved}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            "POS Adapter" -> {
                var skuInput by remember(merchant.id) {
                    mutableStateOf(merchantListings.firstOrNull()?.sku ?: "DL-PNJ-IVR-M")
                }
                var qtyInput by remember { mutableStateOf("1") }
                var idempKey by remember { mutableStateOf("POS-${UUID.randomUUID().toString().take(6).uppercase()}") }
                var validSignature by remember { mutableStateOf(true) }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, tint = PoreyEmerald)
                                    Column {
                                        Text("${merchant.posProvider ?: "RetailBD POS"} · Sandbox Adapter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Status: ${merchant.posStatus} · Last sync: ${formatDhakaTime(merchant.lastSyncedAt)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(
                                    text = "Simulates an authenticated POS webhook POST when a cashier scans a sale in-store. Verifies HMAC signature & idempotency key to prevent duplicate deductions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    merchantListings.take(3).forEach { l ->
                                        FilterChip(
                                            selected = skuInput == l.sku,
                                            onClick = { skuInput = l.sku },
                                            label = { Text(l.sku) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = skuInput,
                                    onValueChange = { skuInput = it },
                                    label = { Text("POS SKU") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = qtyInput,
                                        onValueChange = { qtyInput = it.filter { c -> c.isDigit() } },
                                        label = { Text("Qty Sold") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.35f)
                                    )
                                    OutlinedTextField(
                                        value = idempKey,
                                        onValueChange = { idempKey = it },
                                        label = { Text("Idempotency Key") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.65f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Valid HMAC-SHA256 Signature", style = MaterialTheme.typography.labelLarge)
                                    Switch(checked = validSignature, onCheckedChange = { validSignature = it })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val q = qtyInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                            onSendPosWebhook(merchant.id, skuInput, q, idempKey, validSignature)
                                        },
                                        modifier = Modifier.weight(1f).testTag("send_pos_webhook_btn")
                                    ) {
                                        Text("Dispatch POS Event")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            idempKey = "POS-${UUID.randomUUID().toString().take(6).uppercase()}"
                                        }
                                    ) {
                                        Text("New Key")
                                    }
                                }
                            }
                        }
                    }

                    item { Text("Webhook Event Audit Log", style = MaterialTheme.typography.titleMedium) }
                    items(merchantWebhooks, key = { it.id }) { ev ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${ev.sku} (x${ev.qtySold})", style = MaterialTheme.typography.labelLarge)
                                    Surface(
                                        color = if (ev.status == "PROCESSED") PoreyEmeraldSoft else PoreyCoralSoft,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = ev.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (ev.status == "PROCESSED") PoreyEmerald else PoreyCoral,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Key: ${ev.idempotencyKey} · Sig: ${ev.signatureVerified}", style = MaterialTheme.typography.labelSmall)
                                ev.errorMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = PoreyCoral) }
                            }
                        }
                    }
                }
            }

            "Sales & Returns" -> {
                val totalSales = merchantFulfilments
                    .filter { it.fulfilment.status != FulfilmentStatus.CANCELLED.name }
                    .sumOf { it.fulfilment.subtotalBdt }
                val netPayable = merchantFulfilments
                    .filter { it.fulfilment.status == FulfilmentStatus.DELIVERED.name }
                    .sumOf { it.fulfilment.merchantPayableBdt }
                val lowStockCount = merchantListings.count { (it.physicalQty - it.reservedQty - it.damagedQty) <= 3 }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricStatCard(
                                label = "Gross Sales",
                                value = formatBdt(totalSales),
                                subtitle = "${merchantFulfilments.size} orders",
                                icon = Icons.Outlined.ReceiptLong,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "Net Payable",
                                value = formatBdt(netPayable),
                                subtitle = "${merchant.commissionRatePct}% commission",
                                icon = Icons.Outlined.AccountBalanceWallet,
                                accentColor = PoreyEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricStatCard(
                                label = "Low Stock SKUs",
                                value = "$lowStockCount",
                                subtitle = "Threshold <= 3 units",
                                icon = Icons.Outlined.WarningAmber,
                                accentColor = PoreyCoral,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "Active Listings",
                                value = "${merchantListings.count { it.isPublished }}",
                                subtitle = "${merchantListings.size} total SKUs",
                                icon = Icons.Outlined.Inventory2,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item { Text("Return & Exchange Requests (${merchantReturns.size})", style = MaterialTheme.typography.titleMedium) }
                    items(merchantReturns, key = { it.id }) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${req.requestType} · ${req.status}", style = MaterialTheme.typography.titleMedium)
                                Text(req.reason, style = MaterialTheme.typography.bodyMedium)
                                if (req.status == ReturnStatus.REQUESTED.name) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onReviewReturn(req.id, ReturnStatus.MERCHANT_APPROVED, "Approved for collection") }) {
                                            Text("Approve")
                                        }
                                        OutlinedButton(onClick = { onReviewReturn(req.id, ReturnStatus.MERCHANT_REJECTED, "Outside condition policy") }) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Orders tab
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (merchantFulfilments.isEmpty()) {
                        item {
                            Text(
                                text = "No online orders assigned to ${merchant.name} yet. Place a customer order in Cart to test live reservation & merchant confirmation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(merchantFulfilments, key = { it.fulfilment.id }) { group ->
                        val f = group.fulfilment
                        val status = FulfilmentStatus.entries.firstOrNull { it.name == f.status }
                            ?: FulfilmentStatus.RESERVED_PENDING_MERCHANT
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Fulfilment #${f.id.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(status.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                group.items.forEach { oi ->
                                    Text("• ${oi.quantity}x ${oi.productName} (${oi.size}, ${oi.color})", style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider()
                                when (status) {
                                    FulfilmentStatus.RESERVED_PENDING_MERCHANT -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.CONFIRMED_PREPARING) },
                                                modifier = Modifier.weight(1f).testTag("merchant_confirm_order_btn")
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Confirm Stock")
                                            }
                                            OutlinedButton(
                                                onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.CANCELLED) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Reject")
                                            }
                                        }
                                    }
                                    FulfilmentStatus.CONFIRMED_PREPARING -> {
                                        Button(
                                            onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.READY_FOR_PICKUP) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Mark Ready for Rider Pickup")
                                        }
                                    }
                                    else -> {
                                        Text("Rider Status: ${f.riderName ?: "Awaiting Dispatch"}", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // End-of-day reconciliation dialog
    if (reconcileTarget != null) {
        val target = reconcileTarget!!
        var countText by remember(target.id) { mutableStateOf(target.physicalQty.toString()) }
        AlertDialog(
            onDismissRequest = { reconcileTarget = null },
            title = { Text("End-of-Day Stock Reconciliation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SKU: ${target.sku} (Reserved: ${target.reservedQty})")
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() } },
                        label = { Text("Verified Physical Shelf Count") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val v = countText.toIntOrNull() ?: target.physicalQty
                    onReconcileEod(target.id, v)
                    reconcileTarget = null
                }) { Text("Save Verified Count") }
            },
            dismissButton = {
                TextButton(onClick = { reconcileTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Online Merchant Product Studio Dialog
    if (showAddProductDialog) {
        var pName by remember { mutableStateOf("") }
        var pBrand by remember { mutableStateOf("Dhaka Loom") }
        var pCategory by remember { mutableStateOf("Women") }
        var pFabric by remember { mutableStateOf("100% Organic Cotton") }
        var pSize by remember { mutableStateOf("M") }
        var pColor by remember { mutableStateOf("Sage Green") }
        var pSku by remember { mutableStateOf("DLA-${(100..999).random()}") }
        var pPrice by remember { mutableStateOf("2400") }
        var pStock by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Publish New Fashion Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Product Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pBrand, onValueChange = { pBrand = it }, label = { Text("Brand") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pCategory, onValueChange = { pCategory = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pSize, onValueChange = { pSize = it }, label = { Text("Size") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pColor, onValueChange = { pColor = it }, label = { Text("Color") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pPrice, onValueChange = { pPrice = it.filter { c -> c.isDigit() } }, label = { Text("Price (BDT)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pStock, onValueChange = { pStock = it.filter { c -> c.isDigit() } }, label = { Text("Stock") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = pSku, onValueChange = { pSku = it }, label = { Text("SKU") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPublishProduct(
                        merchant.id,
                        pName,
                        pBrand,
                        pCategory,
                        pFabric,
                        pSize,
                        pColor,
                        pSku,
                        pPrice.toIntOrNull() ?: 2400,
                        null,
                        pStock.toIntOrNull() ?: 10,
                        "saree"
                    )
                    showAddProductDialog = false
                }) { Text("Publish") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Cancel") }
            }
        )
    }
}
