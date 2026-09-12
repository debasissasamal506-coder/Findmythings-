package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.VoiceRecorderSection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.ItemCategory
import com.example.utils.ImageStorageHelper
import com.example.viewmodel.ItemViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(
    viewModel: ItemViewModel,
    itemId: Long = 0L,
    presetBoxId: Long? = null,
    onNavigateBack: () -> Unit,
    onItemSaved: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val availableBoxes by viewModel.availableBoxes.collectAsStateWithLifecycle()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val savedUri = ImageStorageHelper.persistImageToInternalStorage(context, uri)
                viewModel.onPhotoSelected(savedUri ?: uri.toString())
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            coroutineScope.launch {
                val savedUri = ImageStorageHelper.persistImageToInternalStorage(context, tempCameraUri!!)
                viewModel.onPhotoSelected(savedUri ?: tempCameraUri.toString())
            }
        }
    }

    LaunchedEffect(itemId) {
        if (itemId > 0L) {
            viewModel.loadItem(itemId, recordView = false)
        } else {
            viewModel.initForNewItem(presetBoxId = presetBoxId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collect { savedId ->
            onItemSaved(savedId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (itemId > 0L) "Edit Item" else "Add Item",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_item_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("add_item_screen_content"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section: Item Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ITEM NAME *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    placeholder = { Text("e.g. Passport, Car Spare Key, Warranty Card") },
                    singleLine = true,
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Section: Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemCategory.entries.forEach { category ->
                        val isSelected = formState.category.equals(category.displayName, ignoreCase = true)
                        Surface(
                            onClick = { viewModel.onCategoryChange(category.displayName) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) category.color else category.color.copy(alpha = 0.12f),
                            modifier = Modifier.testTag("category_chip_${category.displayName}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.surface else category.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else category.color
                                )
                            }
                        }
                    }
                }
            }

            // Section: Photo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PHOTO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!formState.photoUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = formState.photoUri,
                            contentDescription = "Item Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.onPhotoSelected(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_photo_button")
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Photo")
                        }

                        OutlinedButton(
                            onClick = {
                                val uri = ImageStorageHelper.createCameraTempUri(context)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("take_photo_button")
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo")
                        }
                    }
                }
            }

            // Section: Structured Location
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LOCATION *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formState.liveLocationPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (formState.locationError != null) {
                    Text(
                        text = formState.locationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = formState.mainLocation,
                    onValueChange = { viewModel.onMainLocationChange(it) },
                    label = { Text("Main Location") },
                    placeholder = { Text("e.g. Home, Office, Garage") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_location_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.room,
                    onValueChange = { viewModel.onRoomChange(it) },
                    label = { Text("Room / Area") },
                    placeholder = { Text("e.g. Bedroom, Store Room, Study") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("room_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.storage,
                    onValueChange = { viewModel.onStorageChange(it) },
                    label = { Text("Storage Unit / Furniture") },
                    placeholder = { Text("e.g. Almirah, Wardrobe, Desk, Cabinet") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.specificLocation,
                    onValueChange = { viewModel.onSpecificLocationChange(it) },
                    label = { Text("Specific Place / Container") },
                    placeholder = { Text("e.g. 2nd Drawer, Blue File, Top Shelf") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("specific_location_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Section: Storage Box Assignment (Optional)
            if (availableBoxes.isNotEmpty()) {
                var boxExpanded by remember { mutableStateOf(false) }
                val selectedBox = availableBoxes.firstOrNull { it.id == formState.boxId }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ASSIGN TO PHYSICAL BOX (OPTIONAL)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ExposedDropdownMenuBox(
                        expanded = boxExpanded,
                        onExpandedChange = { boxExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${it.location})" } ?: "None (Unassigned)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boxExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("box_assignment_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = boxExpanded,
                            onDismissRequest = { boxExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Unassigned)") },
                                onClick = {
                                    viewModel.onBoxSelected(null)
                                    boxExpanded = false
                                }
                            )
                            availableBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} - ${box.location}") },
                                    onClick = {
                                        viewModel.onBoxSelected(box.id)
                                        boxExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Voice Recorder (Audio Instructions)
            VoiceRecorderSection(
                audioUri = formState.audioUri,
                durationSec = formState.audioDurationSec,
                onAudioRecorded = { uri, dur ->
                    viewModel.onAudioRecorded(uri, dur)
                },
                onRemoveAudio = {
                    viewModel.onRemoveAudio()
                }
            )

            // Section: Notes (Optional typed text)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TEXT NOTES (OPTIONAL)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = formState.notes,
                    onValueChange = { viewModel.onNotesChange(it) },
                    placeholder = { Text("e.g. Original passport. Keep safely away from moisture.") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Section: Favorite Toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (formState.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (formState.favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mark as Favorite",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pin to your quick access favorites list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = formState.favorite,
                        onCheckedChange = { viewModel.onFavoriteToggle(it) },
                        modifier = Modifier.testTag("item_favorite_switch")
                    )
                }
            }

            // Section: Optional Local Reminder
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Reminder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Set a local alarm notification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = formState.reminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.onReminderToggle(enabled)
                                if (enabled && formState.reminderDate == null) {
                                    // Default to tomorrow 9 AM
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                        set(Calendar.HOUR_OF_DAY, 9)
                                        set(Calendar.MINUTE, 0)
                                    }
                                    viewModel.onReminderDateSelected(cal.timeInMillis)
                                }
                            },
                            modifier = Modifier.testTag("item_reminder_switch")
                        )
                    }

                    if (formState.reminderEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = formState.reminderDate ?: System.currentTimeMillis()
                        }
                        val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                            .format(Date(calendar.timeInMillis))

                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                calendar.set(Calendar.MINUTE, minute)
                                                viewModel.onReminderDateSelected(calendar.timeInMillis)
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("pick_reminder_datetime_button")
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = formattedDate, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Item Button
            Button(
                onClick = { viewModel.saveItem(context) },
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_item_button")
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (itemId > 0L) "Update Item" else "Save Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
