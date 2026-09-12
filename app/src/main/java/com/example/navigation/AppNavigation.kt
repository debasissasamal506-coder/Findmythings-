package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.FindMyThingsApp
import com.example.ui.components.LiquidGlassBottomBar
import com.example.ui.screens.AddItemScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BoxDetailScreen
import com.example.ui.screens.BoxesScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodoScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.BoxViewModel
import com.example.viewmodel.FavoritesViewModel
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.ItemViewModel
import com.example.viewmodel.SearchViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.TodoViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: FindMyThingsApp,
    homeViewModel: HomeViewModel,
    todoViewModel: TodoViewModel,
    itemViewModel: ItemViewModel,
    boxViewModel: BoxViewModel,
    searchViewModel: SearchViewModel,
    favoritesViewModel: FavoritesViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    openItemIdFromIntent: Long? = null
) {
    val onboardingCompleted by app.preferencesRepository.onboardingCompleted.collectAsStateWithLifecycle()
    val appLockEnabled by app.preferencesRepository.appLockEnabled.collectAsStateWithLifecycle()
    val isSessionUnlocked by app.preferencesRepository.isSessionUnlocked.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle initial intent if opened from reminder notification
    LaunchedEffect(openItemIdFromIntent) {
        if (openItemIdFromIntent != null && openItemIdFromIntent > 0L) {
            navController.navigate(Screen.ItemDetail.createRoute(openItemIdFromIntent))
        }
    }

    val topLevelRoutes = remember {
        setOf(
            Screen.Home.route,
            Screen.Search.route,
            Screen.Todo.route,
            Screen.Boxes.route,
            Screen.Favorites.route
        )
    }

    val showBottomBar = currentRoute in topLevelRoutes

    // Offline-first by default: start straight on Home (or Onboarding once, or App Lock if enabled)
    val startDestination = when {
        !onboardingCompleted -> Screen.Onboarding.route
        appLockEnabled && !isSessionUnlocked -> Screen.AppLock.route
        else -> Screen.Home.route
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LiquidGlassBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                val isTargetTop = targetState.destination.route in topLevelRoutes
                val isInitialTop = initialState.destination.route in topLevelRoutes
                if (isTargetTop && isInitialTop) {
                    fadeIn(animationSpec = tween(220))
                } else {
                    fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(260)
                    )
                }
            },
            exitTransition = {
                val isTargetTop = targetState.destination.route in topLevelRoutes
                val isInitialTop = initialState.destination.route in topLevelRoutes
                if (isTargetTop && isInitialTop) {
                    fadeOut(animationSpec = tween(180))
                } else {
                    fadeOut(animationSpec = tween(260)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(260)
                    )
                }
            },
            popEnterTransition = {
                val isTargetTop = targetState.destination.route in topLevelRoutes
                val isInitialTop = initialState.destination.route in topLevelRoutes
                if (isTargetTop && isInitialTop) {
                    fadeIn(animationSpec = tween(220))
                } else {
                    fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(260)
                    )
                }
            },
            popExitTransition = {
                val isTargetTop = targetState.destination.route in topLevelRoutes
                val isInitialTop = initialState.destination.route in topLevelRoutes
                if (isTargetTop && isInitialTop) {
                    fadeOut(animationSpec = tween(180))
                } else {
                    fadeOut(animationSpec = tween(260)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(260)
                    )
                }
            }
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        app.preferencesRepository.setOnboardingCompleted(true)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.AppLock.route,
                exitTransition = {
                    fadeOut(animationSpec = tween(380, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 1.08f, animationSpec = tween(380, easing = FastOutSlowInEasing))
                }
            ) {
                AppLockScreen(
                    preferencesRepository = app.preferencesRepository,
                    onUnlocked = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.AppLock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Home.route,
                enterTransition = {
                    if (initialState.destination.route == Screen.AppLock.route) {
                        fadeIn(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.93f, animationSpec = tween(420, easing = FastOutSlowInEasing))
                    } else {
                        val isTargetTop = targetState.destination.route in topLevelRoutes
                        val isInitialTop = initialState.destination.route in topLevelRoutes
                        if (isTargetTop && isInitialTop) {
                            fadeIn(animationSpec = tween(220))
                        } else {
                            fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(260)
                            )
                        }
                    }
                }
            ) {
                HomeScreen(
                    viewModel = homeViewModel,
                    boxViewModel = boxViewModel,
                    onNavigateToAddItem = {
                        navController.navigate(Screen.AddItem.createRoute())
                    },
                    onNavigateToAddTask = {
                        navController.navigate(Screen.Todo.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToBoxes = {
                        navController.navigate(Screen.Boxes.route)
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Screen.Favorites.route)
                    },
                    onNavigateToTodo = {
                        navController.navigate(Screen.Todo.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToItemDetail = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    onShowAddBoxDialog = {
                        navController.navigate(Screen.Boxes.route)
                    }
                )
            }

            // Things / Search Screen
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    boxViewModel = boxViewModel,
                    onNavigateToItemDetail = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Dedicated To-Do Screen (Prompt Requirement #19-22)
            composable(Screen.Todo.route) {
                TodoScreen(
                    viewModel = todoViewModel,
                    onNavigateToItemDetail = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    }
                )
            }

            composable(Screen.Boxes.route) {
                BoxesScreen(
                    viewModel = boxViewModel,
                    onNavigateToBoxDetail = { boxId ->
                        navController.navigate(Screen.BoxDetail.createRoute(boxId))
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    boxViewModel = boxViewModel,
                    onNavigateToItemDetail = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    authViewModel = authViewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToLock = {
                        navController.navigate(Screen.AppLock.route)
                    }
                )
            }

            // Add Item (with optional boxId argument)
            composable(
                route = Screen.AddItem.route,
                arguments = listOf(
                    navArgument("boxId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val boxIdStr = backStackEntry.arguments?.getString("boxId")
                val presetBoxId = boxIdStr?.toLongOrNull()
                AddItemScreen(
                    viewModel = itemViewModel,
                    itemId = 0L,
                    presetBoxId = presetBoxId,
                    onNavigateBack = { navController.popBackStack() },
                    onItemSaved = { savedId ->
                        navController.popBackStack()
                        navController.navigate(Screen.ItemDetail.createRoute(savedId))
                    }
                )
            }

            // Edit Item
            composable(
                route = Screen.EditItem.route,
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                AddItemScreen(
                    viewModel = itemViewModel,
                    itemId = itemId,
                    onNavigateBack = { navController.popBackStack() },
                    onItemSaved = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            // Item Detail
            composable(
                route = Screen.ItemDetail.route,
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                ItemDetailScreen(
                    viewModel = itemViewModel,
                    itemId = itemId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { editItemId ->
                        navController.navigate(Screen.EditItem.createRoute(editItemId))
                    },
                    onNavigateToBox = { boxId ->
                        navController.navigate(Screen.BoxDetail.createRoute(boxId))
                    }
                )
            }

            // Box Detail
            composable(
                route = Screen.BoxDetail.route,
                arguments = listOf(
                    navArgument("boxId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val boxId = backStackEntry.arguments?.getLong("boxId") ?: 0L
                BoxDetailScreen(
                    viewModel = boxViewModel,
                    boxId = boxId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddItemToBox = { bId ->
                        navController.navigate(Screen.AddItem.createRoute(bId))
                    },
                    onNavigateToItemDetail = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    }
                )
            }
        }
    }
}
