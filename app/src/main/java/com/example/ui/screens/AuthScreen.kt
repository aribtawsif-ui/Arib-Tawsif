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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.UserAccountEntity
import com.example.domain.model.StoreType
import com.example.domain.model.UserRole
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyAmber
import com.example.ui.theme.PoreyAmberSoft
import com.example.ui.theme.PoreyCoral

@Composable
fun AuthScreen(
    uiState: PoreyUiState,
    onToggleDarkMode: () -> Unit,
    onLoginWithCredentials: (identifier: String, password: String) -> Unit,
    onLoginDemoUser: (UserAccountEntity) -> Unit,
    onContinueAsGuest: () -> Unit,
    onOpenWebsite: () -> Unit,
    onRegisterCustomer: (name: String, phone: String, email: String, password: String, address: String) -> Unit,
    onRegisterShopOwner: (
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
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = Customer, 1 = Shop Owner, 2 = Guest
    var selectedRoleTab by rememberSaveable { mutableIntStateOf(0) }
    var isCustomerRegister by rememberSaveable { mutableStateOf(false) }
    var isShopRegister by rememberSaveable { mutableStateOf(false) }
    var showOpsSection by rememberSaveable { mutableStateOf(false) }

    val customerDemo = uiState.users.firstOrNull { it.role == UserRole.CUSTOMER.name }
    val posMerchantDemo = uiState.users.firstOrNull { it.role == UserRole.PHYSICAL_MERCHANT.name }
    val manualMerchantDemo = uiState.users.firstOrNull { it.role == UserRole.NO_POS_MERCHANT.name }
    val onlineMerchantDemo = uiState.users.firstOrNull { it.role == UserRole.ONLINE_MERCHANT.name }
    val adminDemo = uiState.users.firstOrNull { it.role == UserRole.ADMIN.name }
    val riderDemo = uiState.users.firstOrNull { it.role == UserRole.RIDER.name }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header + Dark/Light Mode Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_porey_mark),
                        contentDescription = "Porey logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Porey", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "পরে",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = PoreyCoral
                            )
                        }
                        Text(
                            "Bangladesh Fashion Commerce Ecosystem",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onToggleDarkMode,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("auth_theme_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme mode",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (uiState.isDarkMode) "Light" else "Dark",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Intro Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sign In or Browse Porey",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose Customer to buy & track orders, Shop Owner to register & manage inventory, or Guest to explore freely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3-Mode Tab Selector: Customer | Shop Owner | Guest
        item {
            TabRow(selectedTabIndex = selectedRoleTab) {
                Tab(
                    selected = selectedRoleTab == 0,
                    onClick = { selectedRoleTab = 0 },
                    text = { Text("Customer") },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("auth_tab_customer")
                )
                Tab(
                    selected = selectedRoleTab == 1,
                    onClick = { selectedRoleTab = 1 },
                    text = { Text("Shop Owner") },
                    icon = { Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("auth_tab_shop_owner")
                )
                Tab(
                    selected = selectedRoleTab == 2,
                    onClick = { selectedRoleTab = 2 },
                    text = { Text("Guest") },
                    icon = { Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("auth_tab_guest")
                )
            }
        }

        item {
            when (selectedRoleTab) {
                0 -> CustomerAuthCard(
                    isRegister = isCustomerRegister,
                    onToggleRegister = { isCustomerRegister = !isCustomerRegister },
                    demoCustomer = customerDemo,
                    onLoginWithCredentials = onLoginWithCredentials,
                    onLoginDemo = onLoginDemoUser,
                    onRegisterCustomer = onRegisterCustomer
                )

                1 -> ShopOwnerAuthCard(
                    isRegister = isShopRegister,
                    onToggleRegister = { isShopRegister = !isShopRegister },
                    demoOwners = listOfNotNull(posMerchantDemo, manualMerchantDemo, onlineMerchantDemo),
                    onLoginWithCredentials = onLoginWithCredentials,
                    onLoginDemo = onLoginDemoUser,
                    onRegisterShopOwner = onRegisterShopOwner
                )

                else -> GuestEntryCard(onContinueAsGuest = onContinueAsGuest)
            }
        }

        // Quick Continue as Guest & Open Porey Website shortcuts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenWebsite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_website_button")
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Porey Website (Web Storefront)")
                }

                if (selectedRoleTab != 2) {
                    OutlinedButton(
                        onClick = onContinueAsGuest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("continue_guest_button")
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue as Guest (Browse App Without Login)")
                    }
                }
            }
        }

        // Operations / Admin & Rider Portal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Platform Admin & Rider Login", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(
                            onClick = { showOpsSection = !showOpsSection },
                            modifier = Modifier.testTag("toggle_ops_login")
                        ) {
                            Text(if (showOpsSection) "Hide" else "Open")
                        }
                    }

                    if (showOpsSection) {
                        Text(
                            text = "Platform Admins verify newly registered shops so they receive their Verified badge.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (adminDemo != null) {
                                Button(
                                    onClick = { onLoginDemoUser(adminDemo) },
                                    modifier = Modifier.weight(1f).testTag("demo_admin_login_btn")
                                ) {
                                    Icon(Icons.Outlined.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Admin Login")
                                }
                            }
                            if (riderDemo != null) {
                                OutlinedButton(
                                    onClick = { onLoginDemoUser(riderDemo) },
                                    modifier = Modifier.weight(1f).testTag("demo_rider_login_btn")
                                ) {
                                    Icon(Icons.Outlined.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rider Login")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerAuthCard(
    isRegister: Boolean,
    onToggleRegister: () -> Unit,
    demoCustomer: UserAccountEntity?,
    onLoginWithCredentials: (String, String) -> Unit,
    onLoginDemo: (UserAccountEntity) -> Unit,
    onRegisterCustomer: (String, String, String, String, String) -> Unit
) {
    var identifier by remember { mutableStateOf("nafisa.dhaka@example.bd") }
    var password by remember { mutableStateOf("porey123") }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegister) "Create Customer Account" else "Customer Sign In",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onToggleRegister, modifier = Modifier.testTag("toggle_customer_register")) {
                    Text(if (isRegister) "Have an account?" else "Register")
                }
            }

            if (!isRegister) {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Phone or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_login_identifier")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_login_password")
                )
                Button(
                    onClick = { onLoginWithCredentials(identifier, password) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("customer_login_submit")
                ) {
                    Text("Login as Customer")
                }
                if (demoCustomer != null) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = { onLoginDemo(demoCustomer) },
                        modifier = Modifier.fillMaxWidth().testTag("demo_customer_login_btn")
                    ) {
                        Text("Quick Demo Customer · ${demoCustomer.name}")
                    }
                }
            } else {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_reg_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("BD Phone (+880...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_reg_phone")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_reg_email")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dhaka Delivery Address") },
                    modifier = Modifier.fillMaxWidth().testTag("customer_reg_address")
                )
                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { regPassword = it },
                    label = { Text("Create Password (min 4 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_reg_password")
                )
                Button(
                    onClick = { onRegisterCustomer(fullName, phone, email, regPassword, address) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("customer_reg_submit")
                ) {
                    Text("Register & Start Shopping")
                }
            }
        }
    }
}

@Composable
private fun ShopOwnerAuthCard(
    isRegister: Boolean,
    onToggleRegister: () -> Unit,
    demoOwners: List<UserAccountEntity>,
    onLoginWithCredentials: (String, String) -> Unit,
    onLoginDemo: (UserAccountEntity) -> Unit,
    onRegisterShopOwner: (String, String, String, String, String, String, String, StoreType, String?, String) -> Unit
) {
    var identifier by remember { mutableStateOf("banani.pos@example.bd") }
    var password by remember { mutableStateOf("porey123") }

    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var shopPass by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Banani, Dhaka") }
    var address by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StoreType.PHYSICAL_POS) }
    var posProvider by remember { mutableStateOf("RetailBD Cloud POS") }
    var tradeLicense by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegister) "Register Your Shop" else "Shop Owner Sign In",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onToggleRegister, modifier = Modifier.testTag("toggle_shop_register")) {
                    Text(if (isRegister) "Sign In Instead" else "Register Shop")
                }
            }

            if (!isRegister) {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Shop Owner Email or Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_login_identifier")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_login_password")
                )
                Button(
                    onClick = { onLoginWithCredentials(identifier, password) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("shop_login_submit")
                ) {
                    Text("Login as Shop Owner")
                }
                if (demoOwners.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Quick Demo Shop Accounts:", style = MaterialTheme.typography.labelMedium)
                    demoOwners.forEach { owner ->
                        OutlinedButton(
                            onClick = { onLoginDemo(owner) },
                            modifier = Modifier.fillMaxWidth().testTag("demo_shop_${owner.id}")
                        ) {
                            Text(owner.name)
                        }
                    }
                }
            } else {
                Surface(color = PoreyAmberSoft, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        text = "Verification Policy: Newly registered shops stay Pending until a Platform Admin verifies your Trade License/KYC and grants the Verified sign.",
                        style = MaterialTheme.typography.labelMedium,
                        color = PoreyAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_reg_owner_name")
                )
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop / Brand Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_reg_shop_name")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("shop_reg_phone")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("shop_reg_email")
                    )
                }
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area (e.g., Gulshan 1, Dhaka)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_reg_area")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Storefront / Hub Address") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_reg_address")
                )
                Text("Store Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StoreType.entries.forEach { st ->
                        FilterChip(
                            selected = selectedType == st,
                            onClick = { selectedType = st },
                            label = {
                                Text(
                                    when (st) {
                                        StoreType.PHYSICAL_POS -> "POS Shop"
                                        StoreType.PHYSICAL_MANUAL -> "No-POS"
                                        StoreType.ONLINE_ONLY -> "Online"
                                    }
                                )
                            }
                        )
                    }
                }
                if (selectedType == StoreType.PHYSICAL_POS) {
                    OutlinedTextField(
                        value = posProvider,
                        onValueChange = { posProvider = it },
                        label = { Text("POS Software (e.g., RetailBD / ShopUp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = tradeLicense,
                    onValueChange = { tradeLicense = it },
                    label = { Text("Trade License / DBID / NID Ref (Required for Verification)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_reg_license")
                )
                OutlinedTextField(
                    value = shopPass,
                    onValueChange = { shopPass = it },
                    label = { Text("Create Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_reg_password")
                )
                Button(
                    onClick = {
                        onRegisterShopOwner(
                            ownerName,
                            phone,
                            email,
                            shopPass,
                            shopName,
                            area,
                            address,
                            selectedType,
                            if (selectedType == StoreType.PHYSICAL_POS) posProvider else null,
                            tradeLicense
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("shop_reg_submit")
                ) {
                    Text("Submit Shop for Admin Verification")
                }
            }
        }
    }
}

@Composable
private fun GuestEntryCard(onContinueAsGuest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Guest Preview Mode", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                text = "Explore curated Bangladeshi fashion, compare verified physical & online shops, and browse the responsive Web View without creating an account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(color = PoreyAmberSoft, shape = RoundedCornerShape(10.dp)) {
                Text(
                    text = "Note: To add items to your bag or purchase a product, you will be prompted to log in as a Customer.",
                    style = MaterialTheme.typography.labelMedium,
                    color = PoreyAmber,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Button(
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("guest_tab_continue_btn")
            ) {
                Text("Enter Store as Guest")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
