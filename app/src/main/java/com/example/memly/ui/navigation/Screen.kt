package com.example.memly.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Timeline : Screen("timeline")
    data object Map : Screen("map")
    data object Capture : Screen("capture")
    data object MemoryDetail : Screen("memory/{memoryId}") {
        fun createRoute(memoryId: Long) = "memory/$memoryId"
    }
    data object Settings : Screen("settings")
    data object CollectionList : Screen("collections")
    data object CollectionDetail : Screen("collection/{collectionId}") {
        fun createRoute(collectionId: Long) = "collection/$collectionId"
    }
}
