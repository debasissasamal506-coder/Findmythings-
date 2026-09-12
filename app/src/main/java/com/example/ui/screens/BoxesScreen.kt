package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BoxEntity
import com.example.model.BoxWithItemCount
import com.example.ui.components.EmptyStateView
import com.example.viewmodel.BoxViewModel

@Composable
fun BoxesScreen(
    viewModel: BoxViewModel,
    onNavigateToBoxDetail: (Long) -> Unit
) {
    val boxesWithCounts by viewModel.boxesWithCounts.collectAsStateWithLifecycle()
    val boxSearchQuery by viewModel.boxSearchQuery.collectAsStateWithLifecycle()

    var showCreateBoxDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateBoxDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_box_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Box")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("boxes_screen_content")
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Storage Boxes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Group and track physical containers, kits & folders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search within boxes
            if (boxesWithCounts.isNotEmpty() || boxSearchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = boxSearchQuery,
                    onValueChange = { viewModel.onBoxSearchChange(it) },
                    placeholder = { Text("Search boxes or location...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("box_search_field")
                )
            }

            if (boxesWithCounts.isEmpty() && boxSearchQuery.isBlank()) {
                EmptyStateView(
                    icon = Icons.Default.Archive,
                    title = "No storage boxes yet",
                    subtitle = "Create a physical storage box to group items together (e.g. \"Medicine Kit\" or \"Box A01\").",
                    actionButtonText = "+ Create Box",
                    onActionClick = { showCreateBoxDialog = true },
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(boxesWithCounts, key = { "box_${it.box.id}" }) { boxWithCount ->
                        BoxCard(
                            boxWithCount = boxWithCount,
                            onClick = { onNavigateToBoxDetail(boxWithCount.box.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateBoxDialog) {
        CreateOrEditBoxDialog(
            box = null,
            onDismiss = { showCreateBoxDialog = false },
            onConfirm = { name, location, notes ->
                viewModel.saveBox(0L, name, location, notes) {
                    showCreateBoxDialog = false
                }
            }
        )
    }
}

@Composable
fun BoxCard(
    boxWithCount: BoxWithItemCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val box = boxWithCount.box
    val count = boxWithCount.itemCount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("box_card_${box.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = box.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (box.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = box.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!box.notes.isNullOrBlank()) {
                    Text(
                        text = box.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$count item${if (count != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CreateOrEditBoxDialog(
    box: BoxEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(box?.name ?: "") }
    var location by remember { mutableStateOf(box?.location ?: "") }
    var notes by remember { mutableStateOf(box?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (box == null) "Create Storage Box" else "Edit Box",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Box Name *") },
                    placeholder = { Text("e.g. Box A01, Medicine Kit, Winter Clothes") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Box name is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_box_name_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("e.g. Store Room Shelf 2, Bedroom Top Shelf") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_box_location_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Red plastic container with black lid") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_box_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, location, notes)
                    }
                },
                modifier = Modifier.testTag("dialog_box_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_box_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
