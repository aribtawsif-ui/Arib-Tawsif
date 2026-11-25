package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.model.CatalogProductView
import com.example.domain.model.UserRole
import com.example.domain.model.formatBdt
import com.example.domain.model.formatDhakaTime
import com.example.domain.model.productDrawableRes
import com.example.ui.components.MerchantBadgeStrip
import com.example.ui.components.StockAvailabilityPill
import com.example.ui.components.StockConfidenceBadge
import com.example.ui.state.PoreyUiState
import com.example.ui.theme.PoreyAmber
import com.example.ui.theme.PoreyCoral
import com.example.ui.theme.PoreyEmerald
import com.example.ui.theme.PoreyEmeraldSoft

@Composable
fun ProductDetailScreen(
    productView: CatalogProductView,
    uiState: PoreyUiState,
    onBack: () -> Unit,
    onAddToCart: (listingId: String, qty: Int) -> Unit,
    onToggleWishlist: (String) -> Unit,
    onSubmitReview: (productId: String, merchantId: String, rating: Int, comment: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val offers = productView.offers
    var selectedListingId by remember(productView.product.id, offers.size) {
        mutableStateOf(productView.bestOffer?.listing?.id ?: offers.firstOrNull()?.listing?.id.orEmpty())
    }
    val selectedOffer = offers.firstOrNull { it.listing.id == selectedListingId } ?: offers.firstOrNull()
    val productReviews = uiState.reviews.filter { it.productId == productView.product.id }
    val isWishlisted = productView.product.id in uiState.wishlistProductIds
    var showReviewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Image + Back
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
            ) {
                Image(
                    painter = painterResource(id = productDrawableRes(productView.product.imageKey)),
                    contentDescription = productView.product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .testTag("product_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                    IconButton(
                        onClick = { onToggleWishlist(productView.product.id) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle wishlist",
                            tint = if (isWishlisted) PoreyCoral else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Core Title & Price
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${productView.product.brand.uppercase()} · ${productView.product.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = PoreyAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${productView.product.rating} (${productReviews.size} reviews)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Text(
                    text = productView.product.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedOffer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatBdt(selectedOffer.effectivePriceBdt),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (selectedOffer.listing.discountPriceBdt != null) {
                            Text(
                                text = formatBdt(selectedOffer.listing.priceBdt),
                                style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StockAvailabilityPill(availableQty = selectedOffer.availableQty)
                    }
                }

                Text(
                    text = productView.product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Merchant & Variant Comparison Section
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Compare Participating Shops (${offers.size})",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Select exact size, color & store. Recommended option balances POS live confidence, reliability & distance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(offers, key = { it.listing.id }) { offer ->
            val isSelected = offer.listing.id == selectedOffer?.listing?.id
            Card(
                onClick = { selectedListingId = offer.listing.id },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("merchant_offer_${offer.listing.id}"),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = offer.merchant.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (offer.isRecommended) {
                            Surface(color = PoreyEmeraldSoft, shape = RoundedCornerShape(50)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PoreyEmerald, modifier = Modifier.size(12.dp))
                                    Text("Best Match", style = MaterialTheme.typography.labelSmall, color = PoreyEmerald)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(offer.listing.colorHex))
                            )
                            Text(
                                text = "Size ${offer.listing.size} · ${offer.listing.color}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = formatBdt(offer.effectivePriceBdt),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StockConfidenceBadge(offer.listing.stockConfidence)
                        StockAvailabilityPill(offer.availableQty)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MerchantBadgeStrip(
                            isVerified = offer.merchant.isVerified,
                            posStatus = offer.merchant.posStatus,
                            distanceKm = offer.merchant.distanceKm
                        )
                        Text(
                            text = "Delivery ${formatBdt(offer.merchant.deliveryChargeBdt)} · ★${offer.merchant.rating}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "SKU: ${offer.listing.sku} · Stock checked: ${formatDhakaTime(offer.listing.lastUpdatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Add to Cart CTA
        if (selectedOffer != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAddToCart(selectedOffer.listing.id, 1) },
                        enabled = selectedOffer.availableQty > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_to_cart_button")
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                selectedOffer.availableQty <= 0 -> "Currently Out of Stock at This Store"
                                uiState.activeRole == UserRole.GUEST -> "Login as Customer to Buy · ${formatBdt(selectedOffer.effectivePriceBdt)}"
                                else -> "Add to Cart · ${formatBdt(selectedOffer.effectivePriceBdt)} (${selectedOffer.merchant.area})"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Specifications & Policies
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Fabric & Care Specifications", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Material: ${productView.product.fabric}", style = MaterialTheme.typography.bodyMedium)
                    Text("Care: ${productView.product.careInstructions}", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Return Policy: ${productView.product.returnPolicy}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Reviews Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customer Reviews (${productReviews.size})", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { showReviewDialog = true }) {
                        Text("Write Review")
                    }
                }

                productReviews.forEach { rev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rev.customerName, style = MaterialTheme.typography.labelLarge)
                                Text("★ ${rev.rating}/5", style = MaterialTheme.typography.labelMedium, color = PoreyAmber)
                            }
                            Text(rev.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog && selectedOffer != null) {
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Review ${productView.product.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            Surface(
                                onClick = { rating = star },
                                shape = CircleShape,
                                color = if (star <= rating) PoreyAmber else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$star★",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = if (star <= rating) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Share fabric quality, fit, or delivery speed") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubmitReview(productView.product.id, selectedOffer.merchant.id, rating, comment)
                    showReviewDialog = false
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
            }
        )
    }
}
