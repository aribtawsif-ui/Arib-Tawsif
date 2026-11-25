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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.formatBdt
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MetricStatCard
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyCoral
import com.example.ui.theme.PoreyEmerald

@Composable
fun RiderDashboardScreen(
    uiState: PoreyUiState,
    onToggleOnline: () -> Unit,
    onAdvanceFulfilment: (fulfilmentId: String, status: FulfilmentStatus, proofNote: String?) -> Unit,
    onToggleLocationSharing: (fulfilmentId: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val allJobs = uiState.orders.flatMap { order ->
        order.fulfilmentGroups.map { order.order to it }
    }
    val openJobs = allJobs.filter { it.second.fulfilment.status == FulfilmentStatus.READY_FOR_PICKUP.name }
    val activeJobs = allJobs.filter {
        it.second.fulfilment.status in setOf(
            FulfilmentStatus.RIDER_ASSIGNED.name,
            FulfilmentStatus.PICKED_UP.name,
            FulfilmentStatus.OUT_FOR_DELIVERY.name
        )
    }
    val completedJobs = allJobs.filter { it.second.fulfilment.status == FulfilmentStatus.DELIVERED.name }
    val earningsBdt = completedJobs.sumOf { (it.second.fulfilment.deliveryFeeBdt * 0.8).toInt() }

    var podJobId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Porey Rider Console", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = if (uiState.isRiderOnline) "Online · Dhaka Fleet Dispatch Active" else "Offline · Not receiving dispatches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isRiderOnline) PoreyEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isRiderOnline,
                        onCheckedChange = { onToggleOnline() },
                        modifier = Modifier.testTag("rider_online_switch")
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard(
                    label = "Completed",
                    value = "${completedJobs.size}",
                    subtitle = "${activeJobs.size} active now",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = PoreyEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    label = "Rider Earnings",
                    value = formatBdt(earningsBdt),
                    subtitle = "80% delivery share",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (openJobs.isEmpty() && activeJobs.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.TwoWheeler,
                    title = "No Active Dispatch Jobs",
                    message = "Orders marked 'Ready for Rider Pickup' by Dhaka merchants will appear here automatically."
                )
            }
        }

        if (openJobs.isNotEmpty()) {
            item { Text("Available Pickup Assignments (${openJobs.size})", style = MaterialTheme.typography.titleLarge) }
            items(openJobs, key = { it.second.fulfilment.id }) { (order, group) ->
                val f = group.fulfilment
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pickup: ${f.merchantName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(formatBdt(f.deliveryFeeBdt), style = MaterialTheme.typography.titleMedium, color = PoreyEmerald)
                        }
                        Text("Drop-off: ${order.deliveryAddress}", style = MaterialTheme.typography.bodyMedium)
                        Text("${group.items.sumOf { it.quantity }} garment(s) · Order ${order.orderCode}", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.RIDER_ASSIGNED, null) },
                                enabled = uiState.isRiderOnline,
                                modifier = Modifier.weight(1f).testTag("rider_accept_btn_${f.id}")
                            ) {
                                Text("Accept Dispatch")
                            }
                            OutlinedButton(
                                onClick = { /* Reject returns job to pool */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pass")
                            }
                        }
                    }
                }
            }
        }

        if (activeJobs.isNotEmpty()) {
            item { Text("Active Deliveries (${activeJobs.size})", style = MaterialTheme.typography.titleLarge) }
            items(activeJobs, key = { it.second.fulfilment.id }) { (order, group) ->
                val f = group.fulfilment
                val status = FulfilmentStatus.entries.firstOrNull { it.name == f.status }
                    ?: FulfilmentStatus.RIDER_ASSIGNED
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${order.orderCode} · ${status.label}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("From: ${f.merchantName}", style = MaterialTheme.typography.bodyMedium)
                        Text("To: ${order.customerName} · ${order.deliveryAddress}", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()

                        if (status == FulfilmentStatus.PICKED_UP || status == FulfilmentStatus.OUT_FOR_DELIVERY) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PoreyEmerald)
                                    Text("Share Active Location", style = MaterialTheme.typography.labelLarge)
                                }
                                Switch(
                                    checked = f.isLocationSharingActive,
                                    onCheckedChange = { onToggleLocationSharing(f.id, it) }
                                )
                            }
                        }

                        when (status) {
                            FulfilmentStatus.RIDER_ASSIGNED -> {
                                Button(
                                    onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.PICKED_UP, "Parcel sealed & collected") },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Confirm Store Pickup") }
                            }
                            FulfilmentStatus.PICKED_UP -> {
                                Button(
                                    onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.OUT_FOR_DELIVERY, null) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Start Route (Out for Delivery)") }
                            }
                            FulfilmentStatus.OUT_FOR_DELIVERY -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { podJobId = f.id },
                                        modifier = Modifier.weight(1f).testTag("rider_complete_pod_btn")
                                    ) { Text("Proof of Delivery") }
                                    OutlinedButton(
                                        onClick = { onAdvanceFulfilment(f.id, FulfilmentStatus.FAILED_DELIVERY, "Customer unreachable") },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Mark Failed", color = PoreyCoral) }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    if (podJobId != null) {
        var podNote by remember { mutableStateOf("Received by ${uiState.orders.firstOrNull()?.order?.customerName ?: "Customer"} · Cash/MFS verified") }
        AlertDialog(
            onDismissRequest = { podJobId = null },
            title = { Text("Upload Proof of Delivery") },
            text = {
                OutlinedTextField(
                    value = podNote,
                    onValueChange = { podNote = it },
                    label = { Text("Recipient / OTP / Photo Reference Note") },
                    minLines = 2
                )
            },
            confirmButton = {
                Button(onClick = {
                    podJobId?.let { id -> onAdvanceFulfilment(id, FulfilmentStatus.DELIVERED, podNote) }
                    podJobId = null
                }) { Text("Complete Delivery") }
            },
            dismissButton = {
                TextButton(onClick = { podJobId = null }) { Text("Cancel") }
            }
        )
    }
}
