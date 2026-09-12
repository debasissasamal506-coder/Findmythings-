package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ItemCard
import com.example.viewmodel.BoxViewModel
import com.example.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    boxViewModel: BoxViewModel,
    onNavigateToItemDetail: (Long) -> Unit
) {
    val favoriteItems by viewModel.favoriteItems.collectAsStateWithLifecycle()
    val boxesWithCounts by boxViewModel.boxesWithCounts.collectAsStateWithLifecycle()

    val boxMap = boxesWithCounts.associate { it.box.id to it.box.name }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("favorites_screen_content")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Quick access to your most important belongings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (favoriteItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.StarBorder,
                    title = "No favorites yet",
                    subtitle = "Star your passport, emergency keys, or warranty cards to find them instantly here.",
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favoriteItems, key = { "fav_${it.id}" }) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onNavigateToItemDetail(item.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(item) },
                            boxName = item.boxId?.let { boxMap[it] }
                        )
                    }
                }
            }
        }
    }
}
