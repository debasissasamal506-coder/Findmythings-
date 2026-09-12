package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassQuickActionButton
import com.example.ui.components.GlassSearchBar
import com.example.ui.components.GlassStatCard
import com.example.ui.components.ItemCard
import com.example.ui.screens.GlassTaskCard
import com.example.viewmodel.BoxViewModel
import com.example.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    boxViewModel: BoxViewModel,
    onNavigateToAddItem: () -> Unit,
    onNavigateToAddTask: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBoxes: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToItemDetail: (Long) -> Unit,
    onShowAddBoxDialog: () -> Unit
) {
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val favoriteItems by viewModel.favoriteItems.collectAsStateWithLifecycle()
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val totalItems by viewModel.totalItems.collectAsStateWithLifecycle()
    val totalBoxes by viewModel.totalBoxes.collectAsStateWithLifecycle()
    val totalTasks by viewModel.totalTasks.collectAsStateWithLifecycle()
    val boxesWithCounts by boxViewModel.boxesWithCounts.collectAsStateWithLifecycle()

    val boxMap = boxesWithCounts.associate { it.box.id to it.box.name }

    // Dynamic greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 5..11 -> "Good morning"
            hour in 12..16 -> "Good afternoon"
            hour in 17..21 -> "Good evening"
            else -> "Welcome back"
        }
    }
    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddItem,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(bottom = 76.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary)
                        .testTag("home_add_item_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Thing")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("home_screen_content"),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Top Greeting & Settings bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = todayDateFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .shadow(2.dp, CircleShape)
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Large Glass Search Bar
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                        GlassSearchBar(
                            query = "",
                            onQueryChange = { onNavigateToSearch() },
                            onVoiceClick = onNavigateToSearch,
                            placeholderText = "Search your things...",
                            modifier = Modifier.clickable { onNavigateToSearch() },
                            testTag = "home_glass_search_bar"
                        )
                    }
                }

                // Quick Actions: + Add Thing, + Add Task, + Add Box
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassQuickActionButton(
                            title = "Add Thing",
                            icon = Icons.Default.Add,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToAddItem,
                            modifier = Modifier.weight(1f)
                        )
                        GlassQuickActionButton(
                            title = "Add Task",
                            icon = Icons.Default.Checklist,
                            accentColor = Color(0xFF10B981),
                            onClick = onNavigateToAddTask,
                            modifier = Modifier.weight(1f)
                        )
                        GlassQuickActionButton(
                            title = "Add Box",
                            icon = Icons.Default.Archive,
                            accentColor = Color(0xFFF59E0B),
                            onClick = onShowAddBoxDialog,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Statistics Cards Grid (Things, Boxes, Tasks, Favorites)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassStatCard(
                                title = "Things",
                                count = totalItems,
                                icon = Icons.Default.Inventory2,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToSearch,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                title = "Boxes",
                                count = totalBoxes,
                                icon = Icons.Default.Archive,
                                accentColor = Color(0xFFF59E0B),
                                onClick = onNavigateToBoxes,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassStatCard(
                                title = "Tasks",
                                count = totalTasks,
                                icon = Icons.Default.Checklist,
                                accentColor = Color(0xFF10B981),
                                onClick = onNavigateToTodo,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                title = "Favorites",
                                count = favoriteItems.size,
                                icon = Icons.Default.Star,
                                accentColor = Color(0xFFEC4899),
                                onClick = onNavigateToFavorites,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // "Today's Tasks" Section (prompt section 8: "Then: Today's Tasks")
                if (todayTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View all ›",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onNavigateToTodo)
                            )
                        }
                    }

                    items(todayTasks, key = { "home_task_${it.id}" }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                            GlassTaskCard(
                                task = task,
                                onToggle = { viewModel.toggleTask(task.id, task.isCompleted) },
                                onDelete = {},
                                onThingClick = {
                                    if (task.relatedThingId != null) {
                                        onNavigateToItemDetail(task.relatedThingId)
                                    }
                                }
                            )
                        }
                    }
                }

                // If no items at all: show designated empty state
                if (totalItems == 0 && todayTasks.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Inventory2,
                            title = "No things saved yet",
                            subtitle = "Add your first thing to remember where you kept it.",
                            actionButtonText = "+ Add Thing",
                            onActionClick = onNavigateToAddItem,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                } else {
                    // "Recently Added" Section
                    if (recentlyAdded.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently Added",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "See all ›",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onNavigateToSearch)
                                )
                            }
                        }

                        items(recentlyAdded.take(5), key = { "home_item_${it.id}" }) { item ->
                            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
                                ItemCard(
                                    item = item,
                                    onClick = { onNavigateToItemDetail(item.id) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(item) },
                                    boxName = item.boxId?.let { boxMap[it] }
                                )
                            }
                        }
                    }

                    // "Favorites" Section
                    if (favoriteItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "See all ›",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onNavigateToFavorites)
                                )
                            }
                        }

                        items(favoriteItems.take(4), key = { "home_fav_${it.id}" }) { item ->
                            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
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
    }
}
