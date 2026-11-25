package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.SortOption
import com.example.domain.model.formatBdt
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ProductEditorialCard
import com.example.ui.state.CatalogFilterState
import com.example.ui.state.PoreyUiState

@Composable
fun DiscoverScreen(
    uiState: PoreyUiState,
    onUpdateFilter: ((CatalogFilterState) -> CatalogFilterState) -> Unit,
    onResetFilters: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onToggleWishlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }
    val filter = uiState.filterState
    val categories = listOf("All", "Men", "Women", "Kids", "Sportswear", "New Arrivals", "Offers")
    val sizes = listOf("All", "M", "L", "XL", "Free Size", "6-8Y")

    Column(modifier = modifier.fillMaxSize()) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
                placeholder = { Text("Search product, brand, shop, fabric...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { onUpdateFilter { it.copy(query = "") } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("discover_search_input")
            )
            Surface(
                onClick = { showAdvancedFilters = !showAdvancedFilters },
                shape = RoundedCornerShape(14.dp),
                color = if (showAdvancedFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .height(54.dp)
                    .testTag("discover_filter_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Toggle filters",
                        tint = if (showAdvancedFilters) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = filter.selectedCategory == cat,
                    onClick = { onUpdateFilter { it.copy(selectedCategory = cat) } },
                    label = { Text(cat) }
                )
            }
        }

        // Advanced Filter Panel
        if (showAdvancedFilters) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
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
                        Text("Filter & Sort", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onResetFilters) {
                            Text("Reset All")
                        }
                    }

                    Text("Size", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sizes) { sz ->
                            FilterChip(
                                selected = filter.selectedSize == sz,
                                onClick = { onUpdateFilter { it.copy(selectedSize = sz) } },
                                label = { Text(sz) }
                            )
                        }
                    }

                    Text(
                        text = "Max Price: ${formatBdt(filter.maxPriceBdt)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = filter.maxPriceBdt.toFloat(),
                        onValueChange = { v -> onUpdateFilter { it.copy(maxPriceBdt = v.toInt()) } },
                        valueRange = 1000f..10000f,
                        steps = 8
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filter.inStockOnly,
                            onClick = { onUpdateFilter { it.copy(inStockOnly = !it.inStockOnly) } },
                            label = { Text("In Stock Only") }
                        )
                        FilterChip(
                            selected = filter.expressOnly,
                            onClick = { onUpdateFilter { it.copy(expressOnly = !it.expressOnly) } },
                            label = { Text("Same-Day Dhaka") }
                        )
                    }

                    Text("Sort By", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SortOption.entries) { sort ->
                            FilterChip(
                                selected = filter.sortOption == sort,
                                onClick = { onUpdateFilter { it.copy(sortOption = sort) } },
                                label = { Text(sort.label) }
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "${uiState.filteredProducts.size} items across verified Dhaka & BD shops",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (uiState.filteredProducts.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Filled.Search,
                title = "No matching garments",
                message = "Try adjusting your price ceiling, size, or search keywords.",
                actionLabel = "Clear Filters",
                onAction = onResetFilters
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 164.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredProducts, key = { it.product.id }) { item ->
                    ProductEditorialCard(
                        item = item,
                        isWishlisted = item.product.id in uiState.wishlistProductIds,
                        onClick = { onOpenProduct(item.product.id) },
                        onToggleWishlist = { onToggleWishlist(item.product.id) }
                    )
                }
            }
        }
    }
}
