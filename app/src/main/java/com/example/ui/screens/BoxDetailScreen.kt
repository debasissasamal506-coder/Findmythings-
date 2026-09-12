package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ItemEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ItemCard
import com.example.viewmodel.BoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    viewModel: BoxViewModel,
    boxId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAddItemToBox: (Long) -> Unit,
    onNavigateToItemDetail: (Long) -> Unit
) {
    val currentBox by viewModel.currentBox.collectAsStateWithLifecycle()
    val itemsInBox by viewModel.itemsInCurrentBox.collectAsStateWithLifecycle()
    val unassignedItems by viewModel.unassignedItems.collectAsStateWithLifecycle()

    var showEditBoxDialog by remember { mutableStateOf(false) }
    var showDeleteBoxDialog by remember { mutableStateOf(false) }
    var showAssignExistingItemDialog by remember { mutableStateOf(false) }

    LaunchedEffect(boxId) {
        viewModel.selectBox(boxId)
    }

    val box = currentBox

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = box?.name ?: "Box Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("box_detail_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (box != null) {
                        IconButton(
                            onClick = { showEditBoxDialog = true },
                            modifier = Modifier.testTag("box_detail_edit_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Box")
                        }
                        IconButton(
                            onClick = { showDeleteBoxDialog = true },
                            modifier = Modifier.testTag("box_detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Box",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddItemToBox(boxId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_item_to_box_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item to this Box")
            }
        }
    ) { innerPadding ->
        if (box == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading box...", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("box_detail_content"),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Box Info Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = box.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "${itemsInBox.size} item${if (itemsInBox.size != 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (box.location.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = box.location,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            if (!box.notes.isNullOrBlank()) {
                                Text(
                                    text = box.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Quick Assign Existing Item button
                            OutlinedButton(
                                onClick = { showAssignExistingItemDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("assign_existing_item_button")
                            ) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assign Existing Item to Box")
                            }
                        }
                    }
                }

                // Items list header
                item {
                    Text(
                        text = "ITEMS IN THIS BOX",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (itemsInBox.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Archive,
                            title = "This box is empty",
                            subtitle = "Add items or assign existing belongings into this storage box.",
                            actionButtonText = "+ Add Item",
                            onActionClick = { onNavigateToAddItemToBox(boxId) },
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                } else {
                    items(itemsInBox, key = { "box_item_${it.id}" }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ItemCard(
                                        item = item,
                                        onClick = { onNavigateToItemDetail(item.id) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(item) }
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeItemFromBox(item.id) },
                                    modifier = Modifier.testTag("remove_from_box_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Remove from box",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditBoxDialog && box != null) {
        CreateOrEditBoxDialog(
            box = box,
            onDismiss = { showEditBoxDialog = false },
            onConfirm = { name, location, notes ->
                viewModel.saveBox(box.id, name, location, notes) {
                    showEditBoxDialog = false
                }
            }
        )
    }

    if (showDeleteBoxDialog && box != null) {
        ConfirmDeleteDialog(
            title = "Delete this box?",
            message = "Are you sure you want to delete '${box.name}'? Items inside will remain saved and will be unassigned from this box.",
            onConfirm = {
                showDeleteBoxDialog = false
                viewModel.deleteBox(box.id) {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteBoxDialog = false }
        )
    }

    if (showAssignExistingItemDialog) {
        AssignExistingItemDialog(
            unassignedItems = unassignedItems,
            onDismiss = { showAssignExistingItemDialog = false },
            onItemSelected = { selectedItem ->
                viewModel.addItemToBox(selectedItem.id, boxId)
                showAssignExistingItemDialog = false
            }
        )
    }
}

@Composable
private fun AssignExistingItemDialog(
    unassignedItems: List<ItemEntity>,
    onDismiss: () -> Unit,
    onItemSelected: (ItemEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Assign Item to Box", fontWeight = FontWeight.Bold) },
        text = {
            if (unassignedItems.isEmpty()) {
                Text("No available items to assign. All your items are already in this box or you haven't added any yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unassignedItems, key = { "assign_${it.id}" }) { item ->
                        Surface(
                            onClick = { onItemSelected(item) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.formatLocationHierarchy(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
