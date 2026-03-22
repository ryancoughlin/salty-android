package com.example.saltyoffshore.ui.waypoint.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.WaypointCategory
import com.example.saltyoffshore.data.waypoint.WaypointSymbol
import kotlinx.coroutines.launch

/**
 * Horizontal chip-based symbol picker with category tabs.
 * Ports iOS SymbolChipPicker — a horizontal scroll of symbol chips with
 * category tabs above for quick navigation.
 */
@Composable
fun SymbolChipPicker(
    selectedSymbol: WaypointSymbol,
    onSymbolSelected: (WaypointSymbol) -> Unit
) {
    val categoryOrder = remember {
        listOf(
            WaypointCategory.GARMIN,
            WaypointCategory.FISH,
            WaypointCategory.STRUCTURE,
            WaypointCategory.NAVIGATION,
            WaypointCategory.ENVIRONMENT,
            WaypointCategory.OTHER
        )
    }

    val sortedSymbols = remember {
        WaypointSymbol.sortedByCategory(
            WaypointSymbol.entries,
            categoryOrder
        )
    }

    var selectedCategory by remember { mutableStateOf(selectedSymbol.category) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Category tabs
        ScrollableTabRow(
            selectedTabIndex = categoryOrder.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = 0.dp,
            divider = {},
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            categoryOrder.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = category
                        val firstIndex = sortedSymbols.indexOfFirst { it.category == category }
                        if (firstIndex >= 0) {
                            scope.launch { listState.animateScrollToItem(firstIndex) }
                        }
                    },
                    text = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        // Horizontal symbol chips
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(sortedSymbols, key = { it.name }) { symbol ->
                SymbolChip(
                    symbol = symbol,
                    isSelected = selectedSymbol == symbol,
                    onClick = { onSymbolSelected(symbol) }
                )
            }
        }
    }
}

/**
 * Individual symbol chip: circular icon + label below.
 * Ports iOS SymbolChip.
 */
@Composable
private fun SymbolChip(
    symbol: WaypointSymbol,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val circleSize = 64.dp
    val iconSize = 50.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(width = circleSize, height = 90.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        // Circle with icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            val iconResId = context.resources.getIdentifier(
                symbol.imageName.lowercase().replace(" ", "_").replace("-", "_"),
                "drawable",
                context.packageName
            )
            if (iconResId != 0) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = symbol.rawValue,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Label
        Text(
            text = symbol.shortName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
