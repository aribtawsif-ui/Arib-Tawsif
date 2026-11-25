package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.PaymentMethod
import com.example.domain.model.UserRole
import com.example.domain.model.formatBdt
import com.example.domain.model.productDrawableRes
import com.example.ui.components.EmptyStatePanel
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyAmber
import com.example.ui.theme.PoreyAmberSoft
import com.example.ui.theme.PoreyEmerald

@Composable
fun CartCheckoutScreen(
    uiState: PoreyUiState,
    onUpdateQty: (cartItemId: String, delta: Int) -> Unit,
    onRemoveItem: (cartItemId: String) -> Unit,
    onSelectZone: (zoneId: String) -> Unit,
    onPromoChange: (String) -> Unit,
    onPlaceOrder: (address: String, paymentMethod: PaymentMethod) -> Unit,
    onBrowseShop: () -> Unit,
    onLoginAsCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = uiState.checkoutSummary
    var selectedAddress by remember(uiState.savedAddresses) {
        mutableStateOf(
            uiState.savedAddresses.firstOrNull { it.isDefault }?.addressLine
                ?: uiState.savedAddresses.firstOrNull()?.addressLine
                ?: "House 24, Road 11, Banani, Dhaka 1213"
        )
    }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }

    if (uiState.isGuest || uiState.activeRole == UserRole.GUEST) {
        Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            EmptyStatePanel(
                icon = Icons.Outlined.ShoppingBag,
                title = "Customer Login Required to Buy",
                message = "You are currently browsing as a Guest. Sign in or register as a Customer to add garments to your bag and place orders.",
                actionLabel = "Login / Register as Customer",
                onAction = onLoginAsCustomer
            )
        }
        return
    }

    if (summary.groups.isEmpty()) {
        Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            EmptyStatePanel(
                icon = Icons.Outlined.ShoppingBag,
                title = "Your Porey Bag is Empty",
                message = "Compare nearby Dhaka shops or online brands and reserve authentic garments with real-time stock checks.",
                actionLabel = "Browse Fashion",
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
            Text("Shopping Bag & Checkout", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = if (summary.groups.size > 1) {
                    "Multi-merchant basket: split transparently into ${summary.groups.size} fulfilment groups."
                } else {
                    "Single-merchant fulfilment · Stock verified atomically on checkout."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Merchant Fulfilment Groups
        items(summary.groups, key = { it.merchant.id }) { group ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(group.merchant.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("${group.merchant.area} · ${group.estimatedWindow}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = "Delivery ${formatBdt(group.deliveryChargeBdt)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    group.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = productDrawableRes(item.product.imageKey)),
                                contentDescription = item.product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Size ${item.listing.size} · ${item.listing.color} · ${formatBdt(item.unitPriceBdt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${item.availableQty} available right now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PoreyEmerald
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onUpdateQty(item.cartItem.id, -1) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                }
                                Text("${item.cartItem.quantity}", style = MaterialTheme.typography.titleMedium)
                                IconButton(
                                    onClick = { onUpdateQty(item.cartItem.id, 1) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                }
                                IconButton(
                                    onClick = { onRemoveItem(item.cartItem.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delivery Zone & Address
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Delivery Zone & Address", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.deliveryZones.filter { it.isActive }.forEach { zone ->
                            FilterChip(
                                selected = uiState.selectedZoneId == zone.id,
                                onClick = { onSelectZone(zone.id) },
                                label = { Text("${zone.name.substringBefore(" ")} (${formatBdt(zone.baseChargeBdt)})") }
                            )
                        }
                    }

                    if (uiState.savedAddresses.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.savedAddresses.forEach { addr ->
                                FilterChip(
                                    selected = selectedAddress == addr.addressLine,
                                    onClick = { selectedAddress = addr.addressLine },
                                    label = { Text(addr.label) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedAddress,
                        onValueChange = { selectedAddress = it },
                        label = { Text("Dhaka / Bangladesh Delivery Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_input")
                    )
                }
            }
        }

        // Promo Code & Payment Method
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.promoCode,
                        onValueChange = onPromoChange,
                        label = { Text("Promo Code (try POREY10 or DHAKA50)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_promo_input")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Payment Method", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    PaymentMethod.entries.forEach { method ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPayment == method,
                                onClick = { selectedPayment = method }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method.label, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = method.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (selectedPayment.isSandboxGateway) {
                        Surface(
                            color = PoreyAmberSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = PoreyAmber)
                                Text(
                                    text = "Sandbox Gateway Adapter: Orders placed via ${selectedPayment.label} are marked Sandbox Authorized pending official merchant API/webhook credentials.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PoreyAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // Order Summary & Return Policy Disclosure
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Order Summary", style = MaterialTheme.typography.titleLarge)
                    SummaryLine("Items Subtotal", formatBdt(summary.subtotalBdt))
                    SummaryLine("Delivery (${summary.groups.size} merchant group)", formatBdt(summary.deliveryTotalBdt))
                    if (summary.discountBdt > 0) {
                        SummaryLine("Promo Discount", "-${formatBdt(summary.discountBdt)}", highlight = true)
                    }
                    HorizontalDivider()
                    SummaryLine("Total Payable", formatBdt(summary.grandTotalBdt), bold = true)
                    Text(
                        text = "Return Policy: Unused garments with intact tags qualify for 7-day return or size exchange through Porey resolution.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { onPlaceOrder(selectedAddress, selectedPayment) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("place_order_button")
                    ) {
                        Text(
                            text = "Confirm & Reserve Stock · ${formatBdt(summary.grandTotalBdt)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium,
            color = when {
                highlight -> PoreyEmerald
                bold -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
