package com.example.memly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.memly.ui.components.BottomNavItem
import com.example.memly.ui.components.MemlyBottomNavBar
import com.example.memly.ui.navigation.MemlyNavGraph
import com.example.memly.ui.navigation.Screen
import com.example.memly.ui.theme.MemlyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemlyTheme {
                MemlyApp()
            }
        }
    }
}

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Timeline.route, "Home", Icons.Outlined.Home),
    BottomNavItem(Screen.CollectionList.route, "Collections", Icons.Outlined.StickyNote2),
    BottomNavItem(Screen.Search.route, "Search", Icons.Outlined.FavoriteBorder),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Outlined.GridView)
)

private val bottomNavRoutes = bottomNavItems.map { it.route }

@Composable
private fun MemlyApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            MemlyNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )

            if (showBottomBar) {
                MemlyBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = {
                        navController.navigate(Screen.Capture.route)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
