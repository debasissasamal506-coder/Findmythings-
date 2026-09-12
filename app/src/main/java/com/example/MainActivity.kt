package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.navigation.AppNavigation
import com.example.ui.theme.FindMyThingsTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.BoxViewModel
import com.example.viewmodel.FavoritesViewModel
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.ItemViewModel
import com.example.viewmodel.SearchViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.TodoViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission granted or denied handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FindMyThingsApp

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val openItemId = intent?.getLongExtra("OPEN_ITEM_ID", -1L)?.takeIf { it > 0 }

        setContent {
            val themeMode by app.preferencesRepository.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // Sync user data to cloud in background on app start if logged in
            LaunchedEffect(Unit) {
                app.cloudSyncRepository.autoSyncIfLoggedIn()
            }

            FindMyThingsTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                val homeViewModel: HomeViewModel by viewModels {
                    HomeViewModel.Factory(app.itemRepository, app.boxRepository, app.taskRepository)
                }
                val todoViewModel: TodoViewModel by viewModels {
                    TodoViewModel.Factory(app.taskRepository, app.itemRepository)
                }
                val itemViewModel: ItemViewModel by viewModels {
                    ItemViewModel.Factory(app.itemRepository, app.boxRepository, app.cloudSyncRepository)
                }
                val boxViewModel: BoxViewModel by viewModels {
                    BoxViewModel.Factory(app.boxRepository, app.itemRepository, app.cloudSyncRepository)
                }
                val searchViewModel: SearchViewModel by viewModels {
                    SearchViewModel.Factory(app.itemRepository, app.boxRepository)
                }
                val favoritesViewModel: FavoritesViewModel by viewModels {
                    FavoritesViewModel.Factory(app.itemRepository)
                }
                val settingsViewModel: SettingsViewModel by viewModels {
                    SettingsViewModel.Factory(app.preferencesRepository, app.backupRepository)
                }
                val authViewModel: AuthViewModel by viewModels {
                    AuthViewModel.Factory(app.authRepository, app.cloudSyncRepository, app.preferencesRepository)
                }

                AppNavigation(
                    navController = navController,
                    app = app,
                    homeViewModel = homeViewModel,
                    todoViewModel = todoViewModel,
                    itemViewModel = itemViewModel,
                    boxViewModel = boxViewModel,
                    searchViewModel = searchViewModel,
                    favoritesViewModel = favoritesViewModel,
                    settingsViewModel = settingsViewModel,
                    authViewModel = authViewModel,
                    openItemIdFromIntent = openItemId
                )
            }
        }
    }
}
