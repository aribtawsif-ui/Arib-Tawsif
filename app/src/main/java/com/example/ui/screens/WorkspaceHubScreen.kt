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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.domain.model.UserRole
import com.example.domain.model.formatDhakaTime
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyCoral
import com.example.ui.theme.PoreyCoralSoft
import com.example.ui.theme.PoreyEmerald
import com.example.ui.theme.PoreyEmeraldSoft

@Composable
fun WorkspaceHubScreen(
    uiState: PoreyUiState,
    onSwitchRole: (UserRole) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleBangla: () -> Unit,
    onSignOut: () -> Unit,
    onAddAddress: (label: String, address: String, zone: String, phone: String) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onOpenProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddAddressDialog by remember { mutableStateOf(false) }

    val roleDescriptions = mapOf(
        UserRole.GUEST to "Browse stores & Web View freely (Customer login required to buy)",
        UserRole.CUSTOMER to "Shop fashion, compare stores, track orders & returns",
        UserRole.PHYSICAL_MERCHANT to "POS-connected store (RetailBD/ShopUp webhook sync)",
        UserRole.NO_POS_MERCHANT to "Lightweight manual stock & End-of-Day reconciliation",
        UserRole.ONLINE_MERCHANT to "Online brand studio, catalog publishing & analytics",
        UserRole.STORE_EMPLOYEE to "Restricted floor staff (financials blocked by RBAC)",
        UserRole.RIDER to "Delivery assignments, active location & Proof of Delivery",
        UserRole.ADMIN to "Verify shops, zones, disputes, commissions & audit trail",
        UserRole.SUPER_ADMIN to "Full platform governance & fee configuration"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.padding(12.dp).size(28.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isGuest) "Guest Visitor" else (uiState.activeUser?.name ?: "Porey Member"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${uiState.activeRole.labelEn} · ${uiState.activeRole.labelBn}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PoreyCoral
                            )
                            Text(
                                text = if (uiState.isGuest) "Sign in as Customer to checkout" else "${uiState.activeUser?.phone ?: "+880 1711-440219"} · ${uiState.activeUser?.email ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (uiState.isDarkMode) "Dark Mode Active" else "Light Mode Active",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Switch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            modifier = Modifier.testTag("workspace_dark_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Bangla Bilingual Labels (বাংলা)", style = MaterialTheme.typography.labelLarge)
                        }
                        Switch(checked = uiState.showBanglaSubtitles, onCheckedChange = { onToggleBangla() })
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isGuest) "Sign In / Register (Customer or Shop Owner)" else "Sign Out / Switch Account")
                    }
                }
            }
        }

        // Ecosystem Role Switcher
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Switch Ecosystem Role (Demo RBAC)", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Operate every side of Porey with real backend role permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(UserRole.entries) { role ->
            val active = uiState.activeRole == role
            val icon = when (role) {
                UserRole.GUEST -> Icons.Outlined.Visibility
                UserRole.CUSTOMER -> Icons.Outlined.Person
                UserRole.PHYSICAL_MERCHANT -> Icons.Outlined.PointOfSale
                UserRole.NO_POS_MERCHANT, UserRole.ONLINE_MERCHANT -> Icons.Outlined.Storefront
                UserRole.STORE_EMPLOYEE -> Icons.Outlined.Badge
                UserRole.RIDER -> Icons.Outlined.TwoWheeler
                UserRole.ADMIN, UserRole.SUPER_ADMIN -> Icons.Outlined.AdminPanelSettings
            }
            Card(
                onClick = { onSwitchRole(role) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("switch_role_${role.name}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (active) 2.dp else 1.dp,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${role.labelEn} (${role.labelBn})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(roleDescriptions[role].orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (active) {
                        Surface(color = PoreyEmeraldSoft, shape = RoundedCornerShape(50)) {
                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = PoreyEmerald, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        // Saved Addresses
        if (!uiState.isGuest) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Delivery Addresses", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { showAddAddressDialog = true }) {
                        Icon(Icons.Outlined.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            items(uiState.savedAddresses, key = { it.id }) { addr ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${addr.label} · ${addr.zoneName}", style = MaterialTheme.typography.titleMedium)
                        Text(addr.addressLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Wishlist
        if (uiState.wishlistProductIds.isNotEmpty()) {
            item { Text("Saved Wishlist (${uiState.wishlistProductIds.size})", style = MaterialTheme.typography.titleLarge) }
            val savedProducts = uiState.catalogProducts.filter { it.product.id in uiState.wishlistProductIds }
            items(savedProducts, key = { "w_${it.product.id}" }) { item ->
                Card(
                    onClick = { onOpenProduct(item.product.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = PoreyCoral)
                            Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("View", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Notifications
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                    Text("Notifications (${uiState.notifications.size})", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onMarkNotificationsRead) { Text("Mark Read") }
            }
        }

        items(uiState.notifications.take(12), key = { it.id }) { notif ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (!notif.isRead) PoreyCoralSoft.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(notif.title, style = MaterialTheme.typography.titleMedium)
                        Text(notif.targetRole, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(notif.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDhakaTime(notif.createdAt), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Office · Gulshan") }
        var line by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+880 1711-000000") }
        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("Add Delivery Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
                    OutlinedTextField(value = line, onValueChange = { line = it }, label = { Text("House, Road, Area, Dhaka") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddAddress(label, line, "Dhaka Metro Express", phone)
                    showAddAddressDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") }
            }
        )
    }
}
