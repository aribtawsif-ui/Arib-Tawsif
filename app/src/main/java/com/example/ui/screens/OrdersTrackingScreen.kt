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
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TwoWheeler
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
import com.example.domain.model.PaymentStatus
import com.example.domain.model.ReturnRequestType
import com.example.domain.model.ReturnStatus
import com.example.domain.model.formatBdt
import com.example.domain.model.formatDhakaTime
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.FulfilmentTimelineBar
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyCoral
import com.example.ui.theme.PoreyCoralSoft
import com.example.ui.theme.PoreyEmerald
import com.example.ui.theme.PoreyEmeraldSoft

@Composable
fun OrdersTrackingScreen(
    uiState: PoreyUiState,
    onCancelFulfilment: (fulfilmentId: String) -> Unit,
    onSubmitReturn: (fulfilmentId: String, type: ReturnRequestType, reason: String) -> Unit,
    onBrowseShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var returnTargetFulfilmentId by remember { mutableStateOf<String?>(null) }

    if (uiState.orders.isEmpty()) {
        Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            EmptyStatePanel(
                icon = Icons.Outlined.ReceiptLong,
                title = "No Orders Yet",
                message = "Once you place an order, track each merchant's fulfilment, rider pickup, and delivery status in real time here.",
                actionLabel = "Discover Clothing",
                onAction = onBrowseShop
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Orders & Live Tracking", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Track per-merchant fulfilment, rider telemetry, and returns/exchanges.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(uiState.orders, key = { it.order.id }) { orderDetail ->
            val order = orderDetail.order
            val paymentStatus = PaymentStatus.entries.firstOrNull { it.name == order.paymentStatus }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_card_${order.orderCode}"),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = order.orderCode,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = formatDhakaTime(order.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatBdt(order.grandTotalBdt),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${order.deliveryZone} · ${order.deliveryAddress}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Payment: ${paymentStatus?.label ?: order.paymentStatus}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    orderDetail.fulfilmentGroups.forEach { group ->
                        val f = group.fulfilment
                        val status = FulfilmentStatus.entries.firstOrNull { it.name == f.status }
                            ?: FulfilmentStatus.RESERVED_PENDING_MERCHANT
                        val relatedReturn = uiState.returnRequests.firstOrNull { it.fulfilmentOrderId == f.id }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(f.merchantName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Surface(
                                color = if (status == FulfilmentStatus.DELIVERED) PoreyEmeraldSoft else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (status == FulfilmentStatus.DELIVERED) PoreyEmerald else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        FulfilmentTimelineBar(f.status)

                        group.items.forEach { item ->
                            Text(
                                text = "• ${item.quantity}x ${item.productName} (${item.size}, ${item.color}) — ${formatBdt(item.unitPriceBdt * item.quantity)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (f.riderName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Rider: ${f.riderName}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (f.isLocationSharingActive && f.riderLat != null && f.riderLng != null) {
                            Surface(color = PoreyEmeraldSoft, shape = RoundedCornerShape(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PoreyEmerald, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Live Rider Location Active: ${f.riderLat}, ${f.riderLng} (Dhaka Metro)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PoreyEmerald
                                    )
                                }
                            }
                        }

                        if (f.proofOfDeliveryNote != null) {
                            Text(
                                text = "Proof of Delivery: ${f.proofOfDeliveryNote}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PoreyEmerald
                            )
                        }

                        if (relatedReturn != null) {
                            val rStatus = ReturnStatus.entries.firstOrNull { it.name == relatedReturn.status }
                            Surface(color = PoreyCoralSoft, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${relatedReturn.requestType}: ${rStatus?.label ?: relatedReturn.status} · ${relatedReturn.reason}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PoreyCoral,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (status == FulfilmentStatus.RESERVED_PENDING_MERCHANT || status == FulfilmentStatus.CONFIRMED_PREPARING) {
                                OutlinedButton(onClick = { onCancelFulfilment(f.id) }) {
                                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cancel & Release Stock")
                                }
                            }
                            if (status == FulfilmentStatus.DELIVERED && relatedReturn == null) {
                                OutlinedButton(onClick = { returnTargetFulfilmentId = f.id }) {
                                    Icon(Icons.Outlined.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Return / Exchange")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (returnTargetFulfilmentId != null) {
        var reqType by remember { mutableStateOf(ReturnRequestType.EXCHANGE) }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { returnTargetFulfilmentId = null },
            title = { Text("Request Return or Exchange") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReturnRequestType.entries.forEach { t ->
                            FilterChip(
                                selected = reqType == t,
                                onClick = { reqType = t },
                                label = { Text(t.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason (e.g., Need size L instead of M)") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    returnTargetFulfilmentId?.let { id -> onSubmitReturn(id, reqType, reason) }
                    returnTargetFulfilmentId = null
                }) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { returnTargetFulfilmentId = null }) { Text("Cancel") }
            }
        )
    }
}
