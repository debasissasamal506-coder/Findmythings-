package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CategoryClothes
import com.example.ui.theme.CategoryDoc
import com.example.ui.theme.CategoryElectronics
import com.example.ui.theme.CategoryImportant
import com.example.ui.theme.CategoryKeys
import com.example.ui.theme.CategoryKitchen
import com.example.ui.theme.CategoryMedicine
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryTools

enum class ItemCategory(val displayName: String, val color: Color, val icon: ImageVector) {
    DOCUMENTS("Documents", CategoryDoc, Icons.Default.Description),
    ELECTRONICS("Electronics", CategoryElectronics, Icons.Default.Devices),
    KEYS("Keys", CategoryKeys, Icons.Default.Key),
    MEDICINE("Medicine", CategoryMedicine, Icons.Default.LocalPharmacy),
    CLOTHES("Clothes", CategoryClothes, Icons.Default.CheckCircle),
    TOOLS("Tools", CategoryTools, Icons.Default.Build),
    KITCHEN("Kitchen", CategoryKitchen, Icons.Default.Kitchen),
    IMPORTANT("Important", CategoryImportant, Icons.Default.Star),
    OTHER("Other", CategoryOther, Icons.Default.Folder);

    companion object {
        val ALL_NAMES = entries.map { it.displayName }

        fun fromName(name: String): ItemCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}

data class CategoryWithCount(
    val category: ItemCategory,
    val count: Int
)
