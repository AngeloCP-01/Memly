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
import com.example.memly.ui.search.SearchScreen
import com.example.memly.ui.timeline.TimelineScreen

@Composable
fun MemlyNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timeline.route,
        modifier = modifier
    ) {
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

        composable(Screen.Search.route) {
            SearchScreen(
                onMemoryClick = { memoryId ->
                    navController.navigate(Screen.MemoryDetail.createRoute(memoryId))
                },
                onCollectionsClick = {
                    navController.navigate(Screen.CollectionList.route)
                }
            )
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
                onNavigateBack = { navController.popBackStack() }
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
