package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.PosConnectionStatus
import com.example.domain.model.ReturnStatus
import com.example.domain.model.formatBdt
import com.example.domain.model.formatDhakaTime
import com.example.ui.components.MetricStatCard
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyAmber
import com.example.ui.theme.PoreyAmberSoft
import com.example.ui.theme.PoreyCoral
import com.example.ui.theme.PoreyCoralSoft
import com.example.ui.theme.PoreyEmerald
import com.example.ui.theme.PoreyEmeraldSoft

@Composable
fun AdminDashboardScreen(
    uiState: PoreyUiState,
    onToggleMerchantVerified: (merchantId: String) -> Unit,
    onToggleMerchantSuspended: (merchantId: String) -> Unit,
    onUpdateCommission: (merchantId: String, pct: Int) -> Unit,
    onSettleMerchant: (merchantId: String, amountBdt: Int) -> Unit,
    onReviewReturn: (returnId: String, status: ReturnStatus, note: String) -> Unit,
    onToggleZone: (zoneId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Merchants", "Disputes & Zones", "Ledger & Audit")

    val allFulfilments = uiState.orders.flatMap { it.fulfilmentGroups }.map { it.fulfilment }
    val gmv = uiState.orders.sumOf { it.order.grandTotalBdt }
    val commissions = allFulfilments
        .filter { it.status != FulfilmentStatus.CANCELLED.name }
        .sumOf { it.commissionBdt }
    val verifiedCount = uiState.merchants.count { it.isVerified && !it.isSuspended }
    val syncAlerts = uiState.merchants.count {
        it.posStatus == PosConnectionStatus.SYNC_DELAYED.name || it.posStatus == PosConnectionStatus.DISCONNECTED.name
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = "${uiState.activeRole.labelEn} Control Panel",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Governance, merchant verification, commissions, zones & audit logs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricStatCard(
                                label = "Platform GMV",
                                value = formatBdt(gmv),
                                subtitle = "${uiState.orders.size} customer orders",
                                icon = Icons.Outlined.AccountBalance,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "Commission Rev",
                                value = formatBdt(commissions),
                                subtitle = "${allFulfilments.size} fulfilment groups",
                                icon = Icons.Outlined.AdminPanelSettings,
                                accentColor = PoreyEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricStatCard(
                                label = "Verified Shops",
                                value = "$verifiedCount / ${uiState.merchants.size}",
                                subtitle = "${uiState.catalogProducts.size} catalog products",
                                icon = Icons.Outlined.Storefront,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "POS Alerts",
                                value = "$syncAlerts",
                                subtitle = "${uiState.posWebhookEvents.size} webhook events",
                                icon = Icons.Outlined.WarningAmber,
                                accentColor = if (syncAlerts > 0) PoreyCoral else PoreyEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Integration & Gateway Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("• POS Webhook Engine: Active (HMAC + Idempotency)", style = MaterialTheme.typography.bodyMedium)
                                Text("• Payment Adapters: COD Live · bKash / Nagad / SSLCommerz Sandbox", style = MaterialTheme.typography.bodyMedium)
                                Text("• Storage Engine: Local Acid-Verified Room SQLite (Free Tier)", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            1 -> {
                val pendingMerchants = uiState.merchants.filter { !it.isVerified && !it.isSuspended }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pendingMerchants.isNotEmpty()) {
                        item {
                            Surface(
                                color = PoreyAmberSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = PoreyAmber)
                                    Text(
                                        text = "${pendingMerchants.size} shop(s) awaiting Admin KYC verification before receiving the Verified badge.",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PoreyAmber
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.merchants.sortedBy { it.isVerified }, key = { it.id }) { m ->
                        val merchantPayable = allFulfilments
                            .filter { it.merchantId == m.id && it.status == FulfilmentStatus.DELIVERED.name }
                            .sumOf { it.merchantPayableBdt }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                width = if (!m.isVerified && !m.isSuspended) 2.dp else 1.dp,
                                color = if (!m.isVerified && !m.isSuspended) PoreyAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(m.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            if (m.isVerified) {
                                                Icon(Icons.Filled.Verified, contentDescription = "Verified Shop", tint = PoreyEmerald, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text("${m.area} · ${m.storeType} · ${m.commissionRatePct}% fee", style = MaterialTheme.typography.labelSmall)
                                        if (!m.tradeLicenseRef.isNullOrBlank()) {
                                            Text("KYC / Trade Ref: ${m.tradeLicenseRef}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Surface(
                                        color = when {
                                            m.isSuspended -> PoreyCoralSoft
                                            m.isVerified -> PoreyEmeraldSoft
                                            else -> PoreyAmberSoft
                                        },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = when {
                                                m.isSuspended -> "Suspended"
                                                m.isVerified -> "Verified ✓"
                                                else -> "Pending KYC"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                m.isSuspended -> PoreyCoral
                                                m.isVerified -> PoreyEmerald
                                                else -> PoreyAmber
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!m.isVerified) {
                                        Button(
                                            onClick = { onToggleMerchantVerified(m.id) },
                                            modifier = Modifier.weight(1f).testTag("admin_verify_${m.id}")
                                        ) {
                                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Verify Shop")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onToggleMerchantVerified(m.id) },
                                            modifier = Modifier.weight(1f).testTag("admin_verify_${m.id}")
                                        ) {
                                            Text("Unverify")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onToggleMerchantSuspended(m.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (m.isSuspended) "Reinstate" else "Suspend",
                                            color = if (m.isSuspended) PoreyEmerald else PoreyCoral
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Commission:", style = MaterialTheme.typography.labelMedium)
                                        OutlinedButton(onClick = { onUpdateCommission(m.id, (m.commissionRatePct - 1).coerceAtLeast(1)) }) { Text("-1%") }
                                        Text("${m.commissionRatePct}%", style = MaterialTheme.typography.labelLarge)
                                        OutlinedButton(onClick = { onUpdateCommission(m.id, (m.commissionRatePct + 1).coerceAtMost(30)) }) { Text("+1%") }
                                    }
                                    if (merchantPayable > 0) {
                                        Button(onClick = { onSettleMerchant(m.id, merchantPayable) }) {
                                            Text("Settle ${formatBdt(merchantPayable)}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Text("Delivery Service Zones", style = MaterialTheme.typography.titleLarge) }
                    items(uiState.deliveryZones, key = { it.id }) { z ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${z.name} · ${formatBdt(z.baseChargeBdt)}", style = MaterialTheme.typography.titleMedium)
                                    Text("${z.areas} · ${z.estimatedWindow}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = z.isActive, onCheckedChange = { onToggleZone(z.id) })
                            }
                        }
                    }

                    item { Text("Return & Dispute Resolution (${uiState.returnRequests.size})", style = MaterialTheme.typography.titleLarge) }
                    items(uiState.returnRequests, key = { it.id }) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${req.requestType} · ${req.status}", style = MaterialTheme.typography.titleMedium)
                                Text(req.reason, style = MaterialTheme.typography.bodyMedium)
                                if (req.status != ReturnStatus.REFUNDED_CLOSED.name) {
                                    Button(onClick = {
                                        onReviewReturn(req.id, ReturnStatus.REFUNDED_CLOSED, "Admin authorized refund/exchange settlement")
                                    }) {
                                        Text("Authorize Refund & Close")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("Financial Ledger (${uiState.financialLedger.size})", style = MaterialTheme.typography.titleLarge) }
                    items(uiState.financialLedger.take(20), key = { it.id }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${entry.entryType} · ${entry.status}", style = MaterialTheme.typography.labelLarge)
                                    Text(entry.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatBdt(entry.amountBdt), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item {
                        HorizontalDivider()
                        Text("Immutable Audit Logs", style = MaterialTheme.typography.titleLarge)
                    }
                    items(uiState.auditLogs.take(20), key = { it.id }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${log.action} · ${log.targetEntity}", style = MaterialTheme.typography.labelLarge)
                                Text("${log.actorRole} · ${log.details} · ${formatDhakaTime(log.createdAt)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
