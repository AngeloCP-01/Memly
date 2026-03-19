package com.example.memly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.memly.ui.capture.CaptureScreen
import com.example.memly.ui.collection.CollectionDetailScreen
import com.example.memly.ui.collection.CollectionListScreen
import com.example.memly.ui.detail.MemoryDetailScreen
import com.example.memly.ui.map.MapScreen
import com.example.memly.ui.onboarding.OnboardingScreen
import com.example.memly.ui.settings.SettingsScreen
import com.example.memly.ui.timeline.TimelineScreen

@Composable
fun MemlyNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Timeline.route,
    onOnboardingComplete: () -> Unit = {},
    createCollectionTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Timeline.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onCaptureFirst = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Capture.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Timeline.route) {
            TimelineScreen(
                onMemoryClick = { memoryId ->
                    navController.navigate(Screen.MemoryDetail.createRoute(memoryId))
                },
                onCaptureClick = {
                    navController.navigate(Screen.Capture.route)
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onMemoryClick = { memoryId ->
                    navController.navigate(Screen.MemoryDetail.createRoute(memoryId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.Capture.route) {
            CaptureScreen(
                onMemorySaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.MemoryDetail.route,
            arguments = listOf(
                navArgument("memoryId") { type = NavType.LongType }
            )
        ) {
            MemoryDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CollectionList.route) {
            CollectionListScreen(
                onCollectionClick = { collectionId ->
                    navController.navigate(Screen.CollectionDetail.createRoute(collectionId))
                },
                createTrigger = createCollectionTrigger
            )
        }

        composable(
            route = Screen.CollectionDetail.route,
            arguments = listOf(
                navArgument("collectionId") { type = NavType.LongType }
            )
        ) {
            CollectionDetailScreen(
                onMemoryClick = { memoryId ->
                    navController.navigate(Screen.MemoryDetail.createRoute(memoryId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
