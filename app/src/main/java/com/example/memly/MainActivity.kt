package com.example.memly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.memly.data.local.OnboardingPreferences
import com.example.memly.ui.components.BottomNavItem
import com.example.memly.ui.components.MemlyBottomNavBar
import com.example.memly.ui.navigation.MemlyNavGraph
import com.example.memly.ui.navigation.Screen
import com.example.memly.ui.theme.MemlyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemlyTheme {
                MemlyApp(onboardingPreferences = onboardingPreferences)
            }
        }
    }
}

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Timeline.route, "Home", Icons.Outlined.Home),
    BottomNavItem(Screen.CollectionList.route, "Collections", Icons.Outlined.StickyNote2),
    BottomNavItem(Screen.Map.route, "Map", Icons.Outlined.Map),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings)
)

private val bottomNavRoutes = bottomNavItems.map { it.route }

@Composable
private fun MemlyApp(onboardingPreferences: OnboardingPreferences) {
    val isOnboardingCompleted by onboardingPreferences.isOnboardingCompleted
        .collectAsState(initial = null)

    // Wait for DataStore to emit before creating NavHost
    if (isOnboardingCompleted == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val startDestination = if (isOnboardingCompleted == true) {
        Screen.Timeline.route
    } else {
        Screen.Onboarding.route
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    val showBottomBar = currentRoute in bottomNavRoutes
    val showFab = currentRoute == Screen.Timeline.route ||
            currentRoute == Screen.CollectionList.route

    // Trigger counter to signal CollectionListScreen to open create dialog
    var createCollectionTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            MemlyNavGraph(
                navController = navController,
                startDestination = startDestination,
                onOnboardingComplete = {
                    scope.launch {
                        onboardingPreferences.setOnboardingCompleted()
                    }
                },
                createCollectionTrigger = createCollectionTrigger,
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
                        when (currentRoute) {
                            Screen.CollectionList.route -> createCollectionTrigger++
                            else -> navController.navigate(Screen.Capture.route)
                        }
                    },
                    showFab = showFab,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
