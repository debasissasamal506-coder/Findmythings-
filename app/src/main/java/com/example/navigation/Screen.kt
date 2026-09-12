package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search") // Represents "Things" screen
    object Todo : Screen("todo")     // Dedicated To-Do List screen
    object Boxes : Screen("boxes")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")

    object AddItem : Screen("add_item?boxId={boxId}") {
        fun createRoute(boxId: Long? = null): String {
            return if (boxId != null) "add_item?boxId=$boxId" else "add_item"
        }
    }

    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: Long): String = "edit_item/$itemId"
    }

    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Long): String = "item_detail/$itemId"
    }

    object BoxDetail : Screen("box_detail/{boxId}") {
        fun createRoute(boxId: Long): String = "box_detail/$boxId"
    }

    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object AppLock : Screen("app_lock")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Search, "Things", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    BottomNavItem(Screen.Todo, "To-Do", Icons.Filled.Checklist, Icons.Outlined.Checklist),
    BottomNavItem(Screen.Boxes, "Boxes", Icons.Filled.Archive, Icons.Outlined.Archive),
    BottomNavItem(Screen.Favorites, "Favorites", Icons.Filled.Star, Icons.Outlined.StarBorder)
)
